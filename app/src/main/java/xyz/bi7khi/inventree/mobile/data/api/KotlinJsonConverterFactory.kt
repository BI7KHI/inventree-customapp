package xyz.bi7khi.inventree.mobile.data.api

import java.lang.reflect.Type
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import kotlinx.serialization.serializer

/**
 * Minimal kotlinx.serialization converter. Keeps the dependency graph small and
 * avoids relying on Gson's reflection behaviour for Kotlin default values.
 */
class KotlinJsonConverterFactory(private val json: Json) : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<ResponseBody, *>? {
        if (type == Unit::class.java || type == Void::class.java) return null
        val serializer = serializer(type)
        return Converter<ResponseBody, Any> { body ->
            body.use { json.decodeFromString(serializer, it.string()) }
        }
    }

    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): Converter<*, RequestBody>? {
        if (type == Unit::class.java) return null
        val serializer = serializer(type)
        val mediaType = "application/json; charset=utf-8".toMediaType()
        return Converter<Any, RequestBody> { value ->
            json.encodeToString(serializer, value).toRequestBody(mediaType)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun serializer(type: Type): KSerializer<Any> =
        json.serializersModule.serializer(type) as KSerializer<Any>
}
