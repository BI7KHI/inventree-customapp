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
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.AuthenticatedPartImage
import xyz.bi7khi.inventree.mobile.ui.components.EmptyState
import xyz.bi7khi.inventree.mobile.ui.components.ErrorState
import xyz.bi7khi.inventree.mobile.ui.components.formatQty

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartListScreen(
    repository: InvenTreeRepository,
    initialQuery: String,
    onOpenPart: (Int) -> Unit,
    onNewPart: () -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var searchText by rememberSaveable { mutableStateOf(initialQuery) }
    var appliedQuery by rememberSaveable { mutableStateOf(initialQuery.trim()) }
    var lowStockOnly by rememberSaveable { mutableStateOf(false) }
    var items by remember { mutableStateOf<List<PartDto>>(emptyList()) }
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
    LaunchedEffect(appliedQuery, lowStockOnly, reloadTick) {
        loading = true
        error = null
        runCatching {
            repository.parts(
                query = appliedQuery,
                lowStock = if (lowStockOnly) true else null,
                offset = 0,
            )
        }.onSuccess {
            items = it.results
            total = it.count
        }.onFailure { error = it.toUserMessage() }
        loading = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("零件库", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "$total 个零件 · 与网页端同步",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNewPart,
                icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                text = { Text("新建零件") },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedSearchField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "搜索名称、IPN 或描述",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = lowStockOnly,
                    onClick = { lowStockOnly = !lowStockOnly },
                    label = { Text("低库存") },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                        )
                    },
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { reloadTick++ }) { Text("刷新") }
            }
            when {
                loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 2.5.dp)
                }
                error != null -> ErrorState(error!!, modifier = Modifier.fillMaxSize(), onRetry = { reloadTick++ })
                items.isEmpty() -> EmptyState(
                    icon = Icons.Outlined.Inventory2,
                    title = "没有找到零件",
                    description = if (appliedQuery.isBlank()) "创建第一个零件，或关闭低库存筛选。" else "换个关键词试试。",
                    modifier = Modifier.fillMaxSize(),
                    actionLabel = "新建零件",
                    onAction = onNewPart,
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items, key = { it.pk }) { part ->
                        PartCard(repository, part) { onOpenPart(part.pk) }
                    }
                    if (items.size < total) {
                        item {
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
                                                repository.parts(
                                                    query = appliedQuery,
                                                    lowStock = if (lowStockOnly) true else null,
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
    }
}

@Composable
fun PartCard(
    repository: InvenTreeRepository,
    part: PartDto,
    onClick: () -> Unit,
) {
    val imageUrls = repository.partImageUrls(part.thumbnail, part.image)
    AppCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
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
                            modifier = Modifier.size(28.dp),
                        )
                    },
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    part.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        part.IPN?.takeIf { it.isNotBlank() },
                        part.category_name?.takeIf { it.isNotBlank() },
                    ).joinToString(" · ").ifBlank { "未分类" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(50),
                    ) {
                        Text(
                            "库存 ${formatQty(part.total_in_stock ?: 0.0)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        )
                    }
                    val minimum = part.minimum_stock ?: 0.0
                    if (minimum > 0 && (part.total_in_stock ?: 0.0) < minimum) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(13.dp))
                        Text(
                            "低于最小 ${formatQty(minimum)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
        if (part.description.isNotBlank()) {
            Spacer(Modifier.height(9.dp))
            Text(
                part.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
