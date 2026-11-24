package xyz.bi7khi.inventree.mobile.data.label

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val Context.labelDataStore by preferencesDataStore(name = "inventree_labels")

class LabelDesignStore(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    suspend fun load(type: LabelTemplateType): LabelDesign {
        val raw = context.labelDataStore.data.first()[keyFor(type)]
        if (raw.isNullOrBlank()) return DefaultLabelDesigns.forType(type)
        return runCatching { json.decodeFromString<LabelDesign>(raw) }
            .getOrElse { DefaultLabelDesigns.forType(type) }
    }

    suspend fun save(type: LabelTemplateType, design: LabelDesign) {
        context.labelDataStore.edit { it[keyFor(type)] = json.encodeToString(LabelDesign.serializer(), design) }
    }

    suspend fun reset(type: LabelTemplateType) {
        context.labelDataStore.edit { it.remove(keyFor(type)) }
    }

    suspend fun loadSettings(): LabelPrintSettings {
        val raw = context.labelDataStore.data.first()[KEY_SETTINGS]
        if (raw.isNullOrBlank()) return LabelPrintSettings()
        return runCatching { json.decodeFromString<LabelPrintSettings>(raw) }
            .getOrElse { LabelPrintSettings() }
    }

    suspend fun saveSettings(settings: LabelPrintSettings) {
        context.labelDataStore.edit { it[KEY_SETTINGS] = json.encodeToString(LabelPrintSettings.serializer(), settings) }
    }

    private fun keyFor(type: LabelTemplateType) = stringPreferencesKey("design_${type.key}")

    private companion object {
        val KEY_SETTINGS = stringPreferencesKey("print_settings")
    }
}
