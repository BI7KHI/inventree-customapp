package xyz.bi7khi.inventree.mobile.data.api

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.JsonObject
import retrofit2.HttpException

sealed interface BarcodeOutcome {
    data class StockItem(val id: Int, val label: String?) : BarcodeOutcome
    data class Part(val id: Int, val label: String?) : BarcodeOutcome
    data class Location(val id: Int, val label: String?) : BarcodeOutcome
    data class NotFound(val barcode: String, val message: String) : BarcodeOutcome
}

class InvenTreeRepository(
    val api: InvenTreeApi,
    val serverUrl: String,
    val token: String,
) {

    val json get() = ApiClient.json

    suspend fun me(): UserDto = api.me()

    suspend fun serverInfo(): ServerInfo = api.serverInfo()

    suspend fun stockItems(
        query: String = "",
        locationId: Int? = null,
        partId: Int? = null,
        offset: Int = 0,
        limit: Int = 25,
        inStock: Boolean? = true,
        ordering: String? = "-updated",
    ): Paged<StockItemDto> = api.stockItems(
        limit = limit,
        offset = offset,
        search = query.takeIf { it.isNotBlank() },
        location = locationId,
        part = partId,
        inStock = inStock,
        ordering = ordering,
    )

    suspend fun stockItem(id: Int): StockItemDto = api.stockItem(id)

    suspend fun updateStock(id: Int, body: StockPatchRequest): StockItemDto =
        api.patchStockItem(id, body)

    suspend fun deleteStock(id: Int) {
        val response = api.deleteStockItem(id)
        if (!response.isSuccessful) throw ApiException(response.code(), "删除库存项失败")
    }

    suspend fun createStock(body: StockCreateRequest): StockItemDto {
        val items = api.createStockItem(body)
        return items.firstOrNull() ?: throw ApiException(201, "库存项已创建，但服务器未返回数据")
    }

    suspend fun adjustStock(
        type: StockActionType,
        item: StockItemDto,
        quantity: Double,
        notes: String = "",
        destination: LocationDto? = null,
    ) {
        require(quantity > 0) { "数量必须大于 0" }
        val request = StockAdjustmentRequest(
            items = listOf(
                StockAdjustmentItem(pk = item.pk, quantity = formatQuantity(quantity)),
            ),
            notes = notes.trim(),
            location = destination?.pk,
        )
        when (type) {
            StockActionType.ADD -> api.addStock(request)
            StockActionType.REMOVE -> api.removeStock(request)
            StockActionType.TRANSFER -> {
                requireNotNull(destination) { "请选择目标库位" }
                api.transferStock(request)
            }
            StockActionType.COUNT -> api.countStock(request)
        }
    }

    suspend fun dashboardCounts(): DashboardCounts = coroutineScope {
        val parts = async { api.parts(limit = 1).count }
        val stock = async { api.stockItems(limit = 1, inStock = true).count }
        val locations = async { api.locations(limit = 1).count }
        val low = async { api.parts(limit = 1, lowStock = true).count }
        DashboardCounts(
            parts = parts.await(),
            stockItems = stock.await(),
            locations = locations.await(),
            lowStock = low.await(),
        )
    }

    suspend fun recentStock(limit: Int = 5): List<StockItemDto> =
        api.stockItems(limit = limit, ordering = "-updated").results

    suspend fun lowStockParts(limit: Int = 5): List<PartDto> =
        api.parts(limit = limit, lowStock = true, ordering = "name").results

    suspend fun locations(query: String = "", offset: Int = 0, limit: Int = 100): Paged<LocationDto> =
        api.locations(limit = limit, offset = offset, search = query.takeIf { it.isNotBlank() })

    suspend fun allLocations(): List<LocationDto> = coroutineScope {
        val first = api.locations(limit = 250, offset = 0)
        val items = first.results.toMutableList()
        var offset = items.size
        while (offset < first.count && offset < 2000) {
            val page = api.locations(limit = 250, offset = offset)
            if (page.results.isEmpty()) break
            items += page.results
            offset += page.results.size
        }
        items
    }

    suspend fun location(id: Int): LocationDto = api.location(id)

    suspend fun createLocation(body: LocationCreateRequest): LocationDto = api.createLocation(body)

    suspend fun updateLocation(id: Int, body: LocationPatchRequest): LocationDto =
        api.patchLocation(id, body)

    suspend fun deleteLocation(
        id: Int,
        deleteStockItems: Boolean = false,
        deleteSubLocations: Boolean = false,
    ) {
        val response = api.deleteLocation(
            id,
            LocationDeleteRequest(
                delete_stock_items = deleteStockItems,
                delete_sub_locations = deleteSubLocations,
            ),
        )
        if (!response.isSuccessful) {
            throw ApiException(response.code(), ApiErrorMessage.from(response.errorBody()?.string()))
        }
    }

    suspend fun parts(
        query: String = "",
        category: Int? = null,
        lowStock: Boolean? = null,
        offset: Int = 0,
        limit: Int = 25,
        ordering: String? = "name",
    ): Paged<PartDto> = api.parts(
        limit = limit,
        offset = offset,
        search = query.takeIf { it.isNotBlank() },
        category = category,
        lowStock = lowStock,
        ordering = ordering,
    )

    suspend fun part(id: Int): PartDto = api.part(id)

    suspend fun createPart(body: PartCreateRequest): PartDto = api.createPart(body)

    suspend fun updatePart(id: Int, body: PartPatchRequest): PartDto = api.patchPart(id, body)

    suspend fun deletePart(id: Int) {
        val response = api.deletePart(id)
        if (!response.isSuccessful) {
            throw ApiException(response.code(), ApiErrorMessage.from(response.errorBody()?.string()))
        }
    }

    suspend fun stockForPart(partId: Int): List<StockItemDto> =
        api.stockItems(limit = 100, part = partId, inStock = null, ordering = "location").results

    suspend fun allCategories(): List<PartCategoryDto> = coroutineScope {
        val first = api.categories(limit = 250, offset = 0)
        val items = first.results.toMutableList()
        var offset = items.size
        while (offset < first.count && offset < 2000) {
            val page = api.categories(limit = 250, offset = offset)
            if (page.results.isEmpty()) break
            items += page.results
            offset += page.results.size
        }
        items
    }

    suspend fun scanBarcode(raw: String): BarcodeOutcome {
        try {
            val result = api.scanBarcode(BarcodeRequest(raw.trim()))
            result.stockitem?.let { return BarcodeOutcome.StockItem(it.pk, result.success) }
            result.part?.let { return BarcodeOutcome.Part(it.pk, result.success) }
            result.stocklocation?.let { return BarcodeOutcome.Location(it.pk, result.success) }
            return BarcodeOutcome.NotFound(raw, result.error ?: "未找到匹配的条码")
        } catch (e: HttpException) {
            val body = e.response()?.errorBody()?.string()
            val message = ApiClient.extractError(body) ?: "条码无法识别"
            if (e.code() == 400 || e.code() == 404) {
                return BarcodeOutcome.NotFound(raw, message)
            }
            throw ApiException(e.code(), message, body)
        }
    }

    suspend fun generateBarcode(model: String, pk: Int): String =
        api.generateBarcode(BarcodeGenerateRequest(model, pk)).barcode

    fun imageUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        return mediaUrl(serverUrl, path)
    }

    /**
     * Return the best available image URLs for a part.
     *
     * InvenTree protects /media/ behind an authenticated nginx auth_request, so
     * the client must send its API token. The thumbnail is preferred, with the
     * original image as a fallback. InvenTree's generated "blank image" is not
     * a real part image and is deliberately ignored.
     */
    fun partImageUrls(thumbnail: String?, image: String?): List<String> =
        listOf(thumbnail, image)
            .mapNotNull(::imageUrl)
            .filterNot { it.contains("blank_image", ignoreCase = true) }
            .distinct()

}

enum class StockActionType(val title: String) {
    ADD("入库"),
    REMOVE("出库"),
    TRANSFER("转移库位"),
    COUNT("盘点"),
}

fun formatQuantity(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toBigDecimal().stripTrailingZeros().toPlainString()
}

object ApiErrorMessage {
    fun from(body: String?): String = ApiClient.extractError(body) ?: "服务器拒绝了该操作"
}

fun JsonObject.stringField(key: String): String? =
    runCatching { this[key]?.toString()?.trim('"') }.getOrNull()
