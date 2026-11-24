package xyz.bi7khi.inventree.mobile.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Warehouse
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.StockItemDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.AuthenticatedPartImage
import xyz.bi7khi.inventree.mobile.ui.components.EmptyState
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.LocationPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.QuantityPill
import xyz.bi7khi.inventree.mobile.ui.components.StatusBadge
import xyz.bi7khi.inventree.mobile.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockHubScreen(
    repository: InvenTreeRepository,
    initialQuery: String,
    initialTab: Int = 0,
    onOpenStock: (Int) -> Unit,
    onOpenLocation: (Int) -> Unit,
    onNewStock: () -> Unit,
    onNewLocation: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var refreshKey by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("库存与仓储", style = MaterialTheme.typography.titleLarge)
                        Text(
                            if (tab == 0) "在库物料、批次与序列号" else "库位结构与仓储空间",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { refreshKey++ }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { if (tab == 0) onNewStock() else onNewLocation() },
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text(if (tab == 0) "新建库存" else "新建库位") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.background,
                divider = {},
            ) {
                Tab(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    text = { Text("库存项") },
                    icon = { Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
                Tab(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    text = { Text("库位") },
                    icon = { Icon(Icons.Outlined.Warehouse, contentDescription = null, modifier = Modifier.size(18.dp)) },
                )
            }
            AnimatedContent(targetState = tab, label = "stock-tab") { current ->
                if (current == 0) {
                    StockItemsTab(
                        repository = repository,
                        initialQuery = initialQuery,
                        refreshKey = refreshKey,
                        onOpenStock = onOpenStock,
                    )
                } else {
                    LocationsTab(
                        repository = repository,
                        refreshKey = refreshKey,
                        onOpenLocation = onOpenLocation,
                    )
                }
            }
        }
    }
}

@Composable
private fun StockItemsTab(
    repository: InvenTreeRepository,
    initialQuery: String,
    refreshKey: Int,
    onOpenStock: (Int) -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf(initialQuery) }
    var appliedQuery by rememberSaveable { mutableStateOf(initialQuery.trim()) }
    var onlyInStock by rememberSaveable { mutableStateOf(true) }
    var locationFilter by remember { mutableStateOf<LocationDto?>(null) }
    var allLocations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<StockItemDto>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(searchText) {
        delay(if (searchText.isBlank()) 0 else 450)
        appliedQuery = searchText.trim()
    }

    LaunchedEffect(Unit) {
        runCatching { repository.allLocations() }.onSuccess { allLocations = it }
    }

    LaunchedEffect(appliedQuery, onlyInStock, locationFilter?.pk, refreshKey, reloadTick) {
        loading = true
        error = null
        runCatching {
            repository.stockItems(
                query = appliedQuery,
                locationId = locationFilter?.pk,
                inStock = if (onlyInStock) true else null,
                offset = 0,
            )
        }.onSuccess {
            items = it.results
            total = it.count
        }.onFailure {
            error = it.toUserMessage()
        }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedSearchField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "搜索零件、IPN、批次或序列号",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = onlyInStock,
                onClick = { onlyInStock = !onlyInStock },
                label = { Text(if (onlyInStock) "仅在库" else "全部状态") },
            )
            FilterChip(
                selected = locationFilter != null,
                onClick = { showLocationPicker = true },
                label = {
                    Text(
                        locationFilter?.name ?: "全部库位",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                leadingIcon = { Icon(Icons.Outlined.FilterAlt, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = if (locationFilter != null) {
                    { TextButton(onClick = { locationFilter = null }, contentPadding = PaddingValues(0.dp)) { Text("×") } }
                } else null,
            )
            Spacer(Modifier.weight(1f))
            Text(
                "$total 项",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
            error != null -> ErrorState(
                error!!,
                modifier = Modifier.fillMaxSize(),
                onRetry = { reloadTick++ },
            )
            items.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Inventory2,
                title = "没有找到库存项",
                description = if (appliedQuery.isBlank()) "尝试关闭筛选条件，或新建一个库存项。"
                else "换个关键词试试，或检查库位筛选。",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(items, key = { it.pk }) { item ->
                    StockItemCard(item, repository) { onOpenStock(item.pk) }
                }
                item {
                    if (items.size < total) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (loadingMore) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                TextButton(onClick = {
                                    scope.launch {
                                        loadingMore = true
                                        runCatching {
                                            repository.stockItems(
                                                query = appliedQuery,
                                                locationId = locationFilter?.pk,
                                                inStock = if (onlyInStock) true else null,
                                                offset = items.size,
                                            )
                                        }.onSuccess {
                                            items = items + it.results
                                            total = it.count
                                        }.onFailure { error = it.toUserMessage() }
                                        loadingMore = false
                                    }
                                }) { Text("加载更多（${items.size}/$total）") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLocationPicker) {
        LocationPickerDialog(
            locations = allLocations,
            selectedId = locationFilter?.pk,
            onSelect = { locationFilter = it },
            onDismiss = { showLocationPicker = false },
        )
    }
}

@Composable
fun StockItemCard(
    item: StockItemDto,
    repository: InvenTreeRepository,
    onClick: () -> Unit,
) {
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                contentAlignment = Alignment.Center,
            ) {
                AuthenticatedPartImage(
                    urls = repository.partImageUrls(
                        item.part_detail?.thumbnail,
                        item.part_detail?.image,
                    ),
                    token = repository.token,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    item.partName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val code = item.part_detail?.IPN?.takeIf { it.isNotBlank() }
                Text(
                    listOfNotNull(code, item.part_detail?.description?.takeIf { it.isNotBlank() })
                        .joinToString(" · ")
                        .ifBlank { "无 IPN" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Place,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        item.locationName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                QuantityPill(item.quantity, item.part_detail?.units, compact = true)
                Spacer(Modifier.height(5.dp))
                StatusBadge(item.status, item.status_text)
            }
        }
        if (!item.serial.isNullOrBlank() || !item.batch.isNullOrBlank() || !item.packaging.isNullOrBlank()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                item.batch?.takeIf { it.isNotBlank() }?.let { MetaChip("批次 $it") }
                item.serial?.takeIf { it.isNotBlank() }?.let { MetaChip("序列号 $it") }
                item.packaging?.takeIf { it.isNotBlank() }?.let { MetaChip(it) }
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LocationsTab(
    repository: InvenTreeRepository,
    refreshKey: Int,
    onOpenLocation: (Int) -> Unit,
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var appliedQuery by rememberSaveable { mutableStateOf("") }
    var items by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var total by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadTick by remember { mutableStateOf(0) }
    var expandedIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

    LaunchedEffect(searchText) {
        delay(if (searchText.isBlank()) 0 else 400)
        appliedQuery = searchText.trim()
    }
    LaunchedEffect(appliedQuery, refreshKey, reloadTick) {
        loading = true
        error = null
        runCatching { repository.locations(query = appliedQuery, limit = 250) }
            .onSuccess {
                items = it.results
                total = it.count
            }
            .onFailure { error = it.toUserMessage() }
        loading = false
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedSearchField(
            value = searchText,
            onValueChange = { searchText = it },
            placeholder = "搜索库位名称或完整路径",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        val childrenByParent = remember(items) { items.groupBy { it.parent } }
        val expandableIds = remember(items) {
            items.filter { location -> childrenByParent[location.pk].orEmpty().isNotEmpty() }
                .map { it.pk }
                .toSet()
        }
        val visibleLocations = remember(items, expandedIds, appliedQuery) {
            if (appliedQuery.isNotBlank()) {
                items.sortedBy { it.pathstring ?: it.name }
            } else {
                val roots = items
                    .filter { root -> root.parent == null || items.none { it.pk == root.parent } }
                    .sortedBy { it.name.lowercase() }
                val result = mutableListOf<LocationDto>()
                val visited = mutableSetOf<Int>()
                fun append(nodes: List<LocationDto>) {
                    nodes.forEach { node ->
                        if (!visited.add(node.pk)) return@forEach
                        result += node
                        if (node.pk in expandedIds) {
                            append(
                                childrenByParent[node.pk].orEmpty()
                                    .sortedBy { it.name.lowercase() },
                            )
                        }
                    }
                }
                append(roots)
                result
            }
        }
        val allExpanded = expandableIds.isNotEmpty() && expandedIds.containsAll(expandableIds)

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                if (appliedQuery.isBlank()) "$total 个库位" else "${visibleLocations.size} 个匹配库位",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            if (appliedQuery.isBlank() && expandableIds.isNotEmpty()) {
                TextButton(
                    onClick = {
                        expandedIds = if (allExpanded) emptySet() else expandableIds
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp),
                ) {
                    Text(
                        if (allExpanded) "全部收起" else "全部展开",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            } else if (appliedQuery.isNotBlank()) {
                Text(
                    "搜索结果",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    "点击箭头展开子库位",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
            }
            error != null -> ErrorState(error!!, modifier = Modifier.fillMaxSize(), onRetry = { reloadTick++ })
            visibleLocations.isEmpty() -> EmptyState(
                icon = Icons.Outlined.Warehouse,
                title = "没有找到库位",
                description = if (appliedQuery.isBlank()) "创建库位来组织你的物料。" else "换个关键词试试。",
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(visibleLocations, key = { it.pk }) { location ->
                    val hasChildren = appliedQuery.isBlank() &&
                        childrenByParent[location.pk].orEmpty().isNotEmpty()
                    LocationCard(
                        location = location,
                        hasChildren = hasChildren,
                        expanded = location.pk in expandedIds,
                        childCount = childrenByParent[location.pk].orEmpty().size,
                        onClick = { onOpenLocation(location.pk) },
                        onToggle = {
                            expandedIds = if (location.pk in expandedIds) {
                                expandedIds - location.pk
                            } else {
                                expandedIds + location.pk
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LocationCard(
    location: LocationDto,
    hasChildren: Boolean,
    expanded: Boolean,
    childCount: Int,
    onClick: () -> Unit,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label = "location-chevron",
    )
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.5.dp,
    ) {
        Row(
            Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width((location.level * 12).coerceAtMost(48).dp))
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (location.structural) MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.55f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (location.structural) Icons.Outlined.Folder else Icons.Outlined.Place,
                    contentDescription = null,
                    tint = if (location.structural) MaterialTheme.colorScheme.tertiary
                    else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    location.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    location.pathstring ?: location.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (location.items > 0) {
                Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        formatQty(location.items.toDouble()),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                Spacer(Modifier.width(4.dp))
            }
            if (hasChildren) {
                IconButton(
                    onClick = onToggle,
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    Icon(
                        Icons.Outlined.ExpandMore,
                        contentDescription = if (expanded) "收起 $childCount 个子库位" else "展开 $childCount 个子库位",
                        modifier = Modifier.rotate(rotation),
                    )
                }
            } else {
                Spacer(Modifier.width(10.dp))
            }
        }
    }
}

@Composable
fun OutlinedSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (value.isNotEmpty()) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Text("×", style = MaterialTheme.typography.titleMedium)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
    )
}
