package xyz.bi7khi.inventree.mobile.data.api

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Credentials
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import xyz.bi7khi.inventree.mobile.BuildConfig
import xyz.bi7khi.inventree.mobile.util.UrlUtils

object ApiClient {

    const val USER_AGENT = "InvenTreeMobile/1.2.1 (Android)"

    /** Emits when an authenticated request is rejected with HTTP 401. */
    val unauthorizedEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
        explicitNulls = false
        coerceInputValues = true
        isLenient = true
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
        redactHeader("Authorization")
        redactHeader("Cookie")
    }

    val baseHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(false)
            .addInterceptor(logging)
            .build()
    }

    fun create(serverUrl: String, token: String?): InvenTreeApi {
        val client = baseHttpClient.newBuilder().apply {
            if (!token.isNullOrBlank()) {
                addInterceptor { chain ->
                    val response = chain.proceed(
                        chain.request().newBuilder()
                            .header("Authorization", "Token $token")
                            .header("Accept", "application/json")
                            .header("User-Agent", USER_AGENT)
                            .build(),
                    )
                    if (response.code == 401) {
                        unauthorizedEvents.tryEmit(Unit)
                    }
                    response
                }
            }
        }.build()

        return Retrofit.Builder()
            .baseUrl(serverUrl)
            .client(client)
            .addConverterFactory(KotlinJsonConverterFactory(json))
            .build()
            .create(InvenTreeApi::class.java)
    }

    suspend fun probeServer(serverUrl: String): ServerInfo = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(serverUrl.toHttpUrl().newBuilder().addPathSegments("api/").build())
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        baseHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ApiException(
                    response.code,
                    "服务器返回 HTTP ${response.code}${errorSuffix(body)}",
                    body,
                )
            }
            runCatching { json.decodeFromString<ServerInfo>(body) }
                .getOrElse { throw ApiException(response.code, "该地址不是可识别的 InvenTree API", body) }
        }
    }

    /** Authenticate with HTTP Basic and exchange it for a persistent API token. */
    suspend fun requestToken(
        serverUrl: String,
        username: String,
        password: String,
    ): TokenResponse = withContext(Dispatchers.IO) {
        val url = serverUrl.toHttpUrl().newBuilder()
            .addPathSegments("api/user/me/token/")
            .addQueryParameter("name", "InvenTree-Mobile")
            .build()
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(username, password, Charsets.UTF_8))
            .header("Accept", "application/json")
            .header("User-Agent", USER_AGENT)
            .build()
        baseHttpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val message = when (response.code) {
                    401, 403 -> "用户名或密码不正确，或账户已被禁用"
                    else -> "登录失败：${extractError(body) ?: "HTTP ${response.code}"}"
                }
                throw ApiException(response.code, message, body)
            }
            runCatching { json.decodeFromString<TokenResponse>(body) }
                .getOrElse { throw ApiException(response.code, "服务器返回了无法解析的登录响应", body) }
                .also {
                    if (it.token.isBlank()) throw ApiException(response.code, "服务器未返回 API Token", body)
                }
        }
    }

    fun extractError(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            val root = json.parseToJsonElement(body) as? JsonObject ?: return@runCatching null
            val direct = listOf("detail", "error", "message", "non_field_errors")
                .firstNotNullOfOrNull { key ->
                    root[key]?.let { element ->
                        when {
                            element is JsonObject -> element["detail"]?.jsonPrimitive?.contentOrNull
                                ?: element.toString()
                            element.toString().startsWith("[") -> element.jsonArray
                                .joinToString("；") { it.jsonPrimitive.contentOrNull.orEmpty() }
                            else -> element.jsonPrimitive.contentOrNull
                        }
                    }
                }
            if (!direct.isNullOrBlank()) direct
            else if (root.size == 1) {
                val value = root.values.first()
                runCatching { value.jsonArray.joinToString("；") { it.jsonPrimitive.content } }
                    .getOrElse { value.toString().trim('"') }
            } else {
                root.entries.take(3).joinToString("；") { (key, value) ->
                    val text = runCatching { value.jsonArray.joinToString(",") { it.jsonPrimitive.content } }
                        .getOrElse { value.toString().trim('"') }
                    "$key: $text"
                }.takeIf { it.isNotBlank() }
            }
        }.getOrNull()
    }

    private fun errorSuffix(body: String): String = extractError(body)?.let { "：$it" } ?: ""
}

class ApiException(
    val statusCode: Int,
    override val message: String,
    val body: String? = null,
) : IOException(message)

fun Throwable.toUserMessage(): String = when (this) {
    is ApiException -> message
    is UnknownHostException -> "无法解析服务器地址，请检查域名或网络"
    is ConnectException -> "无法连接服务器，请确认地址、端口和服务状态"
    is SocketTimeoutException -> "连接超时，请稍后重试"
    is SSLException -> "HTTPS 证书校验失败，请确认服务器证书有效"
    is HttpException -> "服务器返回 HTTP ${code()}"
    is IOException -> "网络请求失败：${message ?: "请检查网络连接"}"
    else -> message ?: "操作失败，请稍后重试"
}

fun mediaUrl(serverUrl: String, path: String?): String? = UrlUtils.resolve(serverUrl, path)
