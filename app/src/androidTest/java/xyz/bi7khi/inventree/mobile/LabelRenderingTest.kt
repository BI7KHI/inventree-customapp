package xyz.bi7khi.inventree.mobile

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt
import xyz.bi7khi.inventree.mobile.data.label.DefaultLabelDesigns
import xyz.bi7khi.inventree.mobile.data.label.LabelPrintSettings
import xyz.bi7khi.inventree.mobile.data.label.LabelProtocolEncoder
import xyz.bi7khi.inventree.mobile.data.label.LabelRenderer
import xyz.bi7khi.inventree.mobile.data.label.LabelTemplateType

@RunWith(AndroidJUnit4::class)
class LabelRenderingTest {

    @Test
    fun rendererDrawsTextAndQrOn40x30Label() {
        val design = DefaultLabelDesigns.forType(LabelTemplateType.STOCK)
        val bitmap = LabelRenderer.render(
            design,
            LabelRenderer.sampleValues(LabelTemplateType.STOCK),
            203,
        )
        assertTrue(bitmap.width in 310..330)
        assertTrue(bitmap.height in 230..250)

        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val black = pixels.count { Color.red(it) < 80 && Color.green(it) < 80 && Color.blue(it) < 80 }
        assertTrue("expected visible black label content, got $black pixels", black > 1000)
    }

    @Test
    fun offsetMovesRenderedLabelContent() {
        val design = DefaultLabelDesigns.forType(LabelTemplateType.STOCK)
        val values = LabelRenderer.sampleValues(LabelTemplateType.STOCK)
        val normal = LabelRenderer.render(design, values, 203)
        val shifted = LabelRenderer.render(design, values, 203, offsetXmm = 2.5f, offsetYmm = 1.5f)
        assertTrue("offset must change the rendered bitmap", !normal.sameAs(shifted))
    }

    @Test
    fun positiveHorizontalOffsetPadsLeftWithoutShrinkingLabel() {
        val design = DefaultLabelDesigns.forType(LabelTemplateType.STOCK)
        val values = LabelRenderer.sampleValues(LabelTemplateType.STOCK)
        val normal = LabelRenderer.render(design, values, 203)
        val shifted = LabelRenderer.render(design, values, 203, offsetXmm = 6f)
        val expectedExtra = (6f * 203f / 25.4f).roundToInt()
        assertEquals(normal.width + expectedExtra, shifted.width)
        assertEquals(normal.height, shifted.height)
    }

    @Test
    fun escPosStreamHasSingleResetAndBitmapCommand() {
        val bitmap = testBitmap()
        val one = LabelProtocolEncoder.encode(
            bitmap,
            LabelPrintSettings(protocol = "ESC/POS", dpi = 203, copies = 1),
            80f,
            60f,
        )
        assertEquals(0x1B, one[0].toInt() and 0xFF)
        assertEquals(0x40, one[1].toInt() and 0xFF)
        assertEquals(0x1D, one[2].toInt() and 0xFF)
        assertEquals(0x76, one[3].toInt() and 0xFF)
        assertEquals(0x0C, one.last().toInt() and 0xFF)

        val three = LabelProtocolEncoder.encode(
            bitmap,
            LabelPrintSettings(protocol = "ESC/POS", dpi = 203, copies = 3),
            80f,
            60f,
        )
        val resetCount = three.toList().windowed(2).count {
            it[0] == 0x1B.toByte() && it[1] == 0x40.toByte()
        }
        assertEquals("ESC @ must only be sent once per job", 1, resetCount)
        assertTrue(three.size > one.size * 2)
    }

    @Test
    fun tspl2StreamStartsWithSizeAndEndsWithPrint() {
        val bytes = LabelProtocolEncoder.encode(
            testBitmap(),
            LabelPrintSettings(protocol = "TSPL2", dpi = 203),
            40f,
            30f,
        )
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.startsWith("SIZE 40.0 mm,30.0 mm"))
        assertTrue(text.contains("BITMAP 0,0,"))
        assertTrue(text.endsWith("PRINT 1,1\r\n"))
    }

    private fun testBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawRect(5f, 5f, 75f, 55f, Paint().apply { color = Color.BLACK })
        return bitmap
    }
}
