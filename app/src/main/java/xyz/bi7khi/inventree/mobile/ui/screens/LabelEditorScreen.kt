package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FormatAlignCenter
import androidx.compose.material.icons.outlined.FormatAlignLeft
import androidx.compose.material.icons.outlined.FormatAlignRight
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.data.label.DefaultLabelDesigns
import xyz.bi7khi.inventree.mobile.data.label.LabelDesign
import xyz.bi7khi.inventree.mobile.data.label.LabelDesignStore
import xyz.bi7khi.inventree.mobile.data.label.LabelElement
import xyz.bi7khi.inventree.mobile.data.label.LabelElementType
import xyz.bi7khi.inventree.mobile.data.label.LabelRenderer
import xyz.bi7khi.inventree.mobile.data.label.LabelTemplateType
import xyz.bi7khi.inventree.mobile.data.label.loadLabelDocument
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.FieldTopBar
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelEditorScreen(
    repository: InvenTreeRepository,
    typeKey: String,
    itemId: Int?,
    onBack: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    val context = LocalContext.current
    val type = remember(typeKey) { LabelTemplateType.fromKey(typeKey) }
    val store = remember { LabelDesignStore(context) }
    val scope = rememberCoroutineScope()
    var design by remember { mutableStateOf<LabelDesign?>(null) }
    var values by remember { mutableStateOf(LabelRenderer.sampleValues(type)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(type, itemId) {
        design = store.load(type)
        selectedId = design?.elements?.firstOrNull()?.id
        runCatching { repository.loadLabelDocument(type, itemId) }
            .onSuccess { values = it.values }
    }

    if (design == null) {
        FullScreenLoading("正在加载标签设计…")
        return
    }
    val current = design!!
    val currentDesign by rememberUpdatedState(current)
    val selected = current.elements.firstOrNull { it.id == selectedId }
    val bitmap = remember(current, values) {
        LabelRenderer.render(current, values, 203)
    }

    fun updateElement(block: (LabelElement) -> LabelElement) {
        val id = selectedId ?: return
        design = current.copy(
            elements = current.elements.map { if (it.id == id) block(it) else it },
        )
    }

    fun addElement(element: LabelElement) {
        val updated = current.copy(elements = current.elements + element)
        design = updated
        selectedId = element.id
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldTopBar(
                title = "标签设计器",
                subtitle = "${type.displayName} · 拖动预览中的元素调整位置",
                code = "LABEL // DESIGN",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                saving = true
                                error = null
                                runCatching { store.save(type, current) }
                                    .onSuccess { onBack() }
                                    .onFailure { error = it.toUserMessage() }
                                saving = false
                            }
                        },
                        enabled = !saving,
                    ) {
                        if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Check, contentDescription = "保存")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            AppCard {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(current.widthMm / current.heightMm)
                        .background(Color.White, RoundedCornerShape(12.dp))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { position ->
                                    selectedId = hitTest(
                                        currentDesign,
                                        size.width.toFloat(),
                                        size.height.toFloat(),
                                        position,
                                    )?.id
                                },
                                onDrag = { change, drag ->
                                    change.consume()
                                    val id = selectedId ?: return@detectDragGestures
                                    val w = size.width.toFloat().coerceAtLeast(1f)
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val snapshot = currentDesign
                                    design = snapshot.copy(
                                        elements = snapshot.elements.map { element ->
                                            if (element.id != id) element
                                            else element.copy(
                                                x = (element.x + drag.x / w)
                                                    .coerceIn(0f, (1f - element.width).coerceAtLeast(0f)),
                                                y = (element.y + drag.y / h)
                                                    .coerceIn(0f, (1f - element.height).coerceAtLeast(0f)),
                                            )
                                        },
                                    )
                                },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "标签预览",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit,
                    )
                    Canvas(Modifier.fillMaxSize()) {
                        val el = current.elements.firstOrNull { it.id == selectedId } ?: return@Canvas
                        val rect = LabelRenderer.elementRect(el, size.width, size.height)
                        drawRect(
                            color = Color(0xFF146B54),
                            topLeft = Offset(rect.left, rect.top),
                            size = Size(rect.width(), rect.height()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (itemId != null) "预览数据来自服务器当前记录" else "预览使用示例数据；从详情页打印时使用真实数据",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("纸张与边框")
                Spacer(Modifier.height(6.dp))
                SliderRow("宽度", current.widthMm, 25f..70f, "mm") {
                    design = current.copy(widthMm = it)
                }
                SliderRow("高度", current.heightMm, 15f..60f, "mm") {
                    design = current.copy(heightMm = it)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("绘制外边框", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Switch(
                        checked = current.border,
                        onCheckedChange = { design = current.copy(border = it) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            AppCard {
                SectionHeader("元素")
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(current.elements, key = { it.id }) { element ->
                        FilterChip(
                            selected = element.id == selectedId,
                            onClick = { selectedId = element.id },
                            label = {
                                Text(
                                    elementLabel(element),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            addElement(
                                LabelElement(
                                    id = "text-${System.currentTimeMillis()}",
                                    type = LabelElementType.TEXT,
                                    text = "新文本 {name}",
                                    x = 0.1f,
                                    y = 0.1f,
                                    width = 0.8f,
                                    height = 0.12f,
                                    fontSizeMm = 2.4f,
                                ),
                            )
                        },
                    ) {
                        Icon(Icons.Outlined.TextFields, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("文本")
                    }
                    OutlinedButton(
                        onClick = {
                            addElement(
                                LabelElement(
                                    id = "qr-${System.currentTimeMillis()}",
                                    type = LabelElementType.QR,
                                    text = "{qr}",
                                    x = 0.66f,
                                    y = 0.35f,
                                    width = 0.3f,
                                    height = 0.4f,
                                ),
                            )
                        },
                    ) {
                        Icon(Icons.Outlined.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("二维码")
                    }
                    OutlinedButton(
                        onClick = {
                            addElement(
                                LabelElement(
                                    id = "line-${System.currentTimeMillis()}",
                                    type = LabelElementType.LINE,
                                    x = 0.04f,
                                    y = 0.5f,
                                    width = 0.92f,
                                    height = 0.01f,
                                ),
                            )
                        },
                    ) {
                        Text("分隔线")
                    }
                }
            }

            if (selected != null) {
                Spacer(Modifier.height(12.dp))
                AppCard {
                    SectionHeader("编辑：${elementLabel(selected)}")
                    Spacer(Modifier.height(8.dp))
                    if (selected.type != LabelElementType.LINE) {
                        OutlinedTextField(
                            value = selected.text,
                            onValueChange = { value -> updateElement { it.copy(text = value) } },
                            label = {
                                Text(
                                    if (selected.type == LabelElementType.QR) "二维码内容（可用 {qr}、{url}、{code}）"
                                    else "文本内容（变量如 {name}、{ipn}、{quantity}）",
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 4,
                            shape = RoundedCornerShape(12.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    if (selected.type == LabelElementType.TEXT) {
                        SliderRow("字号", selected.fontSizeMm, 1.5f..6f, "mm") {
                            updateElement { el -> el.copy(fontSizeMm = it) }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("粗体", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Switch(
                                checked = selected.bold,
                                onCheckedChange = { value -> updateElement { it.copy(bold = value) } },
                            )
                        }
                        Text("对齐", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "left" to Icons.Outlined.FormatAlignLeft,
                                "center" to Icons.Outlined.FormatAlignCenter,
                                "right" to Icons.Outlined.FormatAlignRight,
                            ).forEach { (align, icon) ->
                                FilterChip(
                                    selected = selected.align == align,
                                    onClick = { updateElement { it.copy(align = align) } },
                                    label = { Icon(icon, contentDescription = align, modifier = Modifier.size(16.dp)) },
                                )
                            }
                        }
                    }
                    if (selected.type == LabelElementType.QR) {
                        SliderRow("二维码尺寸", selected.width * current.widthMm, 8f..30f, "mm") { mm ->
                            val fraction = (mm / current.widthMm).coerceIn(0.15f, 0.8f)
                            updateElement { it.copy(width = fraction, height = fraction * current.widthMm / current.heightMm) }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("显示此元素", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = selected.visible,
                            onCheckedChange = { value -> updateElement { it.copy(visible = value) } },
                        )
                    }
                    TextButton(
                        onClick = {
                            design = current.copy(elements = current.elements.filterNot { it.id == selected.id })
                            selectedId = current.elements.firstOrNull { it.id != selected.id }?.id
                        },
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("删除此元素", color = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = {
                        val defaults = DefaultLabelDesigns.forType(type)
                        design = defaults
                        selectedId = defaults.elements.firstOrNull()?.id
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("恢复默认") }
                Button(
                    onClick = {
                        scope.launch {
                            saving = true
                            error = null
                            runCatching { store.save(type, current) }
                                .onSuccess { onBack() }
                                .onFailure { error = it.toUserMessage() }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier.weight(1.4f),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("保存设计")
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    suffix: String,
    onChange: (Float) -> Unit,
) {
    Column {
        Row {
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text(
                "%.1f %s".format(value, suffix),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

private fun elementLabel(element: LabelElement): String = when (element.type) {
    LabelElementType.QR -> "二维码"
    LabelElementType.LINE -> "分隔线"
    LabelElementType.TEXT -> element.text.ifBlank { "文本" }.take(12)
}

private fun hitTest(
    design: LabelDesign,
    width: Float,
    height: Float,
    position: Offset,
): LabelElement? = design.elements
    .asReversed()
    .firstOrNull { element ->
        val rect = LabelRenderer.elementRect(element, width, height)
        rect.contains(position.x, position.y)
    }
