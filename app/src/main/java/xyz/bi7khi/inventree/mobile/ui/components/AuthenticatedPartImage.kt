package xyz.bi7khi.inventree.mobile.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import okhttp3.Headers

/**
 * Loads an authenticated InvenTree part image.
 *
 * InvenTree's nginx configuration protects /media/ with an auth_request.
 * <img> tags in the web UI work because the browser sends the Django session
 * cookie. The native app authenticates with an API token, so the token must be
 * added explicitly to every Coil request.
 *
 * If the preferred thumbnail fails (404, missing derivative, etc.) the next
 * candidate URL is tried. When all candidates fail the supplied placeholder is
 * rendered instead of leaving an empty box.
 */
@Composable
fun AuthenticatedPartImage(
    urls: List<String>,
    token: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    fallback: @Composable () -> Unit = {
        Icon(
            imageVector = Icons.Outlined.Inventory2,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    },
) {
    if (urls.isEmpty()) {
        Box(modifier, contentAlignment = Alignment.Center) { fallback() }
        return
    }

    val context = LocalContext.current
    var attempt by remember(urls) { mutableIntStateOf(0) }
    var exhausted by remember(urls) { mutableStateOf(false) }
    val url = urls.getOrNull(attempt)

    Box(modifier, contentAlignment = Alignment.Center) {
        if (!exhausted && url != null) {
            val request = remember(url, token) {
                ImageRequest.Builder(context)
                    .data(url)
                    .headers(
                        Headers.Builder()
                            .add("Authorization", "Token $token")
                            .add("Accept", "image/*")
                            .build(),
                    )
                    // Keep cached media scoped to the authenticated account.
                    .memoryCacheKey("$url#${token.hashCode()}")
                    .diskCacheKey("$url#${token.hashCode()}")
                    .crossfade(180)
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = "零件图片",
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onError = {
                    if (attempt < urls.lastIndex) {
                        attempt += 1
                    } else {
                        exhausted = true
                    }
                },
            )
        }
        if (exhausted) {
            fallback()
        }
    }
}
