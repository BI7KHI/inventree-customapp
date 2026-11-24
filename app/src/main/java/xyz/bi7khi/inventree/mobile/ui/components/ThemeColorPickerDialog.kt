package xyz.bi7khi.inventree.mobile.ui.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private data class ThemeSwatch(val name: String, val color: Color)

private val themeSwatches = listOf(
    ThemeSwatch("森林绿", Color(0xFF146B54)),
    ThemeSwatch("青碧", Color(0xFF00796B)),
    ThemeSwatch("海洋蓝", Color(0xFF1565C0)),
    ThemeSwatch("靛青", Color(0xFF303F9F)),
    ThemeSwatch("葡萄紫", Color(0xFF6A4C93)),
    ThemeSwatch("玫瑰红", Color(0xFFC2185B)),
    ThemeSwatch("珊瑚橙", Color(0xFFD35400)),
    ThemeSwatch("琥珀", Color(0xFFB26A00)),
    ThemeSwatch("石墨", Color(0xFF455A64)),
    ThemeSwatch("松柏", Color(0xFF2E5E4E)),
    ThemeSwatch("赭石", Color(0xFF8D5B4C)),
    ThemeSwatch("午夜", Color(0xFF263238)),
)

@Composable
fun ThemeColorPickerDialog(
    initialColor: Color,
    onApply: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
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
                Text("应用主题色")
                Text(
                    "颜色会应用到导航、按钮、首页渐变和强调元素",
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
                    "常用颜色",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                themeSwatches.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        row.forEach { swatch ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(64.dp),
                            ) {
                                Box(
                                    Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(swatch.color)
                                        .clickable { setFromColor(swatch.color) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (swatch.color.toArgb() == currentColor().toArgb()) {
                                        Icon(
                                            Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                        )
                                    }
                                }
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    swatch.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                Spacer(Modifier.height(6.dp))
                Text(
                    "自定义颜色",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(14.dp))
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
                    Canvas(Modifier.fillMaxWidth().height(176.dp)) {
                        drawRect(Brush.horizontalGradient(listOf(Color.White, color(hue, 1f, 1f))))
                        drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                        val thumb = Offset(saturation * size.width, (1f - value) * size.height)
                        drawCircle(Color.White, radius = 9.dp.toPx(), center = thumb)
                        drawCircle(Color.Black, radius = 9.dp.toPx(), center = thumb, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    }
                }

                Spacer(Modifier.height(10.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(50))
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
                    Canvas(Modifier.fillMaxWidth().height(28.dp)) {
                        drawRect(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Red, Color.Yellow, Color.Green, Color.Cyan,
                                    Color.Blue, Color.Magenta, Color.Red,
                                ),
                            ),
                        )
                        val x = (hue / 360f) * size.width
                        drawCircle(Color.White, radius = 10.dp.toPx(), center = Offset(x, size.height / 2))
                        drawCircle(
                            Color.Black,
                            radius = 10.dp.toPx(),
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
                            .clip(CircleShape)
                            .background(currentColor()),
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
                        label = { Text("十六进制") },
                        prefix = { Text("#") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onApply(currentColor())
            }) { Text("应用") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val default = Color(0xFF146B54)
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
