package xyz.bi7khi.inventree.mobile.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class Paged<T>(
    val count: Int = 0,
    val next: String? = null,
    val previous: String? = null,
    val results: List<T> = emptyList(),
)

@Serializable
data class ServerInfo(
    val server: String = "InvenTree",
    val version: String = "",
    val apiVersion: Int = 0,
    val instance: String = "InvenTree",
    val worker_running: Boolean? = null,
    val plugins_enabled: Boolean? = null,
)

@Serializable
data class TokenResponse(
    val token: String,
    val name: String = "",
    val expiry: String = "",
)

@Serializable
data class UserProfileDto(
    val displayname: String? = null,
    val position: String? = null,
    val type: String? = null,
    val organisation: String? = null,
)

@Serializable
data class UserDto(
    val pk: Int = 0,
    val username: String = "",
    val first_name: String = "",
    val last_name: String = "",
    val email: String = "",
    val is_staff: Boolean = false,
    val is_superuser: Boolean = false,
    val is_active: Boolean = true,
    val profile: UserProfileDto? = null,
) {
    val displayName: String
        get() = profile?.displayname?.takeIf { it.isNotBlank() }
            ?: listOf(first_name, last_name).filter { it.isNotBlank() }.joinToString(" ")
                .ifBlank { username }

    val initials: String
        get() = displayName.trim().take(2).uppercase().ifBlank { "IT" }
}

@Serializable
data class PartBriefDto(
    val pk: Int = 0,
    val IPN: String? = null,
    val name: String = "",
    val full_name: String? = null,
    val description: String = "",
    val image: String? = null,
    val thumbnail: String? = null,
    val active: Boolean = true,
    val minimum_stock: String? = null,
    val units: String? = null,
    val trackable: Boolean = false,
    val default_location: Int? = null,
    val category_default_location: Int? = null,
)

@Serializable
data class PartDto(
    val pk: Int = 0,
    val name: String = "",
    val description: String = "",
    val IPN: String? = null,
    val full_name: String? = null,
    val category: Int? = null,
    val category_name: String? = null,
    val category_path: String? = null,
    val image: String? = null,
    val thumbnail: String? = null,
    val active: Boolean = true,
    val locked: Boolean = false,
    val assembly: Boolean = false,
    val component: Boolean = false,
    val consumable: Boolean = false,
    val purchaseable: Boolean = false,
    val salable: Boolean = false,
    val testable: Boolean = false,
    val trackable: Boolean = false,
    val virtual: Boolean = false,
    val is_template: Boolean = false,
    val units: String? = null,
    val minimum_stock: Double? = null,
    val maximum_stock: Double? = null,
    val default_location: Int? = null,
    val default_location_detail: LocationBrief? = null,
    val keywords: String? = null,
    val link: String? = null,
    val notes: String? = null,
    val revision: String? = null,
    val total_in_stock: Double? = null,
    val stock_item_count: Int? = null,
    val external_stock: Double? = null,
    val unallocated_stock: Double? = null,
    val ordering: Double? = null,
    val in_stock: Double? = null,
    val creation_date: String? = null,
    val pricing_min: Double? = null,
    val pricing_max: Double? = null,
) {
    val displayName: String
        get() = full_name?.takeIf { it.isNotBlank() } ?: name
}

@Serializable
data class LocationBrief(
    val pk: Int = 0,
    val name: String = "",
    val pathstring: String? = null,
    val icon: String? = null,
)

@Serializable
data class LocationDto(
    val pk: Int = 0,
    val name: String = "",
    val description: String = "",
    val pathstring: String? = null,
    val parent: Int? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val items: Int = 0,
    val sublocations: Int = 0,
    val level: Int = 0,
    val location_type: Int? = null,
    val icon: String? = null,
    val custom_icon: String? = null,
)

@Serializable
data class StockItemDto(
    val pk: Int = 0,
    val part: Int = 0,
    val quantity: Double = 0.0,
    val serial: String? = null,
    val batch: String? = null,
    val location: Int? = null,
    val status: Int? = null,
    val status_text: String? = null,
    val in_stock: Boolean = true,
    val notes: String? = null,
    val packaging: String? = null,
    val link: String? = null,
    val expiry_date: String? = null,
    val purchase_price: String? = null,
    val purchase_price_currency: String? = null,
    val allocated: Double? = null,
    val installed_items: Int? = null,
    val child_items: Int? = null,
    val stale: Boolean? = null,
    val updated: String? = null,
    val creation_date: String? = null,
    val barcode_hash: String? = null,
    val part_detail: PartBriefDto? = null,
    val location_detail: LocationBrief? = null,
    val location_path: List<LocationBrief> = emptyList(),
    val tracking_items: Int? = null,
    val delete_on_deplete: Boolean = true,
) {
    val partName: String
        get() = part_detail?.name ?: "零件 #$part"

    val locationName: String
        get() = location_detail?.pathstring ?: location_detail?.name ?: "未分配库位"
}

@Serializable
data class PartCategoryDto(
    val pk: Int = 0,
    val name: String = "",
    val description: String = "",
    val pathstring: String? = null,
    val parent: Int? = null,
    val level: Int = 0,
    val part_count: Int = 0,
    val subcategories: Int = 0,
    val structural: Boolean = false,
    val default_location: Int? = null,
)

@Serializable
data class StockAdjustmentItem(
    val pk: Int,
    val quantity: String,
    val batch: String? = null,
    val status: Int? = null,
    val packaging: String? = null,
    val merge: Boolean = false,
)

@Serializable
data class StockAdjustmentRequest(
    val items: List<StockAdjustmentItem>,
    val notes: String = "",
    val location: Int? = null,
)

@Serializable
data class StockCreateRequest(
    val part: Int,
    val quantity: String,
    val location: Int? = null,
    val batch: String? = null,
    val serial: String? = null,
    val notes: String? = null,
    val status: Int? = null,
    val packaging: String? = null,
)

@Serializable
data class StockPatchRequest(
    val location: Int? = null,
    val batch: String? = null,
    val serial: String? = null,
    val status: Int? = null,
    val packaging: String? = null,
    val notes: String? = null,
    val expiry_date: String? = null,
    val link: String? = null,
    val delete_on_deplete: Boolean? = null,
)

@Serializable
data class PartCreateRequest(
    val name: String,
    val description: String = "",
    val IPN: String = "",
    val category: Int? = null,
    val units: String? = null,
    val minimum_stock: Double = 0.0,
    val maximum_stock: Double = 0.0,
    val active: Boolean = true,
    val assembly: Boolean = false,
    val component: Boolean = false,
    val purchaseable: Boolean = false,
    val salable: Boolean = false,
    val trackable: Boolean = false,
    val virtual: Boolean = false,
    val consumable: Boolean = false,
    val keywords: String? = null,
    val link: String? = null,
    val notes: String? = null,
    val default_location: Int? = null,
)

@Serializable
data class PartPatchRequest(
    val name: String? = null,
    val description: String? = null,
    val IPN: String? = null,
    val category: Int? = null,
    val units: String? = null,
    val minimum_stock: Double? = null,
    val maximum_stock: Double? = null,
    val active: Boolean? = null,
    val assembly: Boolean? = null,
    val component: Boolean? = null,
    val purchaseable: Boolean? = null,
    val salable: Boolean? = null,
    val trackable: Boolean? = null,
    val virtual: Boolean? = null,
    val consumable: Boolean? = null,
    val keywords: String? = null,
    val link: String? = null,
    val notes: String? = null,
    val default_location: Int? = null,
)

@Serializable
data class LocationCreateRequest(
    val name: String,
    val description: String = "",
    val parent: Int? = null,
    val structural: Boolean = false,
    val external: Boolean = false,
    val location_type: Int? = null,
)

@Serializable
data class LocationDeleteRequest(
    val delete_stock_items: Boolean = false,
    val delete_sub_locations: Boolean = false,
)

@Serializable
data class LocationPatchRequest(
    val name: String? = null,
    val description: String? = null,
    val parent: Int? = null,
    val structural: Boolean? = null,
    val external: Boolean? = null,
    val location_type: Int? = null,
)

@Serializable
data class BarcodeScanResponse(
    val stockitem: BarcodeRef? = null,
    val part: BarcodeRef? = null,
    val stocklocation: BarcodeRef? = null,
    val success: String? = null,
    val error: String? = null,
    val plugin: String? = null,
    @SerialName("barcode_data") val barcodeData: String? = null,
    val raw: JsonObject? = null,
)

@Serializable
data class BarcodeRef(
    val pk: Int = 0,
    @SerialName("api_url") val apiUrl: String? = null,
    @SerialName("web_url") val webUrl: String? = null,
)

@Serializable
data class DashboardCounts(
    val parts: Int = 0,
    val stockItems: Int = 0,
    val locations: Int = 0,
    val lowStock: Int = 0,
)

data class Session(
    val serverUrl: String,
    val token: String,
    val user: UserDto,
)

data class StoredSession(
    val serverUrl: String,
    val token: String?,
    val user: UserDto?,
    val serverConfigured: Boolean = false,
)
