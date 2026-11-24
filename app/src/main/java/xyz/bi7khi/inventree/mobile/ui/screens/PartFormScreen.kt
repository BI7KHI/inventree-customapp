package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.PartCategoryDto
import xyz.bi7khi.inventree.mobile.data.api.PartCreateRequest
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.PartPatchRequest
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.CategoryPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.LocationPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartFormScreen(
    repository: InvenTreeRepository,
    partId: Int?,
    onBack: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    val isEdit = partId != null
    var loading by remember { mutableStateOf(isEdit) }
    var name by remember { mutableStateOf("") }
    var ipn by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var units by remember { mutableStateOf("") }
    var minimum by remember { mutableStateOf("0") }
    var maximum by remember { mutableStateOf("0") }
    var keywords by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<PartCategoryDto?>(null) }
    var defaultLocation by remember { mutableStateOf<LocationDto?>(null) }
    var active by remember { mutableStateOf(true) }
    var component by remember { mutableStateOf(false) }
    var assembly by remember { mutableStateOf(false) }
    var purchaseable by remember { mutableStateOf(false) }
    var salable by remember { mutableStateOf(false) }
    var trackable by remember { mutableStateOf(false) }
    var consumable by remember { mutableStateOf(false) }
    var virtual by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<PartCategoryDto>>(emptyList()) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var showCategories by remember { mutableStateOf(false) }
    var showLocations by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var original by remember { mutableStateOf<PartDto?>(null) }

    LaunchedEffect(partId) {
        runCatching { repository.allCategories() }.onSuccess { categories = it }
        runCatching { repository.allLocations() }.onSuccess { locations = it }
        if (partId != null) {
            runCatching { repository.part(partId) }
                .onSuccess { p ->
                    original = p
                    name = p.name
                    ipn = p.IPN.orEmpty()
                    description = p.description
                    units = p.units.orEmpty()
                    minimum = p.minimum_stock?.let { trimNumber(it) } ?: "0"
                    maximum = p.maximum_stock?.let { trimNumber(it) } ?: "0"
                    keywords = p.keywords.orEmpty()
                    link = p.link.orEmpty()
                    notes = p.notes.orEmpty()
                    category = categories.firstOrNull { it.pk == p.category }
                        ?: p.category_name?.let { nameValue -> PartCategoryDto(pk = p.category ?: 0, name = nameValue, pathstring = p.category_path) }
                    defaultLocation = locations.firstOrNull { it.pk == p.default_location }
                    active = p.active
                    component = p.component
                    assembly = p.assembly
                    purchaseable = p.purchaseable
                    salable = p.salable
                    trackable = p.trackable
                    consumable = p.consumable
                    virtual = p.virtual
                }
                .onFailure { error = it.toUserMessage() }
        }
        loading = false
    }

    if (loading) {
        FullScreenLoading("正在加载零件档案…")
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑零件" else "新建零件") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
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
                    SectionHeader("基本信息")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("零件名称 *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = name.isBlank() && error != null,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = ipn,
                        onValueChange = { ipn = it },
                        label = { Text("内部零件号 IPN") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item {
                AppCard {
                    SectionHeader("分类与单位")
                    Spacer(Modifier.height(8.dp))
                    PickerRow(
                        label = "分类",
                        value = category?.pathstring ?: category?.name ?: "未分类",
                        icon = Icons.Outlined.Folder,
                        onClick = { showCategories = true },
                    )
                    Spacer(Modifier.height(10.dp))
                    PickerRow(
                        label = "默认库位",
                        value = defaultLocation?.pathstring ?: defaultLocation?.name ?: "未指定",
                        icon = Icons.Outlined.Place,
                        onClick = { showLocations = true },
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = units,
                        onValueChange = { units = it },
                        label = { Text("计量单位（如 pcs、m、g）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item {
                AppCard {
                    SectionHeader("库存阈值")
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = minimum,
                            onValueChange = { minimum = it },
                            label = { Text("最小库存") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                        OutlinedTextField(
                            value = maximum,
                            onValueChange = { maximum = it },
                            label = { Text("最大库存") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                        )
                    }
                }
            }
            item {
                AppCard {
                    SectionHeader("零件类型")
                    SwitchRow("启用", "停用后不出现在默认零件列表", active) { active = it }
                    SwitchRow("组件", "可被其他零件装配使用", component) { component = it }
                    SwitchRow("装配件", "可以由 BOM 构建", assembly) { assembly = it }
                    SwitchRow("可采购", "可关联供应商与采购单", purchaseable) { purchaseable = it }
                    SwitchRow("可销售", "可出现在销售订单中", salable) { salable = it }
                    SwitchRow("可追踪", "库存项使用唯一序列号", trackable) { trackable = it }
                    SwitchRow("消耗品", "不参与库存数量统计", consumable) { consumable = it }
                    SwitchRow("虚拟零件", "无实物的软件或服务", virtual) { virtual = it }
                }
            }
            item {
                AppCard {
                    SectionHeader("补充信息")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keywords,
                        onValueChange = { keywords = it },
                        label = { Text("关键词") },
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
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            if (error != null) {
                item {
                    Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
            item {
                Button(
                    onClick = {
                        if (name.isBlank()) {
                            error = "零件名称不能为空"
                            return@Button
                        }
                        val min = minimum.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val max = maximum.replace(',', '.').toDoubleOrNull() ?: 0.0
                        scope.launch {
                            saving = true
                            error = null
                            runCatching {
                                if (partId == null) {
                                    repository.createPart(
                                        PartCreateRequest(
                                            name = name.trim(),
                                            description = description.trim(),
                                            IPN = ipn.trim(),
                                            category = category?.pk?.takeIf { it > 0 },
                                            units = units.trim().ifBlank { null },
                                            minimum_stock = min,
                                            maximum_stock = max,
                                            active = active,
                                            assembly = assembly,
                                            component = component,
                                            purchaseable = purchaseable,
                                            salable = salable,
                                            trackable = trackable,
                                            virtual = virtual,
                                            consumable = consumable,
                                            keywords = keywords.trim().ifBlank { null },
                                            link = link.trim().ifBlank { null },
                                            notes = notes.trim().ifBlank { null },
                                            default_location = defaultLocation?.pk,
                                        ),
                                    )
                                } else {
                                    repository.updatePart(
                                        partId,
                                        PartPatchRequest(
                                            name = name.trim(),
                                            description = description.trim(),
                                            IPN = ipn.trim(),
                                            category = category?.pk?.takeIf { it > 0 },
                                            units = units.trim(),
                                            minimum_stock = min,
                                            maximum_stock = max,
                                            active = active,
                                            assembly = assembly,
                                            component = component,
                                            purchaseable = purchaseable,
                                            salable = salable,
                                            trackable = trackable,
                                            virtual = virtual,
                                            consumable = consumable,
                                            keywords = keywords.trim(),
                                            link = link.trim(),
                                            notes = notes.trim(),
                                            default_location = defaultLocation?.pk,
                                        ),
                                    )
                                }
                            }.onSuccess { onSaved(it.pk) }
                                .onFailure { error = it.toUserMessage() }
                            saving = false
                        }
                    },
                    enabled = !saving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    else Text(if (isEdit) "保存更改" else "创建零件", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showCategories) {
        CategoryPickerDialog(
            categories = categories,
            selectedId = category?.pk,
            onSelect = { category = it },
            onDismiss = { showCategories = false },
        )
    }
    if (showLocations) {
        LocationPickerDialog(
            locations = locations,
            selectedId = defaultLocation?.pk,
            onSelect = { defaultLocation = it },
            onDismiss = { showLocations = false },
        )
    }
}

@Composable
private fun PickerRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun trimNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString()
    else value.toBigDecimal().stripTrailingZeros().toPlainString()
