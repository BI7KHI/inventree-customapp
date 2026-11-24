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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import xyz.bi7khi.inventree.mobile.data.api.InvenTreeRepository
import xyz.bi7khi.inventree.mobile.data.api.LocationDto
import xyz.bi7khi.inventree.mobile.data.api.PartDto
import xyz.bi7khi.inventree.mobile.data.api.StockCreateRequest
import xyz.bi7khi.inventree.mobile.data.api.toUserMessage
import xyz.bi7khi.inventree.mobile.ui.StatusBarIcons
import xyz.bi7khi.inventree.mobile.ui.components.AppCard
import xyz.bi7khi.inventree.mobile.ui.components.FieldTopBar
import xyz.bi7khi.inventree.mobile.ui.components.LocationPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.PartPickerDialog
import xyz.bi7khi.inventree.mobile.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockCreateScreen(
    repository: InvenTreeRepository,
    initialPartId: Int?,
    onBack: () -> Unit,
    onCreated: (Int) -> Unit,
) {
    StatusBarIcons(lightIcons = false)
    var part by remember { mutableStateOf<PartDto?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var batch by remember { mutableStateOf("") }
    var serial by remember { mutableStateOf("") }
    var packaging by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var location by remember { mutableStateOf<LocationDto?>(null) }
    var locations by remember { mutableStateOf<List<LocationDto>>(emptyList()) }
    var showPartPicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val parsedQty = quantity.replace(',', '.').toDoubleOrNull()

    LaunchedEffect(Unit) {
        runCatching { repository.allLocations() }.onSuccess { locations = it }
        if (initialPartId != null) {
            runCatching { repository.part(initialPartId) }.onSuccess {
                part = it
                location = locations.firstOrNull { loc -> loc.pk == it.default_location }
                if (location == null && it.default_location != null) {
                    // Locations may still be loading; match after the list arrives.
                    location = runCatching { repository.location(it.default_location!!) }.getOrNull()
                }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FieldTopBar(
                title = "新建库存项",
                subtitle = "STOCK INTAKE // NEW LOT",
                code = "STOCK // IN",
                onBack = onBack,
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
                    SectionHeader("选择零件")
                    Spacer(Modifier.height(8.dp))
                    Surface(
                        onClick = { showPartPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    part?.name ?: "点击选择零件",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    part?.let { listOfNotNull(it.IPN?.takeIf { ipn -> ipn.isNotBlank() }, it.description.takeIf { d -> d.isNotBlank() }).joinToString(" · ") }
                                        ?: "必须先选择一个零件",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            item {
                AppCard {
                    SectionHeader("数量与库位")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { quantity = it },
                        label = { Text("数量") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = quantity.isNotBlank() && (parsedQty == null || parsedQty <= 0),
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = { showLocationPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    ) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Place, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                location?.pathstring ?: "选择库位（可选）",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            item {
                AppCard {
                    SectionHeader("批次与备注")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = batch,
                        onValueChange = { batch = it },
                        label = { Text("批次号（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = serial,
                        onValueChange = { serial = it },
                        label = { Text("序列号（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = packaging,
                        onValueChange = { packaging = it },
                        label = { Text("包装（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("备注（可选）") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
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
                        val selected = part ?: run {
                            error = "请先选择零件"
                            return@Button
                        }
                        val qty = parsedQty
                        if (qty == null || qty <= 0) {
                            error = "请输入大于 0 的数量"
                            return@Button
                        }
                        scope.launch {
                            saving = true
                            error = null
                            runCatching {
                                repository.createStock(
                                    StockCreateRequest(
                                        part = selected.pk,
                                        quantity = qty.toBigDecimal().stripTrailingZeros().toPlainString(),
                                        location = location?.pk,
                                        batch = batch.trim().ifBlank { null },
                                        serial = serial.trim().ifBlank { null },
                                        packaging = packaging.trim().ifBlank { null },
                                        notes = notes.trim().ifBlank { null },
                                    ),
                                )
                            }.onSuccess { onCreated(it.pk) }
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
                    if (saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("创建库存项", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showPartPicker) {
        PartPickerDialog(
            repository = repository,
            selectedId = part?.pk,
            onSelect = { part = it },
            onDismiss = { showPartPicker = false },
        )
    }
    if (showLocationPicker) {
        LocationPickerDialog(
            locations = locations,
            selectedId = location?.pk,
            onSelect = { location = it },
            onDismiss = { showLocationPicker = false },
        )
    }
}
