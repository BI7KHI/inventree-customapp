package xyz.bi7khi.inventree.mobile.data.label

import java.time.LocalDate
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository

suspend fun InvenTreeRepository.loadLabelDocument(
    type: LabelTemplateType,
    itemId: Int?,
): LabelDocument {
    if (itemId == null || itemId <= 0) {
        return LabelDocument(
            type = type,
            title = "标签预览",
            subtitle = "使用示例数据",
            values = LabelRenderer.sampleValues(type),
            qrContent = LabelRenderer.sampleValues(type)["qr"].orEmpty(),
            barcodeSource = "sample",
        )
    }
    return when (type) {
        LabelTemplateType.STOCK -> {
            val item = stockItem(itemId)
            val code = runCatching { generateBarcode("stockitem", itemId) }
                .getOrElse { "INV-SI$itemId" }
            val part = item.part_detail
            val values = mapOf(
                "name" to (part?.name ?: item.partName),
                "ipn" to (part?.IPN.orEmpty()),
                "description" to (part?.description.orEmpty()),
                "category" to "",
                "quantity" to formatQuantityForLabel(item.quantity),
                "batch" to item.batch.orEmpty(),
                "serial" to item.serial.orEmpty(),
                "location" to item.locationName,
                "code" to code,
                "qr" to code,
                "url" to "${serverUrl}web/stock/item/$itemId",
                "date" to LocalDate.now().toString(),
            )
            LabelDocument(
                type = type,
                title = "库存标签",
                subtitle = item.partName,
                values = values,
                qrContent = code,
                barcodeSource = code,
            )
        }
        LabelTemplateType.PART -> {
            val part = part(itemId)
            val code = runCatching { generateBarcode("part", itemId) }
                .getOrElse { "INV-PA$itemId" }
            val values = mapOf(
                "name" to part.name,
                "ipn" to part.IPN.orEmpty(),
                "description" to part.description,
                "category" to (part.category_path ?: part.category_name.orEmpty()),
                "quantity" to formatQuantityForLabel(part.total_in_stock ?: 0.0),
                "code" to code,
                "qr" to code,
                "url" to "${serverUrl}web/part/$itemId",
                "date" to LocalDate.now().toString(),
            )
            LabelDocument(
                type = type,
                title = "零件标签",
                subtitle = part.name,
                values = values,
                qrContent = code,
                barcodeSource = code,
            )
        }
        LabelTemplateType.LOCATION -> {
            val location = location(itemId)
            val parent = location.parent?.let { parentId ->
                runCatching {
                    allLocations().firstOrNull { it.pk == parentId }?.name
                }.getOrNull()
            }.orEmpty()
            val code = runCatching { generateBarcode("stocklocation", itemId) }
                .getOrElse { "INV-SL$itemId" }
            val values = mapOf(
                "name" to location.name,
                "description" to location.description,
                "parent" to parent,
                "path" to (location.pathstring ?: location.name),
                "items" to location.items.toString(),
                "code" to code,
                "qr" to code,
                "url" to "${serverUrl}web/stock/location/$itemId",
                "date" to LocalDate.now().toString(),
            )
            LabelDocument(
                type = type,
                title = "库位标签",
                subtitle = location.pathstring ?: location.name,
                values = values,
                qrContent = code,
                barcodeSource = code,
            )
        }
    }
}

private fun formatQuantityForLabel(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toBigDecimal().stripTrailingZeros().toPlainString()
