package xyz.bi7khi.inventree.mobile.util

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object UrlUtils {

    /** Normalise user input into the root URL used by Retrofit (always ends in '/'). */
    fun normalizeServerUrl(input: String): String {
        var value = input.trim()
        require(value.isNotEmpty()) { "请输入服务器地址" }
        if (!value.startsWith("http://", ignoreCase = true) &&
            !value.startsWith("https://", ignoreCase = true)
        ) {
            value = "https://$value"
        }
        val parsed = value.toHttpUrlOrNull() ?: error("服务器地址格式不正确")
        require(parsed.scheme == "http" || parsed.scheme == "https") { "仅支持 HTTP 或 HTTPS" }

        var path = parsed.encodedPath.trimEnd('/')
        if (path.endsWith("/api", ignoreCase = true)) {
            path = path.dropLast(4)
        }
        val base = parsed.newBuilder()
            .encodedPath(if (path.isBlank()) "/" else "$path/")
            .query(null)
            .fragment(null)
            .build()
        return base.toString()
    }

    fun displayServer(url: String): String = url
        .removePrefix("https://")
        .removePrefix("http://")
        .trimEnd('/')

    fun canUseCleartext(url: String): Boolean = url.startsWith("http://", ignoreCase = true)

    fun resolve(rootUrl: String, path: String?): String? {
        if (path.isNullOrBlank()) return null
        if (path.startsWith("http://") || path.startsWith("https://")) return path
        return rootUrl.toHttpUrlOrNull()?.resolve(path.removePrefix("/"))?.toString()
    }

    fun defaultServer(): String = xyz.bi7khi.inventree.mobile.BuildConfig.DEFAULT_SERVER_URL
}
