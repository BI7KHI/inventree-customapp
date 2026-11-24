package xyz.bi7khi.inventree.mobile.data.label

import kotlinx.serialization.Serializable

enum class LabelTemplateType(val key: String, val displayName: String, val apiModel: String) {
    STOCK("stock", "库存标签", "stockitem"),
    PART("part", "零件标签", "part"),
    LOCATION("location", "库位标签", "stocklocation");

    companion object {
        fun fromKey(key: String?): LabelTemplateType =
            entries.firstOrNull { it.key == key } ?: STOCK
    }
}

@Serializable
enum class LabelElementType { TEXT, QR, LINE }

@Serializable
data class LabelElement(
    val id: String,
    val type: LabelElementType,
    val text: String = "",
    val x: Float = 0.04f,
    val y: Float = 0.05f,
    val width: Float = 0.92f,
    val height: Float = 0.1f,
    val fontSizeMm: Float = 2.5f,
    val bold: Boolean = false,
    val align: String = "left",
    val visible: Boolean = true,
)

@Serializable
data class LabelDesign(
    val name: String = "40 × 30 mm",
    val widthMm: Float = 40f,
    val heightMm: Float = 30f,
    val border: Boolean = true,
    val elements: List<LabelElement> = emptyList(),
)

@Serializable
data class LabelPrintSettings(
    val protocol: String = "ESC/POS",
    val dpi: Int = 203,
    val copies: Int = 1,
    val feedMm: Float = 0f,
    val threshold: Int = 128,
    val invert: Boolean = false,
    val writeChunkSize: Int = 180,
    val writeChunkDelayMs: Int = 15,
) {
    val protocolOptions get() = listOf("ESC/POS", "TSPL2")
    val dpiOptions get() = listOf(203, 300)
}

data class LabelDocument(
    val type: LabelTemplateType,
    val title: String,
    val subtitle: String,
    val values: Map<String, String>,
    val qrContent: String,
    val barcodeSource: String,
)

object DefaultLabelDesigns {

    fun forType(type: LabelTemplateType): LabelDesign = when (type) {
        LabelTemplateType.STOCK -> stock()
        LabelTemplateType.PART -> part()
        LabelTemplateType.LOCATION -> location()
    }

    private fun stock() = LabelDesign(
        name = "库存标签 40×30",
        elements = listOf(
            text("title", "{name}", 0.04f, 0.02f, 0.92f, 0.15f, 3.2f, true, "center"),
            line("head-sep", 0.18f),
            text("ipn", "IPN  {ipn}", 0.04f, 0.23f, 0.58f, 0.09f, 2.4f, true),
            text("batch", "批号  {batch}", 0.04f, 0.34f, 0.58f, 0.09f, 2.4f),
            text("qty", "数量  {quantity}", 0.04f, 0.45f, 0.58f, 0.1f, 2.6f, true),
            text("serial", "序列号 {serial}", 0.04f, 0.56f, 0.58f, 0.09f, 2.2f),
            qr("qr", "{qr}", 0.64f, 0.25f, 0.32f, 0.52f),
            line("foot-sep", 0.82f),
            text("foot", "{code}", 0.04f, 0.84f, 0.92f, 0.1f, 1.9f, false, "center"),
        ),
    )

    private fun part() = LabelDesign(
        name = "零件标签 40×30",
        elements = listOf(
            text("title", "{name}", 0.04f, 0.02f, 0.92f, 0.15f, 3.2f, true, "center"),
            line("head-sep", 0.18f),
            text("ipn", "IPN  {ipn}", 0.04f, 0.24f, 0.58f, 0.09f, 2.5f, true),
            text("desc", "描述  {description}", 0.04f, 0.36f, 0.58f, 0.12f, 2.2f),
            text("category", "分类  {category}", 0.04f, 0.5f, 0.58f, 0.1f, 2.2f),
            text("qty", "库存  {quantity}", 0.04f, 0.61f, 0.58f, 0.1f, 2.4f, true),
            qr("qr", "{qr}", 0.65f, 0.27f, 0.3f, 0.48f),
            line("foot-sep", 0.82f),
            text("foot", "{ipn}", 0.04f, 0.84f, 0.92f, 0.1f, 1.9f, false, "center"),
        ),
    )

    private fun location() = LabelDesign(
        name = "库位标签 40×30",
        elements = listOf(
            text("title", "{name}", 0.04f, 0.02f, 0.92f, 0.15f, 3.2f, true, "center"),
            line("head-sep", 0.18f),
            text("desc", "描述  {description}", 0.04f, 0.25f, 0.58f, 0.12f, 2.3f),
            text("parent", "上级  {parent}", 0.04f, 0.4f, 0.58f, 0.1f, 2.3f),
            text("items", "库存项 {items}", 0.04f, 0.52f, 0.58f, 0.1f, 2.4f, true),
            qr("qr", "{qr}", 0.65f, 0.27f, 0.3f, 0.48f),
            line("foot-sep", 0.82f),
            text("foot", "{path}", 0.04f, 0.84f, 0.92f, 0.1f, 1.8f, false, "center"),
        ),
    )

    private fun text(
        id: String,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        size: Float,
        bold: Boolean = false,
        align: String = "left",
    ) = LabelElement(
        id = id,
        type = LabelElementType.TEXT,
        text = text,
        x = x,
        y = y,
        width = width,
        height = height,
        fontSizeMm = size,
        bold = bold,
        align = align,
    )

    private fun qr(id: String, text: String, x: Float, y: Float, width: Float, height: Float) =
        LabelElement(
            id = id,
            type = LabelElementType.QR,
            text = text,
            x = x,
            y = y,
            width = width,
            height = height,
        )

    private fun line(id: String, y: Float) = LabelElement(
        id = id,
        type = LabelElementType.LINE,
        y = y,
        x = 0.04f,
        width = 0.92f,
        height = 0.002f,
    )
}
