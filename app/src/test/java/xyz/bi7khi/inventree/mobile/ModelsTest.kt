package xyz.bi7khi.inventree.mobile

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.bi7khi.inventree.mobile.data.api.ApiClient
import xyz.bi7khi.inventree.mobile.data.api.ApiException
import xyz.bi7khi.inventree.mobile.data.api.BarcodeScanResponse
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.Paged
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto

class ModelsTest {

    private val json = ApiClient.json

    @Test
    fun `parses a paginated stock response from InvenTree 1_5_2`() {
        val body = """
            {"count":173,"next":"http://127.0.0.1:1337/api/stock/?limit=2&offset=2",
             "previous":null,
             "results":[{"pk":34,"part":31,"quantity":1.0,"serial":null,"batch":"",
             "location":6,"status":10,"status_text":"OK","in_stock":true,
             "location_detail":{"pk":6,"name":"周转存储箱-03",
             "pathstring":"广东工业大学/周转存储箱-03"},
             "part_detail":{"pk":31,"IPN":"INV-0017","name":"1/4W金属膜电阻套件散件",
             "description":"1/4W 金属膜电阻套件散件","thumbnail":"/static/img/blank_image.thumbnail.png",
             "minimum_stock":"0.000000","trackable":false}}]}
        """.trimIndent()
        val page = json.decodeFromString<Paged<StockItemDto>>(body)
        assertEquals(173, page.count)
        assertEquals(1, page.results.size)
        val item = page.results.single()
        assertEquals(34, item.pk)
        assertEquals(1.0, item.quantity, 0.0)
        assertEquals("1/4W金属膜电阻套件散件", item.partName)
        assertEquals("广东工业大学/周转存储箱-03", item.locationName)
        assertEquals("INV-0017", item.part_detail?.IPN)
    }

    @Test
    fun `parses part detail with nullable numeric fields`() {
        val body = """
            {"pk":173,"name":"__APP_TEST_PART__","description":"temporary",
             "IPN":"","category":null,"category_name":null,"active":true,
             "component":true,"purchaseable":true,"trackable":false,
             "minimum_stock":0.0,"maximum_stock":10.0,"total_in_stock":null,
             "stock_item_count":null,"units":"","full_name":"__APP_TEST_PART__"}
        """.trimIndent()
        val part = json.decodeFromString<PartDto>(body)
        assertEquals(173, part.pk)
        assertEquals(10.0, part.maximum_stock!!, 0.0)
        assertNull(part.total_in_stock)
        assertEquals("__APP_TEST_PART__", part.displayName)
    }

    @Test
    fun `parses barcode match response`() {
        val body = """
            {"stockitem":{"pk":178,"api_url":"/api/stock/178/",
            "instance":{"pk":178,"quantity":11.0},
            "web_url":"/web/stock/item/178"},
            "plugin":"InvenTreeBarcode","barcode_data":"INV-SI178",
            "barcode_hash":"45bf","success":"Match found for barcode data"}
        """.trimIndent()
        val response = json.decodeFromString<BarcodeScanResponse>(body)
        assertEquals(178, response.stockitem?.pk)
        assertEquals("INV-SI178", response.barcodeData)
        assertNull(response.part)
        assertEquals("/web/stock/item/178", response.stockitem?.webUrl)
    }

    @Test
    fun `error extraction handles DRF field errors`() {
        assertEquals(
            "名称不能为空",
            ApiClient.extractError("""{"name":["名称不能为空"]}"""),
        )
        assertEquals(
            "未找到匹配的条码",
            ApiClient.extractError("""{"error":"未找到匹配的条码","plugin":null}"""),
        )
        assertNull(ApiClient.extractError("<html>not json</html>"))
    }

    @Test
    fun `part image candidates use authenticated server URLs and skip blank placeholders`() {
        val repository = InvenTreeRepository(
            xyz.bi7khi.inventree.mobile.data.api.ApiClient.create("https://erp.example.com/", "secret"),
            "https://erp.example.com/",
            "secret",
        )
        val urls = repository.partImageUrls(
            "/media/part_images/part.thumbnail.png",
            "/media/part_images/part.png",
        )
        assertEquals(
            listOf(
                "https://erp.example.com/media/part_images/part.thumbnail.png",
                "https://erp.example.com/media/part_images/part.png",
            ),
            urls,
        )
        assertTrue(
            repository.partImageUrls(
                "/static/img/blank_image.thumbnail.png",
                null,
            ).isEmpty(),
        )
    }

    @Test
    fun `api exception carries status`() {
        val error = ApiException(401, "未授权")
        assertEquals(401, error.statusCode)
        assertEquals("未授权", error.message)
    }

    @Test
    fun `json ignores unknown server fields`() {
        val data = Json { ignoreUnknownKeys = true }
        val server = data.decodeFromString<xyz.bi7khi.inventree.mobile.data.api.ServerInfo>(
            """{"server":"InvenTree","version":"1.5.2","apiVersion":530,"future_field":true}""",
        )
        assertEquals(530, server.apiVersion)
        assertTrue(server.version.startsWith("1.5"))
    }
}
