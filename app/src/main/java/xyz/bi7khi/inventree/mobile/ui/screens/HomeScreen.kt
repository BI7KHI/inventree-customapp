package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import xyz.bi7khi.inventree.mobile.ui.components.FieldCard
import xyz.bi7khi.inventree.mobile.ui.components.FieldHero
import xyz.bi7khi.inventree.mobile.ui.components.FieldMicroLabel
import xyz.bi7khi.inventree.mobile.ui.components.FieldPulseDot
import xyz.bi7khi.inventree.mobile.ui.components.FieldSectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.FieldTag
import xyz.bi7khi.inventree.mobile.ui.components.InitialAvatar
import xyz.bi7khi.inventree.mobile.ui.components.QuantityPill
import xyz.bi7khi.inventree.mobile.ui.components.formatQty
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.FieldDisplay
import xyz.bi7khi.inventree.mobile.ui.theme.FieldMono
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette
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
    val palette = LocalFieldPalette.current
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

    val listState = rememberLazyListState()
    val isAtTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset < 280
        }
    }
    StatusBarIcons(lightIcons = isAtTop)
    val greeting = when (LocalTime.now().hour) {
        in 5..10 -> "早上好"
        in 11..13 -> "中午好"
        in 14..18 -> "下午好"
        else -> "晚上好"
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .background(palette.paper),
        contentPadding = PaddingValues(bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            FieldHero(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(304.dp),
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = onOpenMore, color = Color.Transparent, shape = CircleShape) {
                            InitialAvatar(userName, size = 44.dp)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            FieldMicroLabel(
                                "OPERATOR // ACTIVE",
                                color = palette.signal,
                            )
                            Text(
                                "$greeting，$userName",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                UrlUtils.displayServer(serverUrl),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.52f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Surface(
                            onClick = {
                                if (!refreshing) {
                                    refreshing = true
                                    scope.launch { load() }
                                }
                            },
                            modifier = Modifier.size(38.dp),
                            shape = FieldCutSmall,
                            color = Color.White.copy(alpha = 0.04f),
                            border = BorderStroke(
                                1.dp,
                                palette.signal.copy(alpha = if (refreshing) 0.25f else 0.55f),
                            ),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.Refresh,
                                    contentDescription = "刷新",
                                    tint = palette.signal.copy(alpha = if (refreshing) 0.35f else 0.9f),
                                    modifier = Modifier.size(19.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    FieldMicroLabel("MATERIAL CONTROL // LIVE", color = palette.signal)
                    Spacer(Modifier.height(5.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "库存态势",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(10.dp))
                        Box(
                            Modifier
                                .padding(bottom = 5.dp)
                                .clip(FieldCutSmall)
                                .background(palette.signal)
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                "LIVE",
                                fontFamily = FieldDisplay,
                                fontSize = 9.sp,
                                letterSpacing = 1.sp,
                                color = palette.onSignal,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (counts.lowStock > 0) "警告 · ${counts.lowStock} 个零件低于最小库存"
                        else "全系统正常 · 所有零件库存充足",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (counts.lowStock > 0) palette.signal else Color.White.copy(alpha = 0.62f),
                    )

                    Spacer(Modifier.height(15.dp))
                    Surface(
                        onClick = { onOpenStockList() },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White.copy(alpha = 0.035f),
                        shape = FieldCut,
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = palette.signal,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(9.dp))
                            Text(
                                "搜索零件、IPN、序列号…",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.56f),
                                modifier = Modifier.weight(1f),
                            )
                            FieldTag("SCAN", color = palette.signal)
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatCard(
                    value = counts.parts.toString(),
                    label = "零件",
                    code = "P-01",
                    icon = Icons.Outlined.Inventory2,
                    accent = palette.signalText,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenPartList,
                )
                StatCard(
                    value = counts.stockItems.toString(),
                    label = "库存项",
                    code = "S-02",
                    icon = Icons.Outlined.Warehouse,
                    accent = palette.cyanText,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenStockList,
                )
                StatCard(
                    value = counts.locations.toString(),
                    label = "库位",
                    code = "L-03",
                    icon = Icons.Outlined.Place,
                    accent = palette.lineStrong,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenLocations,
                )
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                FieldSectionHeader(title = "快捷操作", code = "A", actionLabel = "扫描指令", onAction = onOpenScanner)
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    QuickAction("扫码", "A1", Icons.Outlined.QrCodeScanner, Modifier.weight(1f), onOpenScanner)
                    QuickAction("新增库存", "A2", Icons.Outlined.AddBox, Modifier.weight(1f), onNewStock)
                    QuickAction("新建零件", "A3", Icons.Outlined.AddCircleOutline, Modifier.weight(1f), onNewPart)
                    QuickAction("库位", "A4", Icons.Outlined.Place, Modifier.weight(1f), onOpenLocations)
                }
            }
        }

        if (lowStock.isNotEmpty()) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    FieldSectionHeader(
                        title = "低库存警告",
                        code = "!",
                        actionLabel = "全部零件",
                        onAction = onOpenPartList,
                    )
                    Spacer(Modifier.height(8.dp))
                    FieldCard(accent = palette.danger) {
                        lowStock.forEachIndexed { index, part ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(FieldCutSmall)
                                    .clickable { onOpenPart(part.pk) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(FieldCutSmall)
                                        .background(palette.danger.copy(alpha = 0.10f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = palette.danger,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                                Spacer(Modifier.width(11.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        part.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        part.IPN?.takeIf { it.isNotBlank() } ?: "NO IPN / UNREGISTERED",
                                        fontFamily = FieldMono,
                                        fontSize = 10.sp,
                                        color = palette.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Text(
                                    "${formatQty(part.total_in_stock ?: 0.0)} / ${formatQty(part.minimum_stock ?: 0.0)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = palette.danger,
                                )
                            }
                            if (index != lowStock.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.line.copy(alpha = 0.55f)),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(Modifier.padding(horizontal = 16.dp)) {
                FieldSectionHeader(
                    title = "最近更新",
                    code = "R",
                    actionLabel = "全部库存",
                    onAction = onOpenStockList,
                )
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
                    FieldCard {
                        recent.forEachIndexed { index, item ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(FieldCutSmall)
                                    .clickable { onOpenStock(item.pk) }
                                    .padding(vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(FieldCutSmall)
                                        .background(palette.panelAlt),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        (index + 1).toString().padStart(2, '0'),
                                        fontFamily = FieldDisplay,
                                        fontWeight = FontWeight.Bold,
                                        color = palette.signalText,
                                        fontSize = 14.sp,
                                    )
                                }
                                Spacer(Modifier.width(11.dp))
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
                                        color = palette.muted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                QuantityPill(item.quantity, item.part_detail?.units, compact = true)
                                Icon(
                                    Icons.Outlined.ChevronRight,
                                    contentDescription = null,
                                    tint = palette.muted,
                                    modifier = Modifier.size(17.dp),
                                )
                            }
                            if (index != recent.lastIndex) {
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(palette.line.copy(alpha = 0.55f)),
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FieldPulseDot(size = 5.dp)
                Spacer(Modifier.width(7.dp))
                FieldMicroLabel("END // INVENTREE MOBILE", color = palette.muted.copy(alpha = 0.7f))
                Spacer(Modifier.weight(1f))
                FieldMicroLabel("API 530+", color = palette.muted.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    code: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalFieldPalette.current
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = FieldCut,
        color = palette.panel,
        border = BorderStroke(1.dp, palette.line),
    ) {
        Box {
            Canvas(Modifier.matchParentSize()) {
                drawLine(
                    color = accent.copy(alpha = 0.85f),
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.46f, 0f),
                    strokeWidth = 2.dp.toPx(),
                )
            }
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FieldMicroLabel(code, color = accent, maxLines = 1)
                    Spacer(Modifier.weight(1f))
                    Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.height(12.dp))
                AnimatedContent(
                    targetState = value,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn()) togetherWith
                            (slideOutVertically { -it } + fadeOut())
                    },
                    label = "stat-$code",
                ) { animatedValue ->
                    Text(
                        animatedValue,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = palette.ink,
                    )
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.muted,
                )
            }
        }
    }
}

@Composable
private fun QuickAction(
    label: String,
    code: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val palette = LocalFieldPalette.current
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            color = palette.panel,
            shape = FieldCut,
            border = BorderStroke(1.dp, palette.line),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp),
            ) {
                FieldMicroLabel(
                    code,
                    modifier = Modifier.padding(start = 7.dp, top = 6.dp),
                    color = palette.signalText.copy(alpha = 0.65f),
                )
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(FieldCutSmall)
                        .background(palette.signal.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = label, tint = palette.signalText, modifier = Modifier.size(19.dp))
                }
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.42f)
                        .height(2.dp)
                        .background(palette.signal.copy(alpha = 0.55f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = palette.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
