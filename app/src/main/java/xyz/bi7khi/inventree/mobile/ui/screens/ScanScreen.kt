package xyz.bi7khi.inventree.mobile.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.concurrent.Executors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.BarcodeOutcome
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.BrandMark
import xyz.bi7khi.inventree.mobile.ui.components.FieldMicroLabel
import xyz.bi7khi.inventree.mobile.ui.components.FieldPulseDot
import xyz.bi7khi.inventree.mobile.ui.scan.BarcodeAnalyzer
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCut
import xyz.bi7khi.inventree.mobile.ui.theme.FieldCutSmall
import xyz.bi7khi.inventree.mobile.ui.theme.LocalFieldPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    repository: InvenTreeRepository,
    onBack: () -> Unit,
    onOpenStock: (Int) -> Unit,
    onOpenPart: (Int) -> Unit,
    onOpenLocation: (Int) -> Unit,
    onSearch: (String) -> Unit,
) {
    val palette = LocalFieldPalette.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var pendingBarcode by remember { mutableStateOf<String?>(null) }
    val analyzer = remember {
        BarcodeAnalyzer { value ->
            mainHandler.post {
                if (pendingBarcode == null) pendingBarcode = value
            }
        }
    }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            setBackgroundColor(android.graphics.Color.BLACK)
        }
    }
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torch by remember { mutableStateOf(false) }
    var outcome by remember { mutableStateOf<BarcodeOutcome?>(null) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var processing by remember { mutableStateOf(false) }
    var showSheet by remember { mutableStateOf(false) }
    var showManual by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted = it }

    StatusBarIcons(lightIcons = !showSheet)

    LaunchedEffect(Unit) {
        if (!granted) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { cameraProvider?.unbindAll() }
            analyzer.close()
            executor.shutdown()
        }
    }

    LaunchedEffect(granted) {
        if (!granted) return@LaunchedEffect
        cameraError = null
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            runCatching {
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { it.setAnalyzer(executor, analyzer) }
                provider.unbindAll()
                camera = provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis,
                )
            }.onFailure { cameraError = it.message ?: "无法启动相机" }
        }, ContextCompat.getMainExecutor(context))
    }

    LaunchedEffect(torch) {
        runCatching { camera?.cameraControl?.enableTorch(torch) }
    }

    LaunchedEffect(pendingBarcode) {
        val raw = pendingBarcode ?: return@LaunchedEffect
        analyzer.pause()
        processing = true
        scanError = null
        outcome = null
        val result = runCatching { repository.scanBarcode(raw) }
        result.onSuccess { outcome = it }
            .onFailure { scanError = it.toUserMessage() }
        processing = false
        showSheet = true
    }

    fun resumeScanning() {
        showSheet = false
        outcome = null
        scanError = null
        pendingBarcode = null
        scope.launch {
            delay(250)
            analyzer.resume()
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (granted) {
            AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (granted) 0.18f else 0.92f)),
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    onClick = onBack,
                    shape = FieldCutSmall,
                    color = Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Color.White)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FieldPulseDot(color = palette.signal, size = 5.dp)
                        Spacer(Modifier.width(6.dp))
                        FieldMicroLabel("OPTICAL SCAN // 04", color = palette.signal)
                    }
                    Text("扫描条码", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "支持 QR、Data Matrix、Code 128 等",
                        color = Color.White.copy(alpha = 0.65f),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Surface(
                    onClick = {
                        torch = !torch
                    },
                    shape = FieldCutSmall,
                    color = if (torch) palette.signal else Color.Black.copy(alpha = 0.35f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                ) {
                    Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            if (torch) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                            contentDescription = "手电筒",
                            tint = if (torch) palette.onSignal else Color.White,
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            if (granted) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(270.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    ScanFrame()
                }
                Spacer(Modifier.height(18.dp))
                FieldMicroLabel(
                    if (processing) "DECODING // PLEASE WAIT" else "ALIGN BARCODE // AUTO DETECT",
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = palette.signal,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (processing) "正在解析条码…" else "将条码放入框内，自动识别",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (processing) {
                    Spacer(Modifier.height(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(22.dp),
                        strokeWidth = 2.dp,
                        color = palette.signal,
                    )
                }
            } else {
                PermissionCard(
                    denied = cameraError != null,
                    onRequest = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenSettings = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:${context.packageName}"),
                            ),
                        )
                    },
                )
            }
            Spacer(Modifier.weight(1f))
            if (cameraError != null && granted) {
                Text(
                    cameraError!!,
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                FilledTonalButton(
                    onClick = { showManual = true },
                    shape = FieldCutSmall,
                ) {
                    Icon(Icons.Outlined.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("手动输入条码")
                }
            }
        }
    }

    if (showManual) {
        var code by remember { mutableStateOf("") }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showManual = false },
            title = { Text("手动输入条码") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("条码内容") },
                    singleLine = true,
                    shape = FieldCutSmall,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (code.isNotBlank()) {
                            showManual = false
                            pendingBarcode = code.trim()
                        }
                    },
                ) { Text("识别") }
            },
            dismissButton = { TextButton(onClick = { showManual = false }) { Text("取消") } },
        )
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { resumeScanning() },
            sheetState = sheetState,
            containerColor = palette.panel,
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(bottom = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FieldMicroLabel("SCAN RESULT // DECODED", color = palette.signalText)
                Spacer(Modifier.height(10.dp))
                when {
                    processing -> {
                        CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.height(14.dp))
                        Text("正在向服务器解析条码…", style = MaterialTheme.typography.bodyMedium)
                    }
                    scanError != null -> {
                        ResultIcon(Icons.Outlined.WarningAmber, error = true)
                        Text("条码解析失败", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        Text(scanError!!, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(18.dp))
                        Button(onClick = { resumeScanning() }, modifier = Modifier.fillMaxWidth()) { Text("重新扫描") }
                    }
                    else -> {
                        val result = outcome
                        when (result) {
                            is BarcodeOutcome.StockItem -> {
                                ResultIcon(Icons.Outlined.Inventory2)
                                Text("已匹配库存项", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("库存 #${result.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        resumeScanning()
                                        onOpenStock(result.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("查看库存详情") }
                            }
                            is BarcodeOutcome.Part -> {
                                ResultIcon(Icons.Outlined.Inventory2)
                                Text("已匹配零件", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("零件 #${result.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        resumeScanning()
                                        onOpenPart(result.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("查看零件") }
                            }
                            is BarcodeOutcome.Location -> {
                                ResultIcon(Icons.Outlined.Place)
                                Text("已匹配库位", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("库位 #${result.id}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(18.dp))
                                Button(
                                    onClick = {
                                        resumeScanning()
                                        onOpenLocation(result.id)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) { Text("查看库位") }
                            }
                            is BarcodeOutcome.NotFound -> {
                                ResultIcon(Icons.Outlined.QrCodeScanner)
                                Text("未识别的条码", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(6.dp))
                                Text(result.message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = palette.panelAlt,
                                    shape = FieldCutSmall,
                                ) {
                                    Text(
                                        result.barcode,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Spacer(Modifier.height(18.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    FilledTonalButton(
                                        onClick = { resumeScanning() },
                                        modifier = Modifier.weight(1f),
                                    ) { Text("重新扫描") }
                                    Button(
                                        onClick = {
                                            resumeScanning()
                                            onSearch(result.barcode)
                                        },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp))
                                        Text("搜索")
                                    }
                                }
                            }
                            null -> {
                                ResultIcon(Icons.Outlined.QrCodeScanner)
                                Text("没有识别结果", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                if (processing) return@Column
                if (scanError == null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { resumeScanning() }) { Text("继续扫描") }
                }
            }
        }
    }
}

@Composable
private fun ScanFrame() {
    val palette = LocalFieldPalette.current
    val transition = rememberInfiniteTransition(label = "scan-line")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-progress",
    )
    Canvas(Modifier.fillMaxSize()) {
        val frameWidth = size.width * 0.80f
        val frameHeight = frameWidth
        val left = (size.width - frameWidth) / 2f
        val top = (size.height - frameHeight) / 2f
        val right = left + frameWidth
        val bottom = top + frameHeight
        val corner = 27.dp.toPx()
        val stroke = 2.2.dp.toPx()

        // Reticle grid.
        val grid = frameWidth / 8f
        for (i in 1 until 8) {
            drawLine(
                color = Color.White.copy(alpha = 0.055f),
                start = Offset(left + grid * i, top),
                end = Offset(left + grid * i, bottom),
                strokeWidth = 1f,
            )
            drawLine(
                color = Color.White.copy(alpha = 0.055f),
                start = Offset(left, top + grid * i),
                end = Offset(right, top + grid * i),
                strokeWidth = 1f,
            )
        }

        // Frame corners: cyan outer, signal inner.
        val c = palette.cyan
        val s = palette.signal
        listOf(
            Triple(Offset(left, top), Offset(left + corner, top), Offset(left, top + corner)),
            Triple(Offset(right, top), Offset(right - corner, top), Offset(right, top + corner)),
            Triple(Offset(left, bottom), Offset(left + corner, bottom), Offset(left, bottom - corner)),
            Triple(Offset(right, bottom), Offset(right - corner, bottom), Offset(right, bottom - corner)),
        ).forEach { (origin, horizontal, vertical) ->
            drawLine(c, origin, horizontal, stroke, StrokeCap.Square)
            drawLine(c, origin, vertical, stroke, StrokeCap.Square)
        }
        // Small inner brackets.
        val tick = 9.dp.toPx()
        drawLine(s, Offset(left + corner + 6.dp.toPx(), top), Offset(left + corner + 6.dp.toPx() + tick, top), 1.5.dp.toPx())
        drawLine(s, Offset(right - corner - 6.dp.toPx(), bottom), Offset(right - corner - 6.dp.toPx() - tick, bottom), 1.5.dp.toPx())

        // Midline ticks.
        drawLine(s.copy(alpha = 0.55f), Offset(left - 10.dp.toPx(), top + frameHeight / 2), Offset(left, top + frameHeight / 2), 1.dp.toPx())
        drawLine(s.copy(alpha = 0.55f), Offset(right, top + frameHeight / 2), Offset(right + 10.dp.toPx(), top + frameHeight / 2), 1.dp.toPx())

        // Scanning beam with a subtle glow.
        val lineY = top + frameHeight * progress
        drawLine(
            color = s.copy(alpha = 0.16f),
            start = Offset(left + 2.dp.toPx(), lineY),
            end = Offset(right - 2.dp.toPx(), lineY),
            strokeWidth = 8.dp.toPx(),
        )
        drawLine(
            color = s,
            start = Offset(left + 8.dp.toPx(), lineY),
            end = Offset(right - 8.dp.toPx(), lineY),
            strokeWidth = 1.8.dp.toPx(),
        )
        drawCircle(s, radius = 2.4.dp.toPx(), center = Offset(left + 8.dp.toPx(), lineY))
        drawCircle(s, radius = 2.4.dp.toPx(), center = Offset(right - 8.dp.toPx(), lineY))
    }
}

@Composable
private fun ResultIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    error: Boolean = false,
) {
    val palette = LocalFieldPalette.current
    Surface(
        shape = FieldCut,
        color = if (error) palette.danger.copy(alpha = 0.10f) else palette.signal.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (error) palette.danger.copy(alpha = 0.45f) else palette.signalText.copy(alpha = 0.30f),
        ),
    ) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (error) palette.danger else palette.signalText,
                modifier = Modifier.size(32.dp),
            )
        }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
private fun PermissionCard(
    denied: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val palette = LocalFieldPalette.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = FieldCut,
        color = palette.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.line),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BrandMark(size = 54.dp)
            Spacer(Modifier.height(14.dp))
            FieldMicroLabel("CAMERA // PERMISSION REQUIRED", color = palette.signalText)
            Spacer(Modifier.height(5.dp))
            Text("需要相机权限", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (denied) "相机启动失败。请检查权限，或手动输入条码。"
                else "授权相机后即可扫描 InvenTree 二维码与一维条码。",
                style = MaterialTheme.typography.bodyMedium,
                color = palette.muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onRequest,
                modifier = Modifier.fillMaxWidth(),
                shape = FieldCutSmall,
            ) { Text("授予相机权限") }
            TextButton(onClick = onOpenSettings) { Text("打开系统设置") }
        }
    }
}
