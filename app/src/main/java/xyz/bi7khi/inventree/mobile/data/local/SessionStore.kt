package xyz.bi7khi.inventree.mobile.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.flow.first
import xyz.bi7khi.inventree.mobile.data.api.Session
import xyz.bi7khi.inventree.mobile.data.api.StoredSession
import xyz.bi7khi.inventree.mobile.data.api.UserDto
import xyz.bi7khi.inventree.mobile.data.api.UserProfileDto
import xyz.bi7khi.inventree.mobile.util.UrlUtils

private val Context.sessionDataStore by preferencesDataStore(name = "inventree_session")

/**
 * Stores the server URL plus an API token encrypted with an Android Keystore
 * AES-GCM key. The token never touches disk in plaintext.
 */
class SessionStore(private val context: Context) {

    private val tokenCipher = TokenCipher()

    suspend fun read(): StoredSession {
        val prefs = context.sessionDataStore.data.first()
        val server = prefs[KEY_SERVER]?.takeIf { it.isNotBlank() } ?: UrlUtils.defaultServer()
        val encrypted = prefs[KEY_TOKEN]
        val token = encrypted?.let { tokenCipher.decrypt(it) }
        val username = prefs[KEY_USERNAME]
        val user = if (username != null) {
            UserDto(
                pk = prefs[KEY_USER_PK] ?: 0,
                username = username,
                first_name = prefs[KEY_FIRST_NAME].orEmpty(),
                last_name = prefs[KEY_LAST_NAME].orEmpty(),
                email = prefs[KEY_EMAIL].orEmpty(),
                is_staff = prefs[KEY_IS_STAFF] ?: false,
                is_superuser = prefs[KEY_IS_SUPERUSER] ?: false,
                profile = UserProfileDto(displayname = prefs[KEY_DISPLAY_NAME]),
            )
        } else {
            null
        }
        return StoredSession(
            serverUrl = server,
            token = token,
            user = user,
            serverConfigured = prefs[KEY_CONFIGURED] ?: false,
        )
    }

    suspend fun saveServer(url: String) {
        context.sessionDataStore.edit { it[KEY_SERVER] = url }
    }

    suspend fun markServerConfigured(url: String) {
        context.sessionDataStore.edit {
            it[KEY_SERVER] = url
            it[KEY_CONFIGURED] = true
        }
    }

    suspend fun resetServerConfiguration() {
        context.sessionDataStore.edit { it[KEY_CONFIGURED] = false }
    }

    suspend fun saveSession(session: Session) {
        val encrypted = tokenCipher.encrypt(session.token)
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SERVER] = session.serverUrl
            prefs[KEY_CONFIGURED] = true
            prefs[KEY_TOKEN] = encrypted
            prefs[KEY_USERNAME] = session.user.username
            prefs[KEY_USER_PK] = session.user.pk
            prefs[KEY_FIRST_NAME] = session.user.first_name
            prefs[KEY_LAST_NAME] = session.user.last_name
            prefs[KEY_EMAIL] = session.user.email
            prefs[KEY_DISPLAY_NAME] = session.user.profile?.displayname.orEmpty()
            prefs[KEY_IS_STAFF] = session.user.is_staff
            prefs[KEY_IS_SUPERUSER] = session.user.is_superuser
        }
    }

    suspend fun updateUser(user: UserDto) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_USERNAME] = user.username
            prefs[KEY_USER_PK] = user.pk
            prefs[KEY_FIRST_NAME] = user.first_name
            prefs[KEY_LAST_NAME] = user.last_name
            prefs[KEY_EMAIL] = user.email
            prefs[KEY_DISPLAY_NAME] = user.profile?.displayname.orEmpty()
            prefs[KEY_IS_STAFF] = user.is_staff
            prefs[KEY_IS_SUPERUSER] = user.is_superuser
        }
    }

    suspend fun clearToken(keepServer: String) {
        context.sessionDataStore.edit { prefs ->
            prefs[KEY_SERVER] = keepServer
            prefs.remove(KEY_TOKEN)
            prefs.remove(KEY_USERNAME)
            prefs.remove(KEY_USER_PK)
            prefs.remove(KEY_FIRST_NAME)
            prefs.remove(KEY_LAST_NAME)
            prefs.remove(KEY_EMAIL)
            prefs.remove(KEY_DISPLAY_NAME)
            prefs.remove(KEY_IS_STAFF)
            prefs.remove(KEY_IS_SUPERUSER)
        }
    }

    private companion object {
        val KEY_SERVER = stringPreferencesKey("server_url")
        val KEY_CONFIGURED = booleanPreferencesKey("server_configured")
        val KEY_TOKEN = stringPreferencesKey("token_encrypted")
        val KEY_USERNAME = stringPreferencesKey("username")
        val KEY_USER_PK = intPreferencesKey("user_pk")
        val KEY_FIRST_NAME = stringPreferencesKey("first_name")
        val KEY_LAST_NAME = stringPreferencesKey("last_name")
        val KEY_EMAIL = stringPreferencesKey("email")
        val KEY_DISPLAY_NAME = stringPreferencesKey("display_name")
        val KEY_IS_STAFF = booleanPreferencesKey("is_staff")
        val KEY_IS_SUPERUSER = booleanPreferencesKey("is_superuser")
    }
}

internal class TokenCipher {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val payload = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv, Base64.NO_WRAP) + ":" +
            Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? = runCatching {
        val (ivPart, payloadPart) = encoded.split(":", limit = 2)
        val iv = Base64.decode(ivPart, Base64.NO_WRAP)
        val payload = Base64.decode(payloadPart, Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(payload), Charsets.UTF_8)
    }.getOrNull()

    private fun getOrCreateKey(): SecretKey {
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "inventree_mobile_session"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
