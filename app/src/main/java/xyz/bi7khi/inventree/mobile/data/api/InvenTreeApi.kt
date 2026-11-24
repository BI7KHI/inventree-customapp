package xyz.bi7khi.inventree.mobile.data.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface InvenTreeApi {

    @GET("api/")
    suspend fun serverInfo(): ServerInfo

    @GET("api/user/me/")
    suspend fun me(): UserDto

    @GET("api/user/me/token/")
    suspend fun authToken(@Query("name") name: String = "InvenTree-Mobile"): TokenResponse

    @GET("api/stock/")
    suspend fun stockItems(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("location") location: Int? = null,
        @Query("part") part: Int? = null,
        @Query("in_stock") inStock: Boolean? = null,
        @Query("ordering") ordering: String? = null,
        @Query("part_detail") partDetail: Boolean = true,
        @Query("location_detail") locationDetail: Boolean = true,
    ): Paged<StockItemDto>

    @GET("api/stock/{id}/")
    suspend fun stockItem(
        @Path("id") id: Int,
        @Query("part_detail") partDetail: Boolean = true,
        @Query("location_detail") locationDetail: Boolean = true,
    ): StockItemDto

    @PATCH("api/stock/{id}/")
    suspend fun patchStockItem(
        @Path("id") id: Int,
        @Body body: StockPatchRequest,
    ): StockItemDto

    @DELETE("api/stock/{id}/")
    suspend fun deleteStockItem(@Path("id") id: Int): Response<Unit>

    @POST("api/stock/")
    suspend fun createStockItem(@Body body: StockCreateRequest): List<StockItemDto>

    @POST("api/stock/add/")
    suspend fun addStock(@Body body: StockAdjustmentRequest)

    @POST("api/stock/remove/")
    suspend fun removeStock(@Body body: StockAdjustmentRequest)

    @POST("api/stock/transfer/")
    suspend fun transferStock(@Body body: StockAdjustmentRequest)

    @POST("api/stock/count/")
    suspend fun countStock(@Body body: StockAdjustmentRequest)

    @GET("api/stock/location/")
    suspend fun locations(
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("parent") parent: Int? = null,
        @Query("ordering") ordering: String = "pathstring",
    ): Paged<LocationDto>

    @GET("api/stock/location/{id}/")
    suspend fun location(@Path("id") id: Int): LocationDto

    @POST("api/stock/location/")
    suspend fun createLocation(@Body body: LocationCreateRequest): LocationDto

    @PATCH("api/stock/location/{id}/")
    suspend fun patchLocation(
        @Path("id") id: Int,
        @Body body: LocationPatchRequest,
    ): LocationDto

    @DELETE("api/stock/location/{id}/")
    suspend fun deleteLocation(
        @Path("id") id: Int,
        @Body body: LocationDeleteRequest,
    ): Response<Unit>

    @GET("api/part/")
    suspend fun parts(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0,
        @Query("search") search: String? = null,
        @Query("category") category: Int? = null,
        @Query("low_stock") lowStock: Boolean? = null,
        @Query("has_stock") hasStock: Boolean? = null,
        @Query("active") active: Boolean? = null,
        @Query("ordering") ordering: String? = null,
    ): Paged<PartDto>

    @GET("api/part/{id}/")
    suspend fun part(@Path("id") id: Int): PartDto

    @POST("api/part/")
    suspend fun createPart(@Body body: PartCreateRequest): PartDto

    @PATCH("api/part/{id}/")
    suspend fun patchPart(
        @Path("id") id: Int,
        @Body body: PartPatchRequest,
    ): PartDto

    @DELETE("api/part/{id}/")
    suspend fun deletePart(@Path("id") id: Int): Response<Unit>

    @GET("api/part/category/")
    suspend fun categories(
        @Query("limit") limit: Int = 200,
        @Query("offset") offset: Int = 0,
        @Query("ordering") ordering: String = "pathstring",
    ): Paged<PartCategoryDto>

    @POST("api/barcode/")
    suspend fun scanBarcode(@Body body: BarcodeRequest): BarcodeScanResponse

    @POST("api/barcode/generate/")
    suspend fun generateBarcode(@Body body: BarcodeGenerateRequest): BarcodeGenerateResponse

    @GET("api/user/me/roles/")
    suspend fun roles(): RolesResponse
}

@kotlinx.serialization.Serializable
data class BarcodeRequest(val barcode: String)

@kotlinx.serialization.Serializable
data class BarcodeGenerateRequest(val model: String, val pk: Int)

@kotlinx.serialization.Serializable
data class BarcodeGenerateResponse(val barcode: String = "")

@kotlinx.serialization.Serializable
data class RolesResponse(
    val roles: Map<String, List<String>> = emptyMap(),
)
