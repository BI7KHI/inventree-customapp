package xyz.bi7khi.inventree.mobile.data.label

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlin.math.max
import kotlin.math.min

object LabelRenderer {

    private val placeholder = Regex("\\{([a-zA-Z0-9_]+)\\}")

    fun render(
        design: LabelDesign,
        values: Map<String, String>,
        dpi: Int = 203,
    ): Bitmap {
        val dotsPerMm = dpi / 25.4f
        val width = max(1, (design.widthMm * dotsPerMm).toInt())
        val height = max(1, (design.heightMm * dotsPerMm).toInt())
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        if (design.border) {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = max(1f, 0.4f * dotsPerMm)
            }
            val inset = paint.strokeWidth / 2f
            canvas.drawRect(inset, inset, width - inset, height - inset, paint)
        }

        design.elements.filter { it.visible }.forEach { element ->
            when (element.type) {
                LabelElementType.LINE -> drawLine(canvas, element, width, height, dotsPerMm)
                LabelElementType.TEXT -> drawText(canvas, element, values, width, height, dotsPerMm)
                LabelElementType.QR -> drawQr(canvas, element, values, width, height, dotsPerMm)
            }
        }
        return bitmap
    }

    private fun drawLine(
        canvas: Canvas,
        element: LabelElement,
        width: Int,
        height: Int,
        dotsPerMm: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            strokeWidth = max(1f, 0.3f * dotsPerMm)
        }
        val y = element.y * height
        canvas.drawLine(element.x * width, y, (element.x + element.width) * width, y, paint)
    }

    private fun drawText(
        canvas: Canvas,
        element: LabelElement,
        values: Map<String, String>,
        width: Int,
        height: Int,
        dotsPerMm: Float,
    ) {
        val text = resolve(element.text, values)
        if (text.isBlank()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textSize = element.fontSizeMm * dotsPerMm
            typeface = Typeface.create(
                Typeface.SANS_SERIF,
                if (element.bold) Typeface.BOLD else Typeface.NORMAL,
            )
        }
        val left = element.x * width
        val top = element.y * height
        val maxWidth = element.width * width
        val lineHeight = paint.textSize * 1.18f
        val lines = text.split("\n")
        canvas.save()
        canvas.clipRect(left, top, left + maxWidth, top + element.height * height + lineHeight)
        lines.forEachIndexed { index, line ->
            val fitted = fitText(line, paint, maxWidth)
            val dx = when (element.align.lowercase()) {
                "center" -> left + (maxWidth - paint.measureText(fitted)) / 2f
                "right" -> left + maxWidth - paint.measureText(fitted)
                else -> left
            }
            val baseline = top + paint.textSize + index * lineHeight
            if (baseline <= top + element.height * height + paint.textSize) {
                canvas.drawText(fitted, dx, baseline, paint)
            }
        }
        canvas.restore()
    }

    private fun drawQr(
        canvas: Canvas,
        element: LabelElement,
        values: Map<String, String>,
        width: Int,
        height: Int,
        dotsPerMm: Float,
    ) {
        val content = resolve(element.text, values).ifBlank { values["qr"].orEmpty() }
        if (content.isBlank()) return
        val boxWidth = element.width * width
        val boxHeight = element.height * height
        val side = min(boxWidth, boxHeight)
        val size = max(1, side.toInt())
        val matrix = runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                size,
                size,
                mapOf(
                    EncodeHintType.MARGIN to 0,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                ),
            )
        }.getOrNull() ?: return
        val left = element.x * width + (boxWidth - side) / 2f
        val top = element.y * height + (boxHeight - side) / 2f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
        val cell = side / matrix.width
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                if (matrix[x, y]) {
                    canvas.drawRect(
                        left + x * cell,
                        top + y * cell,
                        left + (x + 1) * cell + 0.4f,
                        top + (y + 1) * cell + 0.4f,
                        paint,
                    )
                }
            }
        }
    }

    private fun fitText(text: String, paint: Paint, maxWidth: Float): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "…"
        var end = text.length
        while (end > 0) {
            val candidate = text.substring(0, end).trimEnd() + ellipsis
            if (paint.measureText(candidate) <= maxWidth) return candidate
            end--
        }
        return ellipsis
    }

    fun resolve(template: String, values: Map<String, String>): String =
        placeholder.replace(template) { match ->
            values[match.groupValues[1]].orEmpty()
        }

    fun sampleValues(type: LabelTemplateType): Map<String, String> = when (type) {
        LabelTemplateType.STOCK -> mapOf(
            "name" to "STM32F103C8T6 最小系统板",
            "ipn" to "INV-0042",
            "description" to "ARM Cortex-M3 核心板",
            "quantity" to "12",
            "batch" to "B240918",
            "serial" to "SN-0012",
            "location" to "SMT零件盒 / A-03",
            "code" to "INV-SI178",
            "qr" to "INV-SI178",
            "url" to "https://erp.bi7khi.xyz/web/stock/item/178",
            "date" to "2026-11-24",
        )
        LabelTemplateType.PART -> mapOf(
            "name" to "STM32F103C8T6",
            "ipn" to "INV-0042",
            "description" to "ARM Cortex-M3 48-LQFP",
            "category" to "MCU / STM32",
            "quantity" to "36",
            "code" to "INV-PA42",
            "qr" to "INV-PA42",
            "url" to "https://erp.bi7khi.xyz/web/part/42",
            "date" to "2026-11-24",
        )
        LabelTemplateType.LOCATION -> mapOf(
            "name" to "SMT零件盒",
            "description" to "贴片元件分格收纳",
            "parent" to "广东工业大学.../东一",
            "path" to "东一/SMT零件盒",
            "items" to "7",
            "code" to "INV-SL7",
            "qr" to "INV-SL7",
            "url" to "https://erp.bi7khi.xyz/web/stock/location/7",
            "date" to "2026-11-24",
        )
    }

    fun elementRect(element: LabelElement, width: Float, height: Float): RectF =
        RectF(
            element.x * width,
            element.y * height,
            (element.x + element.width) * width,
            (element.y + element.height) * height,
        )
}
