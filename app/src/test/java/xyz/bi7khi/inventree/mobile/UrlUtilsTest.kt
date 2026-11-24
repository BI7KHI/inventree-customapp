package xyz.bi7khi.inventree.mobile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.bi7khi.inventree.mobile.util.UrlUtils

class UrlUtilsTest {

    @Test
    fun `adds https scheme and trailing slash`() {
        assertEquals("https://erp.example.com/", UrlUtils.normalizeServerUrl("erp.example.com"))
    }

    @Test
    fun `keeps explicit http scheme for local servers`() {
        assertEquals("http://10.0.2.2:1337/", UrlUtils.normalizeServerUrl("http://10.0.2.2:1337"))
    }

    @Test
    fun `strips api suffix`() {
        assertEquals("https://erp.example.com/", UrlUtils.normalizeServerUrl("https://erp.example.com/api/"))
        assertEquals("https://erp.example.com/", UrlUtils.normalizeServerUrl("https://erp.example.com/api"))
    }

    @Test
    fun `preserves a deployment subpath`() {
        assertEquals("https://example.com/inventree/", UrlUtils.normalizeServerUrl("https://example.com/inventree"))
    }

    @Test
    fun `removes query and fragment`() {
        assertEquals(
            "https://erp.example.com/",
            UrlUtils.normalizeServerUrl("https://erp.example.com/?x=1#top"),
        )
    }

    @Test
    fun `rejects invalid input`() {
        assertTrue(runCatching { UrlUtils.normalizeServerUrl("   ") }.isFailure)
    }

    @Test
    fun `display server hides scheme`() {
        assertEquals("erp.example.com", UrlUtils.displayServer("https://erp.example.com/"))
    }

    @Test
    fun `resolves media paths`() {
        assertEquals(
            "https://erp.example.com/media/part_images/a.png",
            UrlUtils.resolve("https://erp.example.com/", "/media/part_images/a.png"),
        )
    }
}
