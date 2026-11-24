package xyz.bi7khi.inventree.mobile.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCarbon
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutLarge
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.FieldDisplay
import xyz.bi7khi.inventree.mobile.ui.theme.FieldInk
import xyz.bi7khi.inventree.mobile.ui.theme.FieldMono
import xyz.bi7khi.inventree.mobile.ui.theme.FieldPalette
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette

/**
 * A very small original glyph for the app: an abstract stock tree / stacked
 * pallets inside a bracketed frame. It is not derived from any game logo.
 */
@Composable
fun FieldGlyph(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    accent: Color = Color(0xFFF2E900),
) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = w * 0.075f
        // three stacked shelves
        repeat(3) { i ->
            val y = h * (0.30f + i * 0.20f)
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.28f, y),
                end = androidx.compose.ui.geometry.Offset(w * 0.72f, y),
                strokeWidth = stroke,
                cap = StrokeCap.Square,
            )
            drawLine(
                color = if (i == 1) accent else color,
                start = androidx.compose.ui.geometry.Offset(w * 0.28f, y),
                end = androidx.compose.ui.geometry.Offset(w * 0.28f, y + h * 0.12f),
                strokeWidth = stroke,
            )
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(w * 0.72f, y),
                end = androidx.compose.ui.geometry.Offset(w * 0.72f, y + h * 0.12f),
                strokeWidth = stroke,
            )
        }
        // trunk
        drawLine(
            color = color,
            start = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.20f),
            end = androidx.compose.ui.geometry.Offset(w * 0.50f, h * 0.84f),
            strokeWidth = stroke,
        )
        // bracket frame
        val inset = w * 0.08f
        val len = w * 0.16f
        drawLine(accent, androidx.compose.ui.geometry.Offset(inset, inset + len), androidx.compose.ui.geometry.Offset(inset, inset), stroke)
        drawLine(accent, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + len, inset), stroke)
        drawLine(accent, androidx.compose.ui.geometry.Offset(w - inset - len, h - inset), androidx.compose.ui.geometry.Offset(w - inset, h - inset), stroke)
        drawLine(accent, androidx.compose.ui.geometry.Offset(w - inset, h - inset), androidx.compose.ui.geometry.Offset(w - inset, h - inset - len), stroke)
    }
}

@Composable
fun FieldMicroLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalFieldPalette.current.signalText,
    maxLines: Int = 1,
) {
    Text(
        text = text.uppercase(),
        modifier = modifier,
        color = color,
        fontFamily = FieldMono,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.3.sp,
        fontWeight = FontWeight.Medium,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
fun FieldPulseDot(
    color: Color = LocalFieldPalette.current.signal,
    size: Dp = 7.dp,
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse-alpha",
    )
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha)),
    )
}

@Composable
fun FieldStatusLine(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = LocalFieldPalette.current.signal,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FieldPulseDot(color = accent, size = 6.dp)
        Spacer(Modifier.width(8.dp))
        FieldMicroLabel(label, color = LocalFieldPalette.current.muted)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(LocalFieldPalette.current.line),
        )
        Spacer(Modifier.width(8.dp))
        FieldMicroLabel(value, color = accent)
    }
}

@Composable
fun FieldHazardStrip(
    modifier: Modifier = Modifier,
    color: Color = LocalFieldPalette.current.signal,
    height: Dp = 7.dp,
) {
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val stripe = 12.dp.toPx()
        var x = -size.height
        while (x < size.width + size.height) {
            drawLine(
                color = color,
                start = androidx.compose.ui.geometry.Offset(x, size.height),
                end = androidx.compose.ui.geometry.Offset(x + size.height, 0f),
                strokeWidth = 4.dp.toPx(),
            )
            x += stripe * 2
        }
    }
}

@Composable
fun FieldHero(
    modifier: Modifier = Modifier,
    showGhost: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = LocalFieldPalette.current
    val transition = rememberInfiniteTransition(label = "hero-scan")
    val scan by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hero-scan-y",
    )
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hero-grid-drift",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF090B09), Color(0xFF161A12), Color(0xFF0A0C0A)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val grid = 26.dp.toPx()
            val offset = (drift * grid) % grid
            var x = -grid + offset
            while (x < size.width + grid) {
                drawLine(
                    color = Color.White.copy(alpha = 0.055f),
                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                    end = androidx.compose.ui.geometry.Offset(x, size.height),
                    strokeWidth = 1f,
                )
                x += grid
            }
            var y = -grid + offset
            while (y < size.height + grid) {
                drawLine(
                    color = Color.White.copy(alpha = 0.045f),
                    start = androidx.compose.ui.geometry.Offset(0f, y),
                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                    strokeWidth = 1f,
                )
                y += grid
            }

            // Technical contour rings, clipped by the hero frame.
            val cx = size.width * 0.82f
            val cy = size.height * 0.26f
            repeat(4) { i ->
                drawCircle(
                    color = palette.cyan.copy(alpha = 0.055f + i * 0.012f),
                    radius = size.minDimension * (0.20f + i * 0.11f),
                    center = androidx.compose.ui.geometry.Offset(cx, cy),
                    style = Stroke(width = 1f),
                )
            }
            drawLine(
                color = palette.signal.copy(alpha = 0.28f),
                start = androidx.compose.ui.geometry.Offset(cx, 0f),
                end = androidx.compose.ui.geometry.Offset(cx, cy),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 7f)),
            )
            drawCircle(
                color = palette.signal.copy(alpha = 0.8f),
                radius = 2.5.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(cx, cy),
            )

            // Moving scan line.
            val sy = scan * size.height
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, palette.signal.copy(alpha = 0.055f), Color.Transparent),
                    startY = sy - 30.dp.toPx(),
                    endY = sy + 30.dp.toPx(),
                ),
                topLeft = androidx.compose.ui.geometry.Offset(0f, sy - 30.dp.toPx()),
                size = androidx.compose.ui.geometry.Size(size.width, 60.dp.toPx()),
            )
            drawLine(
                color = palette.signal.copy(alpha = 0.24f),
                start = androidx.compose.ui.geometry.Offset(0f, sy),
                end = androidx.compose.ui.geometry.Offset(size.width, sy),
                strokeWidth = 1f,
            )

            drawFrameCorners(
                color = palette.signal.copy(alpha = 0.7f),
                length = 18.dp.toPx(),
                stroke = 1.5.dp.toPx(),
            )

            // Bottom caution band.
            val band = 7.dp.toPx()
            val stripe = 12.dp.toPx()
            var sx = -band
            while (sx < size.width + band) {
                drawLine(
                    color = palette.signal.copy(alpha = 0.55f),
                    start = androidx.compose.ui.geometry.Offset(sx, size.height),
                    end = androidx.compose.ui.geometry.Offset(sx + band, size.height - band),
                    strokeWidth = 3.2.dp.toPx(),
                )
                sx += stripe * 2
            }
        }

        if (showGhost) {
            Text(
                text = "04",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp),
                fontFamily = FieldDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 78.sp,
                color = Color.White.copy(alpha = 0.045f),
            )
        }

        content()
    }
}

@Composable
fun FieldCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    accent: Color = LocalFieldPalette.current.signal,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalFieldPalette.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = spring(stiffness = 900f, dampingRatio = 0.8f),
        label = "field-card-press",
    )
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interaction,
            indication = null,
            onClick = onClick,
        )
    } else {
        Modifier
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(FieldCut)
            .then(clickModifier),
        shape = FieldCut,
        color = palette.panel,
        border = BorderStroke(1.dp, palette.line),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = accent,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(38.dp.toPx(), 0f),
                    strokeWidth = 2.dp.toPx(),
                )
                drawFrameCorners(
                    color = palette.lineStrong.copy(alpha = 0.65f),
                    length = 8.dp.toPx(),
                    stroke = 1f,
                    inset = 4.dp.toPx(),
                )
            }
            Column(
                modifier = Modifier.padding(contentPadding),
                content = content,
            )
        }
    }
}

@Composable
fun FieldSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    code: String = "01",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = LocalFieldPalette.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(25.dp)
                .border(1.dp, palette.signalText, FieldCutSmall),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                code,
                fontFamily = FieldMono,
                fontSize = 9.sp,
                color = palette.signalText,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(9.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = palette.ink,
        )
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(palette.line),
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.width(4.dp))
            Text(
                actionLabel,
                modifier = Modifier
                    .clip(FieldCutSmall)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                style = MaterialTheme.typography.labelMedium,
                color = palette.signalText,
            )
        }
    }
}

@Composable
fun FieldTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    code: String = "SYS // 01",
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val palette = LocalFieldPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(palette.paper, palette.paper.copy(alpha = 0.96f)),
                ),
            )
            .statusBarsPadding()
            .padding(horizontal = 12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    modifier = Modifier.size(38.dp),
                    shape = FieldCutSmall,
                    color = Color.Transparent,
                    border = BorderStroke(1.dp, palette.line),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = "返回",
                            tint = palette.ink,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }
                Spacer(Modifier.width(11.dp))
            }
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldMicroLabel(code, color = palette.signalText)
                    Spacer(Modifier.width(6.dp))
                    FieldPulseDot(size = 5.dp)
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                content = actions,
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(palette.line),
        ) {
            Box(
                Modifier
                    .width(52.dp)
                    .height(2.dp)
                    .background(palette.signal),
            )
        }
    }
}

data class FieldNavItem(
    val label: String,
    val code: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector = icon,
    val isAction: Boolean = false,
)

@Composable
fun FieldBottomBar(
    items: List<FieldNavItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFieldPalette.current
    val haptic = LocalHapticFeedback.current
    val background = if (palette.isDark) palette.panel else FieldInk
    val foreground = if (palette.isDark) palette.ink else Color(0xFFF3F1E7)
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = background,
        shadowElevation = 14.dp,
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Box(Modifier.fillMaxWidth().height(1.dp).background(palette.signal.copy(alpha = 0.45f)))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, item ->
                    val selected = index == selectedIndex
                    val color by animateColorAsState(
                        targetValue = if (selected) palette.signal else foreground.copy(alpha = 0.56f),
                        animationSpec = tween(180),
                        label = "nav-color-$index",
                    )
                    val lineWidth by animateFloatAsState(
                        targetValue = if (selected) 26f else 0f,
                        animationSpec = spring(stiffness = 700f, dampingRatio = 0.85f),
                        label = "nav-line-$index",
                    )
                    if (item.isAction) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(FieldCutSmall)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelect(index)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            FieldScanButton(selected = selected, onClick = { onSelect(index) })
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.label,
                                color = color,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.6.sp,
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(FieldCutSmall)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSelect(index)
                                },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                Modifier
                                    .width(lineWidth.dp)
                                    .height(2.dp)
                                    .background(if (selected) palette.signal else Color.Transparent),
                            )
                            Spacer(Modifier.height(5.dp))
                            Icon(
                                imageVector = if (selected) item.selectedIcon else item.icon,
                                contentDescription = item.label,
                                tint = color,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                item.label,
                                color = color,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 0.6.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FieldScanButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalFieldPalette.current
    val transition = rememberInfiniteTransition(label = "scan-pulse")
    val ring by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse),
        label = "scan-ring",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(46.dp)
                .graphicsLayer {
                    scaleX = ring
                    scaleY = ring
                    alpha = 0.15f
                }
                .border(1.dp, palette.signal, FieldCutSmall),
        )
        Surface(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp),
            color = if (selected) palette.signal else palette.panel,
            border = BorderStroke(1.dp, palette.signal),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Outlined.QrCodeScanner,
                    contentDescription = "扫描",
                    tint = if (selected) palette.onSignal else palette.signal,
                    modifier = Modifier.size(23.dp),
                )
            }
        }
    }
}

@Composable
fun FieldDivider(
    modifier: Modifier = Modifier,
    color: Color = LocalFieldPalette.current.line,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color),
    )
}

@Composable
fun FieldTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = LocalFieldPalette.current.signalText,
    filled: Boolean = false,
) {
    val palette = LocalFieldPalette.current
    Box(
        modifier = modifier
            .clip(FieldCutSmall)
            .background(if (filled) color else color.copy(alpha = 0.08f))
            .border(1.dp, color.copy(alpha = if (filled) 1f else 0.45f), FieldCutSmall)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        FieldMicroLabel(
            text,
            color = if (filled) palette.onSignal else color,
        )
    }
}

private fun DrawScope.drawFrameCorners(
    color: Color,
    length: Float,
    stroke: Float,
    inset: Float = 0f,
) {
    val w = size.width
    val h = size.height
    drawLine(color, androidx.compose.ui.geometry.Offset(inset, inset + length), androidx.compose.ui.geometry.Offset(inset, inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(inset, inset), androidx.compose.ui.geometry.Offset(inset + length, inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, inset + length), androidx.compose.ui.geometry.Offset(w - inset, inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, inset), androidx.compose.ui.geometry.Offset(w - inset - length, inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(inset, h - inset - length), androidx.compose.ui.geometry.Offset(inset, h - inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(inset, h - inset), androidx.compose.ui.geometry.Offset(inset + length, h - inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, h - inset - length), androidx.compose.ui.geometry.Offset(w - inset, h - inset), stroke)
    drawLine(color, androidx.compose.ui.geometry.Offset(w - inset, h - inset), androidx.compose.ui.geometry.Offset(w - inset - length, h - inset), stroke)
}
