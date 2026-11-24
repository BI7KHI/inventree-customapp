package xyz.bi7khi.inventree.mobile.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette
import xyz.bi7khi.inventree.mobile.ui.theme.contrastOn

private data class ThemeSwatch(val name: String, val color: Color)

private val themeSwatches = listOf(
    ThemeSwatch("谷地黄", Color(0xFFF2E900)),
    ThemeSwatch("武陵青", Color(0xFF19D3D3)),
    ThemeSwatch("重燃赤", Color(0xFFFF4B32)),
    ThemeSwatch("浸蓝", Color(0xFF6E63FF)),
    ThemeSwatch("琥珀", Color(0xFFFFB000)),
    ThemeSwatch("酸绿", Color(0xFF9BD900)),
    ThemeSwatch("铜锈", Color(0xFFB86B3C)),
    ThemeSwatch("钢蓝", Color(0xFF3F6F8E)),
    ThemeSwatch("品红", Color(0xFFD6006E)),
    ThemeSwatch("深红", Color(0xFFB32414)),
    ThemeSwatch("苔绿", Color(0xFF4F6B2A)),
    ThemeSwatch("暮紫", Color(0xFF6A4C93)),
    ThemeSwatch("冰青", Color(0xFF7FE7E0)),
    ThemeSwatch("橙", Color(0xFFD35400)),
    ThemeSwatch("石墨", Color(0xFF455A64)),
    ThemeSwatch("午夜", Color(0xFF263238)),
)

@Composable
fun ThemeColorPickerDialog(
    initialColor: Color,
    onApply: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalFieldPalette.current
    val initialHsv = remember(initialColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(initialColor.toArgb(), it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var hexText by remember { mutableStateOf(initialColor.toHexValue()) }

    fun currentColor(): Color = Color(
        AndroidColor.HSVToColor(floatArrayOf(hue, saturation, value)),
    )

    fun setFromColor(color: Color) {
        val hsv = FloatArray(3)
        AndroidColor.colorToHSV(color.toArgb(), hsv)
        hue = hsv[0]
        saturation = hsv[1]
        value = hsv[2]
        hexText = color.toHexValue()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                FieldMicroLabel("SIGNAL // COLOR", color = palette.signalText)
                Spacer(Modifier.height(3.dp))
                Text("信号色", style = MaterialTheme.typography.titleLarge)
                Text(
                    "只切换界面中的信号与选中态；纸白、碳黑、网格与警示斜纹保持不变。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    "预设信号",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(9.dp))
                themeSwatches.chunked(4).forEachIndexed { rowIndex, row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.forEach { swatch ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(FieldCutSmall)
                                    .clickable { setFromColor(swatch.color) }
                                    .padding(vertical = 3.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(FieldCutSmall)
                                        .background(swatch.color)
                                        .border(
                                            width = if (swatch.color.toArgb() == currentColor().toArgb()) 2.dp else 1.dp,
                                            color = if (swatch.color.toArgb() == currentColor().toArgb()) palette.ink
                                            else palette.lineStrong.copy(alpha = 0.45f),
                                            shape = FieldCutSmall,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (swatch.color.toArgb() == currentColor().toArgb()) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = swatch.color.contrastOn(),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    swatch.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                )
                            }
                        }
                        repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                    if (rowIndex != themeSwatches.chunked(4).lastIndex) Spacer(Modifier.height(9.dp))
                }

                Spacer(Modifier.height(18.dp))
                Text(
                    "自定义信号",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                        .clip(FieldCut)
                        .border(1.dp, palette.line, FieldCut)
                        .pointerInput(Unit) {
                            fun update(position: Offset) {
                                saturation = (position.x / size.width).coerceIn(0f, 1f)
                                value = (1f - position.y / size.height).coerceIn(0f, 1f)
                                hexText = currentColor().toHexValue()
                            }
                            detectDragGestures(
                                onDragStart = ::update,
                                onDrag = { change, _ ->
                                    change.consume()
                                    update(change.position)
                                },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxWidth().height(168.dp)) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, color(hue, 1f, 1f))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val thumb = Offset(saturation * size.width, (1f - value) * size.height)
                        drawCircle(Color.White, radius = 8.dp.toPx(), center = thumb)
                        drawCircle(
                            Color.Black,
                            radius = 8.dp.toPx(),
                            center = thumb,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .clip(FieldCutSmall)
                        .pointerInput(Unit) {
                            fun update(position: Offset) {
                                hue = (position.x / size.width).coerceIn(0f, 0.9999f) * 360f
                                hexText = currentColor().toHexValue()
                            }
                            detectDragGestures(
                                onDragStart = ::update,
                                onDrag = { change, _ ->
                                    change.consume()
                                    update(change.position)
                                },
                            )
                        },
                ) {
                    Canvas(Modifier.fillMaxWidth().height(26.dp)) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                    Color.Blue, Color.Magenta, Color.Red,
                                ),
                            ),
                        )
                        val x = (hue / 360f) * size.width
                        drawCircle(Color.White, radius = 9.dp.toPx(), center = Offset(x, size.height / 2))
                        drawCircle(
                            Color.Black,
                            radius = 9.dp.toPx(),
                            center = Offset(x, size.height / 2),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()),
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .clip(FieldCutSmall)
                            .background(currentColor())
                            .border(1.dp, palette.lineStrong, FieldCutSmall),
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = hexText,
                        onValueChange = { input ->
                            val cleaned = input.uppercase().removePrefix("#").take(6)
                            hexText = cleaned
                            if (cleaned.length == 6) {
                                runCatching {
                                    Color(AndroidColor.parseColor("#$cleaned"))
                                }.onSuccess { setFromColor(it) }
                            }
                        },
                        label = { Text("HEX") },
                        prefix = { Text("#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = FieldCutSmall,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onApply(currentColor()) },
                shape = FieldCutSmall,
            ) { Text("应用信号色") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val default = Color(0xFFF2E900)
                    setFromColor(default)
                    onApply(default)
                }) { Text("恢复默认") }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

private fun color(hue: Float, saturation: Float, value: Float): Color =
    Color(
        AndroidColor.HSVToColor(
            floatArrayOf(
                ((hue % 360f) + 360f) % 360f,
                saturation.coerceIn(0f, 1f),
                value.coerceIn(0f, 1f),
            ),
        ),
    )

private fun Color.toHexValue(): String =
    String.format("%06X", 0xFFFFFF and toArgb())
