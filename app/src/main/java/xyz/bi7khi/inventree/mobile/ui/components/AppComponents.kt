package xyz.bi7khi.inventree.mobile.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import xyz.bi7khi.inventree.mobile.ui.theme.Danger
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCarbon
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette
import xyz.bi7khi.inventree.mobile.ui.theme.SoftBlue
import xyz.bi7khi.inventree.mobile.ui.theme.Success
import xyz.bi7khi.inventree.mobile.ui.theme.Warning

@Composable
fun BrandMark(
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
    backgroundColor: Color? = null,
    iconColor: Color = Color.White,
) {
    val palette = LocalFieldPalette.current
    val background = backgroundColor ?: FieldCarbon
    Box(
        modifier = modifier
            .size(size)
            .clip(FieldCutSmall)
            .background(background)
            .border(1.dp, palette.signal.copy(alpha = 0.75f), FieldCutSmall),
        contentAlignment = Alignment.Center,
    ) {
        FieldGlyph(
            modifier = Modifier.size(size * 0.60f),
            color = iconColor,
            accent = palette.signal,
        )
    }
}

@Composable
fun GradientHero(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit,
) {
    FieldHero(
        modifier = modifier,
        content = content,
    )
}

@Composable
fun InvenTreeTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {},
) {
    FieldTopBar(
        title = title,
        subtitle = "INVENTREE MOBILE",
        code = "SYS // LOCAL",
        onBack = onBack,
        actions = actions,
    )
}

@Composable
fun InitialAvatar(name: String, size: Dp = 42.dp) {
    val palette = LocalFieldPalette.current
    val initials = name.trim().take(2).uppercase().ifBlank { "IT" }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(FieldCarbon)
            .border(1.dp, palette.signal.copy(alpha = 0.85f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initials,
            color = palette.signal,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.34f).sp,
            letterSpacing = 0.5.sp,
        )
    }
}

@Composable
fun FullScreenLoading(message: String = "正在加载…") {
    val palette = LocalFieldPalette.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    modifier = Modifier.size(52.dp),
                    strokeWidth = 1.5.dp,
                    color = palette.signalText,
                )
                FieldMicroLabel("SYNC", color = palette.signalText)
            }
            Spacer(Modifier.height(18.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = palette.muted)
            Spacer(Modifier.height(5.dp))
            FieldMicroLabel("FETCHING REMOTE MANIFEST", color = palette.muted.copy(alpha = 0.65f))
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    val palette = LocalFieldPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(74.dp)
                .clip(FieldCut)
                .background(palette.danger.copy(alpha = 0.12f))
                .border(1.dp, palette.danger.copy(alpha = 0.55f), FieldCut),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.CloudOff,
                contentDescription = null,
                tint = palette.danger,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        FieldMicroLabel("LINK // FAULT", color = palette.danger)
        Spacer(Modifier.height(6.dp))
        Text(
            "连接中断",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            Spacer(Modifier.height(18.dp))
            Button(onClick = onRetry, shape = FieldCutSmall) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("重新连接")
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val palette = LocalFieldPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(76.dp)
                .clip(FieldCut)
                .background(palette.panelAlt)
                .border(1.dp, palette.line, FieldCut),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.signalText, modifier = Modifier.size(34.dp))
        }
        Spacer(Modifier.height(16.dp))
        FieldMicroLabel("NO DATA // 00", color = palette.muted)
        Spacer(Modifier.height(5.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.muted,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = onAction, shape = FieldCutSmall) { Text(actionLabel) }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    FieldSectionHeader(
        title = title,
        modifier = modifier,
        code = "S",
        actionLabel = actionLabel,
        onAction = onAction,
    )
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    FieldCard(
        modifier = modifier.animateContentSize(),
        onClick = onClick,
        contentPadding = 16.dp,
        content = content,
    )
}

@Composable
fun StatusBadge(status: Int?, text: String?) {
    val palette = LocalFieldPalette.current
    val label = text?.takeIf { it.isNotBlank() } ?: when (status) {
        10 -> "正常"
        50 -> "需关注"
        55 -> "损坏"
        60 -> "已销毁"
        65 -> "已拒收"
        70 -> "丢失"
        75 -> "隔离"
        85 -> "已退货"
        else -> "未知"
    }
    val color = when (status) {
        10 -> Success
        50 -> Warning
        85 -> SoftBlue
        55, 60, 65, 70, 75 -> Danger
        else -> palette.muted
    }
    Surface(
        color = color.copy(alpha = 0.11f),
        shape = FieldCutSmall,
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f)),
    ) {
        Text(
            label,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun QuantityPill(quantity: Double, units: String? = null, compact: Boolean = false) {
    val palette = LocalFieldPalette.current
    Surface(
        color = palette.signal.copy(alpha = 0.13f),
        shape = FieldCutSmall,
        border = BorderStroke(1.dp, palette.signalText.copy(alpha = 0.22f)),
    ) {
        Row(
            Modifier.padding(horizontal = if (compact) 8.dp else 10.dp, vertical = if (compact) 3.dp else 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Text(
                text = formatQty(quantity),
                style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = palette.signalText,
            )
            if (!units.isNullOrBlank()) {
                Text(
                    " $units",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.signalText.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String?,
    modifier: Modifier = Modifier,
    valueColor: Color = MaterialTheme.colorScheme.onSurface,
) {
    if (value.isNullOrBlank()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = LocalFieldPalette.current.muted,
            modifier = Modifier.width(88.dp),
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "确认",
    destructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = FieldCutSmall,
                colors = if (destructive) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                else ButtonDefaults.buttonColors(),
            ) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

fun formatQty(value: Double): String {
    if (value.isNaN() || value.isInfinite()) return "0"
    return if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toBigDecimal().stripTrailingZeros().toPlainString()
}
