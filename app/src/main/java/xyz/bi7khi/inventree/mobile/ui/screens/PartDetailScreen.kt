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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.AuthenticatedPartImage
import xyz.bi7khi.inventree.mobile.ui.components.ConfirmDialog
import xyz.bi7khi.inventree.mobile.ui.components.EmptyState
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.FieldTopBar
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.InfoRow
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartDetailScreen(
    repository: InvenTreeRepository,
    partId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onAddStock: () -> Unit,
    onOpenStock: (Int) -> Unit,
    onPrint: () -> Unit,
    onDeleted: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var part by remember { mutableStateOf<PartDto?>(null) }
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
            val p = repository.part(partId)
            val s = repository.stockForPart(partId)
            p to s
        }.onSuccess { (p, s) ->
            part = p
            stock = s
        }.onFailure { error = it.toUserMessage() }
        loading = false
    }

    LaunchedEffect(partId) { load() }

    if (loading && part == null) {
        FullScreenLoading("正在加载零件…")
        return
    }
    if (part == null) {
        ErrorState(error ?: "零件不存在", modifier = Modifier.fillMaxSize(), onRetry = { scope.launch { load() } })
        return
    }
    val p = part!!
    val imageUrls = repository.partImageUrls(p.thumbnail, p.image)
    val isLow = (p.minimum_stock ?: 0.0) > 0 && (p.total_in_stock ?: 0.0) < (p.minimum_stock ?: 0.0)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldTopBar(
                title = "零件详情",
                subtitle = p.IPN?.takeIf { it.isNotBlank() } ?: p.name,
                code = "PART // RECORD",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onPrint) { Icon(Icons.Outlined.Print, contentDescription = "打印标签") }
                    IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AuthenticatedPartImage(
                                urls = imageUrls,
                                token = repository.token,
                                modifier = Modifier.fillMaxSize(),
                                fallback = {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(38.dp),
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                p.IPN?.takeIf { it.isNotBlank() } ?: "无 IPN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (!p.category_name.isNullOrBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    p.category_path ?: p.category_name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (p.description.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Text(p.description, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("总库存", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    formatQty(p.total_in_stock ?: 0.0) + (p.units?.takeIf { it.isNotBlank() }?.let { " $it" } ?: ""),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Surface(
                            color = if (isLow) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("最小库存", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (isLow) {
                                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        formatQty(p.minimum_stock ?: 0.0),
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isLow) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onAddStock,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("为这个零件添加库存")
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader("零件属性")
                    Spacer(Modifier.height(4.dp))
                    InfoRow("IPN", p.IPN)
                    InfoRow("分类", p.category_path ?: p.category_name)
                    InfoRow("单位", p.units)
                    InfoRow("默认库位", p.default_location_detail?.pathstring ?: p.default_location_detail?.name)
                    InfoRow("关键词", p.keywords)
                    InfoRow("外部链接", p.link)
                    val flags = buildList {
                        if (p.active) add("启用")
                        if (p.component) add("组件")
                        if (p.assembly) add("装配件")
                        if (p.purchaseable) add("可采购")
                        if (p.salable) add("可销售")
                        if (p.trackable) add("可追踪")
                        if (p.virtual) add("虚拟")
                        if (p.consumable) add("消耗品")
                    }
                    if (flags.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            flags.take(6).forEach { flag ->
                                Surface(
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Text(
                                        flag,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (!p.notes.isNullOrBlank()) {
                item {
                    AppCard {
                        SectionHeader("备注")
                        Spacer(Modifier.height(6.dp))
                        Text(p.notes!!, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            item {
                SectionHeader("库存分布", actionLabel = "添加", onAction = onAddStock)
            }
            if (stock.isEmpty()) {
                item {
                    AppCard {
                        EmptyState(
                            icon = Icons.Outlined.Inventory2,
                            title = "暂无库存",
                            description = "这个零件还没有任何库存项。",
                            actionLabel = "添加库存",
                            onAction = onAddStock,
                        )
                    }
                }
            } else {
                items(stock, key = { it.pk }) { item ->
                    AppCard(onClick = { onOpenStock(item.pk) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.locationName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    listOfNotNull(
                                        item.batch?.takeIf { it.isNotBlank() }?.let { "批次 $it" },
                                        item.serial?.takeIf { it.isNotBlank() }?.let { "S/N $it" },
                                    ).joinToString(" · ").ifBlank { "库存 #${item.pk}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                formatQty(item.quantity),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除零件？",
            message = "将删除「${p.name}」。如果仍有库存、BOM 或订单引用，服务器会拒绝该操作。",
            confirmLabel = if (deleting) "删除中…" else "删除",
            destructive = true,
            onConfirm = {
                if (!deleting) {
                    scope.launch {
                        deleting = true
                        runCatching { repository.deletePart(p.pk) }
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
