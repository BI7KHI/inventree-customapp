package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddBox
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.DashboardCounts
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.EmptyState
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.GradientHero
import xyz.bi7khi.inventree.mobile.ui.components.InitialAvatar
import xyz.bi7khi.inventree.mobile.ui.components.QuantityPill
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.formatQty
import xyz.bi7khi.inventree.mobile.util.UrlUtils

@Composable
fun HomeScreen(
    repository: InvenTreeRepository,
    userName: String,
    serverUrl: String,
    onOpenStock: (Int) -> Unit,
    onOpenPart: (Int) -> Unit,
    onOpenScanner: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenStockList: () -> Unit,
    onOpenPartList: () -> Unit,
    onOpenLocations: () -> Unit,
    onNewStock: () -> Unit,
    onNewPart: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var counts by remember { mutableStateOf(DashboardCounts()) }
    var recent by remember { mutableStateOf<List<StockItemDto>>(emptyList()) }
    var lowStock by remember { mutableStateOf<List<PartDto>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        error = null
        runCatching {
            val c = repository.dashboardCounts()
            val r = repository.recentStock(5)
            val l = repository.lowStockParts(5)
            Triple(c, r, l)
        }.onSuccess { (c, r, l) ->
            counts = c
            recent = r
            lowStock = l
            loading = false
            refreshing = false
        }.onFailure {
            error = it.toUserMessage()
            loading = false
            refreshing = false
        }
    }

    LaunchedEffect(Unit) { load() }

    if (loading) {
        StatusBarIcons(lightIcons = false)
        xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading("正在同步库存概览…")
        return
    }
    if (error != null) {
        ErrorState(error!!, modifier = Modifier.fillMaxSize(), onRetry = { scope.launch { loading = true; load() } })
        return
    }

    StatusBarIcons(lightIcons = true)
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..18 -> "下午好"
        else -> "晚上好"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            GradientHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(272.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = onOpenMore, color = Color.Transparent) {
                            InitialAvatar(userName, size = 44.dp)
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "$greeting，$userName",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                UrlUtils.displayServer(serverUrl),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.68f),
                            )
                        }
                        IconButton(
                            onClick = {
                                if (!refreshing) {
                                    refreshing = true
                                    scope.launch { load() }
                                }
                            },
                            enabled = !refreshing,
                        ) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "刷新",
                                tint = Color.White.copy(alpha = if (refreshing) 0.4f else 0.9f),
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))
                    Text(
                        "库存，一目了然",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (counts.lowStock > 0) "有 ${counts.lowStock} 个零件低于最小库存"
                        else "所有零件库存充足",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (counts.lowStock > 0) Color(0xFFFFD59B) else Color.White.copy(alpha = 0.75f),
                    )

                    Spacer(Modifier.height(20.dp))
                    Surface(
                        onClick = { onOpenStockList() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 15.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                            Spacer(Modifier.size(10.dp))
                            Text(
                                "搜索零件名称、IPN、序列号…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard(
                    value = counts.parts.toString(),
                    label = "零件",
                    icon = Icons.Outlined.Inventory2,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPartList,
                )
                StatCard(
                    value = counts.stockItems.toString(),
                    label = "库存项",
                    icon = Icons.Outlined.Warehouse,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStockList,
                )
                StatCard(
                    value = counts.locations.toString(),
                    label = "库位",
                    icon = Icons.Outlined.Place,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLocations,
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionHeader("快捷操作")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    QuickAction("扫码", Icons.Outlined.QrCodeScanner, Modifier.weight(1f), onOpenScanner)
                    QuickAction("新增库存", Icons.Outlined.AddBox, Modifier.weight(1f), onNewStock)
                    QuickAction("新建零件", Icons.Outlined.AddCircleOutline, Modifier.weight(1f), onNewPart)
                    QuickAction("库位", Icons.Outlined.Place, Modifier.weight(1f), onOpenLocations)
                }
            }
        }

        if (lowStock.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 18.dp)) {
                    SectionHeader("低库存提醒", actionLabel = "全部零件", onAction = onOpenPartList)
                    Spacer(Modifier.height(8.dp))
                    AppCard {
                        lowStock.forEachIndexed { index, part ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenPart(part.pk) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .background(MaterialTheme.colorScheme.errorContainer, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        part.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        part.IPN?.takeIf { it.isNotBlank() } ?: "无 IPN",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text(
                                    "${formatQty(part.total_in_stock ?: 0.0)} / ${formatQty(part.minimum_stock ?: 0.0)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                            if (index != lowStock.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 18.dp)) {
                SectionHeader("最近更新", actionLabel = "全部库存", onAction = onOpenStockList)
                Spacer(Modifier.height(8.dp))
                if (recent.isEmpty()) {
                    AppCard {
                        EmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = "还没有库存记录",
                            description = "扫码或新建一个库存项开始使用。",
                        )
                    }
                } else {
                    AppCard {
                        recent.forEachIndexed { index, item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenStock(item.pk) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                                Spacer(Modifier.size(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.partName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        item.locationName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.size(8.dp))
                                QuantityPill(item.quantity, item.part_detail?.units, compact = true)
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            if (index != recent.lastIndex) {
                                androidx.compose.material3.HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(10.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            onClick = onClick,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp),
        ) {
            Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
