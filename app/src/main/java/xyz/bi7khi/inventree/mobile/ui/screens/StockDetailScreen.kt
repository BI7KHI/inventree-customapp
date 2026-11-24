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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.StockActionType
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.AuthenticatedPartImage
import xyz.bi7khi.inventree.mobile.ui.components.ConfirmDialog
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.FieldTopBar
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.InfoRow
import xyz.bi7khi.inventree.mobile.ui.components.InvenTreeTopBar
import xyz.bi7khi.inventree.mobile.ui.components.LocationPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.QrCodeDialog
import xyz.bi7khi.inventree.mobile.ui.components.QuantityPill
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.StatusBadge
import xyz.bi7khi.inventree.mobile.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockDetailScreen(
    repository: InvenTreeRepository,
    stockId: Int,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenPart: (Int) -> Unit,
    onOpenLocation: (Int) -> Unit,
    onPrint: () -> Unit,
    onDeleted: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var item by remember { mutableStateOf<StockItemDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var action by remember { mutableStateOf<StockActionType?>(null) }
    var showQr by remember { mutableStateOf(false) }
    var qrText by remember { mutableStateOf("") }
    var showDelete by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var deleteError by remember { mutableStateOf<String?>(null) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    suspend fun load(showLoader: Boolean = true) {
        if (showLoader) loading = true
        runCatching { repository.stockItem(stockId) }
            .onSuccess {
                item = it
                error = null
            }
            .onFailure { error = it.toUserMessage() }
        loading = false
    }

    LaunchedEffect(stockId) {
        load()
        runCatching { repository.allLocations() }.onSuccess { locations = it }
    }

    if (loading && item == null) {
        FullScreenLoading("正在加载库存详情…")
        return
    }
    if (item == null) {
        ErrorState(error ?: "库存项不存在", modifier = Modifier.fillMaxSize(), onRetry = { scope.launch { load() } })
        return
    }
    val current = item!!

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            FieldTopBar(
                title = "库存详情",
                subtitle = current.partName,
                code = "STOCK // RECORD",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onPrint) {
                        Icon(Icons.Outlined.Print, contentDescription = "打印标签")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            runCatching { repository.generateBarcode("stockitem", stockId) }
                                .onSuccess {
                                    qrText = it
                                    showQr = true
                                }
                                .onFailure { snackbar.showSnackbar(it.toUserMessage()) }
                        }
                    }) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = "二维码")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = "删除")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            AuthenticatedPartImage(
                                urls = repository.partImageUrls(
                                    current.part_detail?.thumbnail,
                                    current.part_detail?.image,
                                ),
                                token = repository.token,
                                modifier = Modifier.fillMaxSize(),
                                fallback = {
                                    Icon(
                                        Icons.Outlined.Inventory2,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(34.dp),
                                    )
                                },
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                current.partName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                current.part_detail?.IPN?.takeIf { it.isNotBlank() } ?: "库存 #${current.pk}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            StatusBadge(current.status, current.status_text)
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "当前数量",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    formatQty(current.quantity),
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                current.part_detail?.units?.takeIf { it.isNotBlank() }?.let {
                                    Text(
                                        " $it",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 6.dp),
                                    )
                                }
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "已分配 ${formatQty(current.allocated ?: 0.0)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "可用 ${formatQty(current.quantity - (current.allocated ?: 0.0))}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        onClick = { current.location?.let(onOpenLocation) },
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.width(5.dp))
                            Text(
                                current.locationName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StockActionButton("入库", Icons.Outlined.Add, Modifier.weight(1f)) { action = StockActionType.ADD }
                    StockActionButton("出库", Icons.Outlined.Remove, Modifier.weight(1f)) { action = StockActionType.REMOVE }
                    StockActionButton("转移", Icons.Outlined.SwapHoriz, Modifier.weight(1f)) { action = StockActionType.TRANSFER }
                    StockActionButton("盘点", Icons.Outlined.Tune, Modifier.weight(1f)) { action = StockActionType.COUNT }
                }
            }

            item {
                AppCard {
                    SectionHeader("库存信息")
                    Spacer(Modifier.height(4.dp))
                    InfoRow("库存 ID", "#${current.pk}")
                    InfoRow("批次", current.batch)
                    InfoRow("序列号", current.serial)
                    InfoRow("包装", current.packaging)
                    InfoRow("到期日期", current.expiry_date)
                    InfoRow("创建时间", current.creation_date)
                    InfoRow("更新时间", current.updated)
                    InfoRow("备注", current.notes)
                    if (current.installed_items != null && current.installed_items!! > 0) {
                        InfoRow("已安装子项", current.installed_items.toString())
                    }
                }
            }

            item {
                AppCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("零件档案", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                current.part_detail?.description?.takeIf { it.isNotBlank() } ?: "查看零件详情与全部库存",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { onOpenPart(current.part) }) { Text("打开") }
                    }
                }
            }
        }
    }

    action?.let { type ->
        StockAdjustDialog(
            item = current,
            action = type,
            locations = locations,
            onDismiss = { action = null },
            onSubmit = { qty, notes, destination ->
                repository.adjustStock(type, current, qty, notes, destination)
                load(showLoader = false)
                snackbar.showSnackbar("${type.title}成功")
            },
        )
    }

    if (showQr) {
        QrCodeDialog(title = "库存二维码", content = qrText, onDismiss = { showQr = false })
    }

    if (showDelete) {
        ConfirmDialog(
            title = "删除库存项？",
            message = "将永久删除 #${current.pk}「${current.partName}」。已有出入库记录或关联订单时，服务器可能拒绝此操作。",
            confirmLabel = if (deleting) "删除中…" else "删除",
            destructive = true,
            onConfirm = {
                if (!deleting) {
                    scope.launch {
                        deleting = true
                        runCatching { repository.deleteStock(current.pk) }
                            .onSuccess { showDelete = false; onDeleted() }
                            .onFailure { deleteError = it.toUserMessage() }
                        deleting = false
                    }
                }
            },
            onDismiss = { if (!deleting) { showDelete = false; deleteError = null } },
        )
        if (deleteError != null) {
            AlertDialog(
                onDismissRequest = { deleteError = null },
                title = { Text("无法删除") },
                text = { Text(deleteError!!) },
                confirmButton = { TextButton(onClick = { deleteError = null }) { Text("知道了") } },
            )
        }
    }
}

@Composable
private fun StockActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(23.dp))
            Spacer(Modifier.height(5.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun StockAdjustDialog(
    item: StockItemDto,
    action: StockActionType,
    locations: List<LocationDto>,
    onDismiss: () -> Unit,
    onSubmit: suspend (Double, String, LocationDto?) -> Unit,
) {
    var quantity by remember(action) {
        mutableStateOf(if (action == StockActionType.COUNT) formatQty(item.quantity) else "1")
    }
    var notes by remember(action) { mutableStateOf("") }
    var destination by remember(action) { mutableStateOf<LocationDto?>(null) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val parsed = quantity.replace(',', '.').toDoubleOrNull()

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text(action.title) },
        text = {
            Column {
                Text(
                    item.partName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "当前 ${formatQty(item.quantity)} · ${item.locationName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (action == StockActionType.COUNT) "实盘数量" else "数量") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    enabled = !busy,
                    isError = quantity.isNotBlank() && (parsed == null || parsed < 0),
                    shape = RoundedCornerShape(14.dp),
                )
                if (action == StockActionType.TRANSFER) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                destination?.pathstring ?: "选择目标库位",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (destination == null) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("备注（可选）") },
                    maxLines = 3,
                    enabled = !busy,
                    shape = RoundedCornerShape(14.dp),
                )
                if (error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (busy) return@Button
                    val qty = parsed
                    if (qty == null || qty < 0 || (action != StockActionType.COUNT && qty <= 0)) {
                        error = "请输入有效的数量"
                        return@Button
                    }
                    if (action == StockActionType.TRANSFER && destination == null) {
                        error = "请选择目标库位"
                        return@Button
                    }
                    scope.launch {
                        busy = true
                        error = null
                        runCatching { onSubmit(qty, notes, destination) }
                            .onSuccess { onDismiss() }
                            .onFailure { error = it.toUserMessage() }
                        busy = false
                    }
                },
                enabled = !busy,
            ) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("确认${action.title}")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )

    if (showLocationPicker) {
        LocationPickerDialog(
            locations = locations.filter { it.pk != item.location },
            selectedId = destination?.pk,
            title = "目标库位",
            onSelect = { destination = it },
            onDismiss = { showLocationPicker = false },
        )
    }
}
