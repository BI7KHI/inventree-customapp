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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Folder
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationCreateRequest
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.LocationPatchRequest
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.FullScreenLoading
import xyz.bi7khi.inventree.mobile.ui.components.LocationPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationFormScreen(
    repository: InvenTreeRepository,
    locationId: Int?,
    initialParentId: Int?,
    onBack: () -> Unit,
    onSaved: (Int) -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    val isEdit = locationId != null
    var loading by remember { mutableStateOf(isEdit) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var parent by remember { mutableStateOf<LocationDto?>(null) }
    var structural by remember { mutableStateOf(false) }
    var external by remember { mutableStateOf(false) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var showParents by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(locationId) {
        runCatching { repository.allLocations() }.onSuccess { all ->
            locations = all
            parent = all.firstOrNull { it.pk == initialParentId }
        }
        if (locationId != null) {
            runCatching { repository.location(locationId) }
                .onSuccess { loc ->
                    name = loc.name
                    description = loc.description
                    parent = locations.firstOrNull { it.pk == loc.parent }
                    structural = loc.structural
                    external = loc.external
                }
                .onFailure { error = it.toUserMessage() }
        }
        loading = false
    }

    if (loading) {
        FullScreenLoading("正在加载库位信息…")
        return
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "编辑库位" else "新建库位") },
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
                        label = { Text("库位名称 *") },
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
                        maxLines = 3,
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            item {
                AppCard {
                    SectionHeader("上级库位")
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showParents = true },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("上级库位", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    parent?.pathstring ?: parent?.name ?: "顶级库位",
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            item {
                AppCard {
                    SectionHeader("库位选项")
                    SwitchRow("结构库位", "只用于分组，不允许直接放置库存", structural) { structural = it }
                    SwitchRow("外部库位", "标记为不在主仓库中的位置", external) { external = it }
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
                            error = "库位名称不能为空"
                            return@Button
                        }
                        scope.launch {
                            saving = true
                            error = null
                            runCatching {
                                if (locationId == null) {
                                    repository.createLocation(
                                        LocationCreateRequest(
                                            name = name.trim(),
                                            description = description.trim(),
                                            parent = parent?.pk,
                                            structural = structural,
                                            external = external,
                                        ),
                                    )
                                } else {
                                    repository.updateLocation(
                                        locationId,
                                        LocationPatchRequest(
                                            name = name.trim(),
                                            description = description.trim(),
                                            parent = parent?.pk,
                                            structural = structural,
                                            external = external,
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
                    else Text(if (isEdit) "保存更改" else "创建库位", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showParents) {
        LocationPickerDialog(
            locations = locations.filter { it.pk != locationId },
            selectedId = parent?.pk,
            title = "选择上级库位",
            allowStructural = true,
            onSelect = { parent = it },
            onDismiss = { showParents = false },
        )
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
