package xyz.bi7khi.inventree.mobile.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.data.label.BlePrinterManager
import xyz.bi7khi.inventree.mobile.data.label.BlePrinterState
import xyz.bi7khi.inventree.mobile.data.label.LabelDesign
import xyz.bi7khi.inventree.mobile.data.label.LabelDesignStore
import xyz.bi7khi.inventree.mobile.data.label.LabelDocument
import xyz.bi7khi.inventree.mobile.data.label.LabelPrintSettings
import xyz.bi7khi.inventree.mobile.data.label.LabelRenderer
import xyz.bi7khi.inventree.mobile.data.label.LabelTemplateType
import xyz.bi7khi.inventree.mobile.data.label.loadLabelDocument
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPrintScreen(
    repository: InvenTreeRepository,
    typeKey: String,
    itemId: Int?,
    onBack: () -> Unit,
    onEditDesign: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val type = remember(typeKey) { LabelTemplateType.fromKey(typeKey) }
    val store = remember { LabelDesignStore(context) }
    val manager = remember { BlePrinterManager(context) }
    val bleState by manager.state.collectAsStateWithLifecycle()
    val devices by manager.devices.collectAsStateWithLifecycle()
    var document by remember { mutableStateOf<LabelDocument?>(null) }
    var design by remember { mutableStateOf<LabelDesign?>(null) }
    var settings by remember { mutableStateOf(LabelPrintSettings()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    var showScanner by remember { mutableStateOf(false) }
    var printing by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    DisposableEffect(Unit) {
        onDispose { manager.close() }
    }

    LaunchedEffect(type, itemId, reloadTick) {
        loading = true
        error = null
        runCatching {
            val doc = repository.loadLabelDocument(type, itemId)
            val d = store.load(type)
            val s = store.loadSettings()
            Triple(doc, d, s)
        }.onSuccess { (doc, d, s) ->
            document = doc
            design = d
            settings = s
        }.onFailure { error = it.toUserMessage() }
        loading = false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        // Returning from the design editor should refresh the preview immediately.
        reloadTick++
    }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        if (result.values.all { it } || manager.hasPermissions()) {
            manager.startScan()
            showScanner = true
        }
    }

    fun beginScan() {
        val granted = permissions.all {
            androidx.core.content.ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }
        if (granted) {
            manager.startScan()
            showScanner = true
        } else {
            permissionLauncher.launch(permissions)
        }
    }

    if (loading && design == null) {
        FullScreenLoading("正在生成标签…")
        return
    }
    if (design == null || document == null) {
        ErrorState(error ?: "无法加载标签", modifier = Modifier.fillMaxSize(), onRetry = { reloadTick++ })
        return
    }
    val currentDesign = design!!
    val doc = document!!
    val bitmap: Bitmap = remember(currentDesign, doc, settings.dpi) {
        LabelRenderer.render(currentDesign, doc.values, settings.dpi)
    }
    val connected = bleState is BlePrinterState.Connected

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("打印标签")
                        Text(
                            doc.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onEditDesign) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑标签")
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
                AppCard {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(currentDesign.widthMm / currentDesign.heightMm)
                            .background(Color.White, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "标签预览",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                doc.subtitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${currentDesign.widthMm.toInt()} × ${currentDesign.heightMm.toInt()} mm · ${settings.dpi} dpi · ${currentDesign.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = onEditDesign) { Text("编辑版式") }
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader("打印机")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (connected) Icons.Outlined.BluetoothConnected else Icons.Outlined.Bluetooth,
                            contentDescription = null,
                            tint = if (connected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (val s = bleState) {
                                    is BlePrinterState.Connected -> s.name
                                    is BlePrinterState.Connecting -> "正在连接 ${s.name}…"
                                    is BlePrinterState.Discovering -> "正在发现服务…"
                                    is BlePrinterState.Printing -> "正在打印 ${(s.progress * 100).toInt()}%"
                                    is BlePrinterState.Error -> "连接异常"
                                    else -> "未连接打印机"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            if (bleState is BlePrinterState.Connected) {
                                Text(
                                    (bleState as BlePrinterState.Connected).address,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (connected) {
                            OutlinedButton(onClick = { manager.disconnect() }) { Text("断开") }
                        } else {
                            Button(onClick = { beginScan() }) { Text("搜索") }
                        }
                    }
                    if (bleState is BlePrinterState.Printing) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (bleState as BlePrinterState.Printing).progress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    (bleState as? BlePrinterState.Error)?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                    if (connected) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching { manager.testFeed() }
                                            .onFailure { snackbar.showSnackbar(it.toUserMessage()) }
                                    }
                                },
                            ) { Text("测试走纸") }
                            OutlinedButton(onClick = onEditDesign) { Text("编辑标签") }
                        }
                    }
                }
            }

            item {
                AppCard {
                    SectionHeader("打印参数")
                    Spacer(Modifier.height(8.dp))
                    Text("协议", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelPrintSettings().protocolOptions.forEach { protocol ->
                            FilterChip(
                                selected = settings.protocol == protocol,
                                onClick = { settings = settings.copy(protocol = protocol) },
                                label = { Text(protocol) },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("分辨率", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LabelPrintSettings().dpiOptions.forEach { dpi ->
                            FilterChip(
                                selected = settings.dpi == dpi,
                                onClick = { settings = settings.copy(dpi = dpi) },
                                label = { Text("$dpi dpi") },
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = settings.copies.toString(),
                            onValueChange = {
                                settings = settings.copy(copies = it.toIntOrNull()?.coerceIn(1, 9) ?: 1)
                            },
                            label = { Text("份数") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        OutlinedTextField(
                            value = if (settings.feedMm == 0f) "自动" else settings.feedMm.toString(),
                            onValueChange = {
                                settings = settings.copy(feedMm = it.replace(",", ".").toFloatOrNull() ?: 0f)
                            },
                            label = { Text("走纸 mm（0=间隙）") },
                            modifier = Modifier.weight(1.4f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "二值化阈值 ${settings.threshold}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = settings.threshold.toFloat(),
                        onValueChange = { settings = settings.copy(threshold = it.toInt()) },
                        valueRange = 40f..220f,
                        steps = 179,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("反色打印", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = settings.invert,
                            onCheckedChange = { settings = settings.copy(invert = it) },
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = settings.writeChunkSize.toString(),
                            onValueChange = {
                                settings = settings.copy(writeChunkSize = it.toIntOrNull()?.coerceIn(16, 512) ?: 180)
                            },
                            label = { Text("BLE 块大小") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                        OutlinedTextField(
                            value = settings.writeChunkDelayMs.toString(),
                            onValueChange = {
                                settings = settings.copy(writeChunkDelayMs = it.toIntOrNull()?.coerceIn(0, 500) ?: 15)
                            },
                            label = { Text("块间延迟 ms") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    store.saveSettings(settings)
                                    snackbar.showSnackbar("打印参数已保存")
                                }
                            },
                        ) { Text("保存参数") }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        if (!connected || printing) return@Button
                        scope.launch {
                            printing = true
                            runCatching {
                                manager.print(bitmap, settings, currentDesign.widthMm, currentDesign.heightMm)
                            }.onSuccess {
                                snackbar.showSnackbar("标签已发送到打印机")
                            }.onFailure {
                                snackbar.showSnackbar(it.toUserMessage())
                            }
                            printing = false
                        }
                    },
                    enabled = connected && !printing,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (printing) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Outlined.Print, contentDescription = null, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (connected) "打印标签" else "请先连接打印机")
                    }
                }
            }
            item {
                Text(
                    "提示：标签数据来自 InvenTree 服务器；二维码内容为服务器生成的条码。BLE 打印机需支持 ESC/POS 或 TSPL2。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (showScanner) {
        ModalBottomSheet(
            onDismissRequest = {
                showScanner = false
                manager.stopScan()
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("选择 BLE 打印机", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "发现 ${devices.size} 台设备",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { manager.startScan() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "重新扫描")
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (devices.isEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("正在扫描附近的 BLE 设备…", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    devices.forEach { device ->
                        Surface(
                            onClick = {
                                manager.connect(device.address)
                                showScanner = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Row(
                                Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Outlined.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(device.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        device.address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Text("${device.rssi} dBm", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}
