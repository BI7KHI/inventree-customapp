package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.StockPatchRequest
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader
import xyz.bi7khi.inventree.mobile.ui.components.StatusPickerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditScreen(
    repository: InvenTreeRepository,
    stockId: Int,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var item by remember { mutableStateOf<StockItemDto?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var batch by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var status by remember { mutableStateOf<Int?>(10) }
    var showStatus by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(stockId, reloadTick) {
        runCatching { repository.stockItem(stockId) }
            .onSuccess {
                item = it
                batch = it.batch.orEmpty()
                serial = it.serial.orEmpty()
                packaging = it.packaging.orEmpty()
                notes = it.notes.orEmpty()
                link = it.link.orEmpty()
                expiry = it.expiry_date.orEmpty()
                status = it.status
                loadError = null
            }
            .onFailure { loadError = it.toUserMessage() }
        loading = false
    }

    if (loading && item == null) {
        FullScreenLoading("正在加载…")
        return
    }
    if (item == null) {
        ErrorState(
            loadError ?: "库存项不存在",
            modifier = Modifier.fillMaxSize(),
            onRetry = { loading = true; reloadTick++ },
        )
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("编辑库存")
                        Text(
                            item!!.partName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                saving = true
                                saveError = null
                                runCatching {
                                    repository.updateStock(
                                        stockId,
                                        StockPatchRequest(
                                            batch = batch.trim(),
                                            serial = serial.trim().ifBlank { null },
                                            packaging = packaging.trim().ifBlank { null },
                                            notes = notes.trim().ifBlank { null },
                                            link = link.trim().ifBlank { null },
                                            expiry_date = expiry.trim().ifBlank { null },
                                            status = status,
                                        ),
                                    )
                                }.onSuccess { onSaved() }
                                    .onFailure { saveError = it.toUserMessage() }
                                saving = false
                            }
                        },
                        enabled = !saving,
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text("当前库位", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item!!.locationName, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(
                            "移动库位请用「转移」",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item {
                AppCard {
                    SectionHeader("标识信息")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("批次号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = serial,
                        onValueChange = { serial = it },
                        label = { Text("序列号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = { packaging = it },
                        label = { Text("包装") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item {
                AppCard {
                    SectionHeader("状态与日期")
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showStatus = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text("库存状态", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (status) {
                                    10 -> "正常 (OK)"
                                    50 -> "需关注"
                                    55 -> "损坏"
                                    60 -> "已销毁"
                                    65 -> "已拒收"
                                    70 -> "丢失"
                                    75 -> "隔离"
                                    85 -> "已退货"
                                    else -> "未设置"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = expiry,
                        onValueChange = { expiry = it },
                        label = { Text("到期日期") },
                        placeholder = { Text("YYYY-MM-DD") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        label = { Text("外部链接") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item {
                AppCard {
                    SectionHeader("备注")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            if (saveError != null) {
                item {
                    Text(
                        saveError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            saveError = null
                            runCatching {
                                repository.updateStock(
                                    stockId,
                                    StockPatchRequest(
                                        batch = batch.trim(),
                                        serial = serial.trim().ifBlank { null },
                                        packaging = packaging.trim().ifBlank { null },
                                        notes = notes.trim().ifBlank { null },
                                        link = link.trim().ifBlank { null },
                                        expiry_date = expiry.trim().ifBlank { null },
                                        status = status,
                                    ),
                                )
                            }.onSuccess { onSaved() }
                                .onFailure { saveError = it.toUserMessage() }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Text(if (saving) "保存中…" else "保存更改", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showStatus) {
        StatusPickerDialog(
            selected = status,
            onSelect = { status = it },
            onDismiss = { showStatus = false },
        )
    }
}
