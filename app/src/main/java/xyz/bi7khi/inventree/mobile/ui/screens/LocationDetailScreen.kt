package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.ConfirmDialog
import xyz.bi7khi.inventree.mobile.ui.components.EmptyState
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.InfoRow
import xyz.bi7khi.inventree.mobile.ui.components.QuantityPill
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationDetailScreen(
    repository: InvenTreeRepository,
    locationId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNewChild: () -> Unit,
    onOpenLocation: (Int) -> Unit,
    onOpenStock: (Int) -> Unit,
    onPrint: () -> Unit,
    onDeleted: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var location by remember { mutableStateOf<LocationDto?>(null) }
    var children by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var stock by remember { mutableStateOf<List<StockItemDto>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        loading = true
        error = null
        runCatching {
            val loc = repository.location(locationId)
            val all = repository.allLocations()
            val items = repository.stockItems(locationId = locationId, inStock = null, limit = 100).results
            Triple(loc, all.filter { it.parent == locationId }.sortedBy { it.name }, items)
        }.onSuccess { (loc, kids, items) ->
            location = loc
            children = kids
            stock = items
        }.onFailure { error = it.toUserMessage() }
        loading = false
    }

    LaunchedEffect(locationId) { load() }

    if (loading && location == null) {
        FullScreenLoading("正在加载库位…")
        return
    }
    if (location == null) {
        ErrorState(error ?: "库位不存在", modifier = Modifier.fillMaxSize(), onRetry = { scope.launch { load() } })
        return
    }
    val loc = location!!

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("库位详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = onPrint) { Icon(Icons.Outlined.Print, contentDescription = "打印标签") }
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewChild) {
                Icon(Icons.Outlined.Add, contentDescription = "新建子库位")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(62.dp)
                                .background(
                                    if (loc.structural) MaterialTheme.colorScheme.tertiaryContainer
                                    else MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(18.dp),
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (loc.structural) Icons.Outlined.Folder else Icons.Outlined.Place,
                                contentDescription = null,
                                tint = if (loc.structural) MaterialTheme.colorScheme.tertiary
                                else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(30.dp),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(loc.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                loc.pathstring ?: loc.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (loc.description.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(loc.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("库存项", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${loc.items}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("子库位", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${loc.sublocations}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader("库位属性")
                    InfoRow("层级", "第 ${loc.level + 1} 层")
                    InfoRow("结构库位", if (loc.structural) "是（只用于分组）" else "否")
                    InfoRow("外部库位", if (loc.external) "是" else "否")
                    InfoRow("库位 ID", "#${loc.pk}")
                }
            }

            if (children.isNotEmpty()) {
                item { SectionHeader("子库位") }
                items(children, key = { it.pk }) { child ->
                    Surface(
                        onClick = { onOpenLocation(child.pk) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 0.5.dp,
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(child.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("${child.items} 个库存项", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { SectionHeader("库位中的库存", actionLabel = if (stock.isNotEmpty()) "共 ${stock.size} 项" else null) }
            if (stock.isEmpty()) {
                item {
                    AppCard {
                        EmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = "这个库位暂无库存",
                            description = "可以从库存详情中转移物料到这里。",
                        )
                    }
                }
            } else {
                items(stock, key = { it.pk }) { item ->
                    AppCard(onClick = { onOpenStock(item.pk) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.partName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOfNotNull(
                                        item.part_detail?.IPN?.takeIf { it.isNotBlank() },
                                        item.batch?.takeIf { it.isNotBlank() }?.let { "批次 $it" },
                                    ).joinToString(" · ").ifBlank { "库存 #${item.pk}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            QuantityPill(item.quantity, item.part_detail?.units, compact = true)
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除库位？",
            message = "将删除「${loc.pathstring ?: loc.name}」。如果库位中仍有库存或子库位，服务器会拒绝该操作。",
            confirmLabel = if (deleting) "删除中…" else "删除",
            destructive = true,
            onConfirm = {
                if (!deleting) {
                    scope.launch {
                        deleting = true
                        runCatching { repository.deleteLocation(loc.pk) }
                            .onSuccess { showDelete = false; onDeleted() }
                            .onFailure { deleteError = it.toUserMessage() }
                        deleting = false
                    }
                }
            },
            onDismiss = { if (!deleting) showDelete = false },
        )
    }
    if (deleteError != null) {
        AlertDialog(
            onDismissRequest = { deleteError = null },
            title = { Text("无法删除") },
            text = { Text(deleteError!!) },
            confirmButton = { TextButton(onClick = { deleteError = null }) { Text("知道了") } },
        )
    }
}
