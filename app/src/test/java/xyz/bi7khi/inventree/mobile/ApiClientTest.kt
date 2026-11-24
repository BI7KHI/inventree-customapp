package xyz.bi7khi.inventree.mobile

import java.util.Base64
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import xyz.bi7khi.inventree.mobile.data.api.ApiClient

class ApiClientTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `probe server parses InvenTree metadata`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"server":"InvenTree","version":"1.5.2","apiVersion":530,"worker_running":true}""",
            ),
        )
        val info = ApiClient.probeServer(server.url("/").toString())
        assertEquals("1.5.2", info.version)
        assertEquals(530, info.apiVersion)
        val request = server.takeRequest()
        assertEquals("/api/", request.path)
    }

    @Test
    fun `token request uses basic auth and returns token`() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"token":"abc123","name":"InvenTree-Mobile","expiry":"2027-01-01"}""",
            ),
        )
        val token = ApiClient.requestToken(server.url("/").toString(), "alice", "s3cret")
        assertEquals("abc123", token.token)
        val request = server.takeRequest()
        assertTrue(request.path!!.startsWith("/api/user/me/token/"))
        assertTrue(request.path!!.contains("name=InvenTree-Mobile"))
        val expected = "Basic " + Base64.getEncoder()
            .encodeToString("alice:s3cret".toByteArray(Charsets.UTF_8))
        assertEquals(expected, request.getHeader("Authorization"))
    }

    @Test
    fun `login maps 401 to a friendly Chinese error`() = runBlocking {
        server.enqueue(
            MockResponse().setResponseCode(401).setBody("""{"detail":"Invalid username/password."}"""),
        )
        val error = runCatching {
            ApiClient.requestToken(server.url("/").toString(), "alice", "wrong")
        }.exceptionOrNull()
        assertTrue(error is xyz.bi7khi.inventree.mobile.data.api.ApiException)
        val apiError = error as xyz.bi7khi.inventree.mobile.data.api.ApiException
        assertEquals(401, apiError.statusCode)
        assertTrue(apiError.message.contains("用户名或密码"))
    }

    @Test
    fun `probe rejects non InvenTree html responses`() = runBlocking {
        server.enqueue(MockResponse().setBody("<html>login</html>"))
        val error = runCatching { ApiClient.probeServer(server.url("/").toString()) }.exceptionOrNull()
        assertTrue(error is xyz.bi7khi.inventree.mobile.data.api.ApiException)
    }
}
