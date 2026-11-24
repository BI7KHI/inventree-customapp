package xyz.bi7khi.inventree.mobile.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.PartCategoryDto
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage

@Composable
fun LocationPickerDialog(
    locations: List<LocationDto>,
    selectedId: Int?,
    title: String = "选择库位",
    allowStructural: Boolean = false,
    onSelect: (LocationDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var expandedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }
    val childrenByParent = remember(locations) { locations.groupBy { it.parent } }
    val filtered = remember(locations, query, allowStructural) {
        if (query.isNotBlank()) {
            locations.filter {
                (allowStructural || !it.structural) &&
                    (it.name.contains(query, ignoreCase = true) ||
                        it.pathstring.orEmpty().contains(query, ignoreCase = true))
            }
        } else {
            val roots = locations
                .filter { it.parent == null || locations.none { parent -> parent.pk == it.parent } }
                .sortedBy { it.name.lowercase() }
            val result = mutableListOf<LocationDto>()
            val visited = mutableSetOf<Int>()
            fun append(nodes: List<LocationDto>) {
                nodes.forEach { node ->
                    if (!visited.add(node.pk)) return@forEach
                    result += node
                    if (node.pk in expandedIds) {
                        append(childrenByParent[node.pk].orEmpty().sortedBy { it.name.lowercase() })
                    }
                }
            }
            append(roots)
            result
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索库位") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.size(10.dp))
                if (filtered.isEmpty()) {
                    Text(
                        "没有匹配的库位",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(filtered, key = { it.pk }) { location ->
                            val canSelect = allowStructural || !location.structural
                            val hasChildren = query.isBlank() &&
                                childrenByParent[location.pk].orEmpty().isNotEmpty()
                            val expanded = location.pk in expandedIds
                            val rotation by animateFloatAsState(
                                targetValue = if (expanded) 0f else -90f,
                                label = "picker-chevron",
                            )
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(Modifier.width((location.level * 12).coerceAtMost(48).dp))
                                Row(
                                    Modifier
                                        .weight(1f)
                                        .clickable(enabled = canSelect) {
                                            onSelect(location)
                                            onDismiss()
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        if (location.structural) Icons.Outlined.Folder else Icons.Outlined.Place,
                                        contentDescription = null,
                                        tint = if (location.pk == selectedId) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(Modifier.size(10.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            location.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (location.pk == selectedId) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            "${location.items} 个库存项" + if (location.structural) " · 结构库位" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (hasChildren) {
                                    androidx.compose.material3.IconButton(
                                        onClick = {
                                            expandedIds = if (expanded) expandedIds - location.pk
                                            else expandedIds + location.pk
                                        },
                                        modifier = Modifier.size(36.dp),
                                    ) {
                                        Icon(
                                            Icons.Outlined.ExpandMore,
                                            contentDescription = if (expanded) "收起" else "展开",
                                            modifier = Modifier.rotate(rotation),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                } else {
                                    Spacer(Modifier.width(36.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun PartPickerDialog(
    repository: InvenTreeRepository,
    selectedId: Int?,
    onSelect: (PartDto) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<PartDto>>(emptyList()) }

    LaunchedEffect(query) {
        loading = true
        delay(if (query.isBlank()) 0 else 300)
        runCatching { repository.parts(query = query, limit = 40).results }
            .onSuccess {
                results = it
                error = null
            }
            .onFailure { error = it.toUserMessage() }
        loading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择零件") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("名称、IPN 或描述") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.size(8.dp))
                if (loading) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) { CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp) }
                } else if (error != null) {
                    Text(
                        error!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp),
                    )
                } else {
                    LazyColumn(Modifier.heightIn(max = 420.dp)) {
                        items(results, key = { it.pk }) { part ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelect(part)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Outlined.Inventory2,
                                    contentDescription = null,
                                    tint = if (part.pk == selectedId) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.size(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        part.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    val sub = listOfNotNull(
                                        part.IPN?.takeIf { it.isNotBlank() },
                                        part.description.takeIf { it.isNotBlank() },
                                    ).joinToString(" · ")
                                    if (sub.isNotBlank()) {
                                        Text(
                                            sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        if (results.isEmpty()) {
                            item {
                                Text(
                                    "没有匹配的零件",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun CategoryPickerDialog(
    categories: List<PartCategoryDto>,
    selectedId: Int?,
    onSelect: (PartCategoryDto?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(categories, query) {
        categories.filter {
            query.isBlank() || (it.pathstring ?: it.name).contains(query, ignoreCase = true)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择分类") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索分类") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
                Spacer(Modifier.size(8.dp))
                LazyColumn(Modifier.heightIn(max = 400.dp)) {
                    item {
                        Text(
                            "不分类",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(null)
                                    onDismiss()
                                }
                                .padding(12.dp),
                        )
                    }
                    items(filtered, key = { it.pk }) { category ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelect(category)
                                    onDismiss()
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                category.pathstring ?: category.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (category.pk == selectedId) FontWeight.Bold else FontWeight.Normal,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
fun StatusPickerDialog(
    selected: Int?,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        10 to "正常 (OK)",
        50 to "需关注 (Attention)",
        55 to "损坏 (Damaged)",
        60 to "已销毁 (Destroyed)",
        65 to "已拒收 (Rejected)",
        70 to "丢失 (Lost)",
        75 to "隔离 (Quarantined)",
        85 to "已退货 (Returned)",
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("库存状态") },
        text = {
            Column {
                options.forEach { (code, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(code)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (code == selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (code == selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
