package com.example.ui.screens.fleet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.fleet.DocumentType
import com.example.data.fleet.Driver
import com.example.data.fleet.FleetDocument
import com.example.data.fleet.Vehicle
import com.example.ui.viewmodel.FleetViewModel
import com.example.util.Formatters

@Composable
fun FleetDocumentsTab(
    viewModel: FleetViewModel,
    modifier: Modifier = Modifier
) {
    val documents by viewModel.filteredDocuments.collectAsStateWithLifecycle()
    val expiringOnly by viewModel.docFilterExpiringOnly.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val drivers by viewModel.drivers.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var docToEdit by remember { mutableStateOf<FleetDocument?>(null) }
    var docToDelete by remember { mutableStateOf<FleetDocument?>(null) }

    var isBatchMode by remember { mutableStateOf(false) }
    val selectedDocIds = remember { mutableStateListOf<Long>() }
    var showBatchEditDialog by remember { mutableStateOf(false) }
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (isBatchMode) {
                // Batch Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isBatchMode = false
                            selectedDocIds.clear()
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Отмена")
                    }
                    Text(
                        text = "Выбрано: ${selectedDocIds.size}",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (selectedDocIds.size == documents.size) {
                                selectedDocIds.clear()
                            } else {
                                selectedDocIds.clear()
                                selectedDocIds.addAll(documents.map { it.id })
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (selectedDocIds.size == documents.size) Icons.Default.Close else Icons.Default.SelectAll,
                            contentDescription = "Выбрать все"
                        )
                    }
                    IconButton(
                        onClick = { showBatchEditDialog = true },
                        enabled = selectedDocIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Пакетное изменение",
                            tint = if (selectedDocIds.isNotEmpty()) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                    IconButton(
                        onClick = { showBatchDeleteConfirm = true },
                        enabled = selectedDocIds.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Пакетное удаление",
                            tint = if (selectedDocIds.isNotEmpty()) MaterialTheme.colorScheme.error else Color.Gray
                        )
                    }
                }
            } else {
                // Regular Filter Chips & Batch Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !expiringOnly,
                            onClick = { viewModel.docFilterExpiringOnly.value = false },
                            label = { Text("Все (${documents.size})", fontSize = 12.sp) }
                        )
                        FilterChip(
                            selected = expiringOnly,
                            onClick = { viewModel.docFilterExpiringOnly.value = true },
                            label = { Text("Истекающие", fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                    if (documents.isNotEmpty()) {
                        IconButton(onClick = { isBatchMode = true }) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Пакетный выбор", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Документы не найдены",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val isSelected = selectedDocIds.contains(doc.id)
                        FleetDocumentCard(
                            doc = doc,
                            isSelectionMode = isBatchMode,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) selectedDocIds.remove(doc.id)
                                else selectedDocIds.add(doc.id)
                            },
                            onLongClick = {
                                if (!isBatchMode) {
                                    isBatchMode = true
                                    selectedDocIds.add(doc.id)
                                }
                            },
                            onEdit = {
                                docToEdit = doc
                                showAddDialog = true
                            },
                            onDelete = { docToDelete = doc }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }

        if (!isBatchMode) {
            FloatingActionButton(
                onClick = {
                    docToEdit = null
                    showAddDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Добавить документ")
            }
        }
    }

    if (showAddDialog) {
        FleetDocumentAddEditDialog(
            doc = docToEdit,
            vehicles = vehicles,
            drivers = drivers,
            onDismiss = { showAddDialog = false },
            onSave = { updatedDoc ->
                viewModel.saveDocument(updatedDoc)
                showAddDialog = false
            }
        )
    }

    docToDelete?.let { doc ->
        FleetDocumentDeleteDialog(
            doc = doc,
            onDismiss = { docToDelete = null },
            onConfirm = {
                viewModel.deleteDocument(doc)
                docToDelete = null
            }
        )
    }

    // Batch Delete Confirm
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            title = { Text("Удалить выбранные документы?") },
            text = { Text("Будет безвозвратно удалено ${selectedDocIds.size} документов.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.batchDeleteDocuments(selectedDocIds.toSet())
                        selectedDocIds.clear()
                        isBatchMode = false
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить (${selectedDocIds.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Batch Edit Dialog
    if (showBatchEditDialog) {
        BatchEditDocumentsDialog(
            selectedCount = selectedDocIds.size,
            vehicles = vehicles,
            drivers = drivers,
            onDismiss = { showBatchEditDialog = false },
            onSave = { newPlateOrDriver, newAuthority ->
                viewModel.batchUpdateDocuments(
                    ids = selectedDocIds.toSet(),
                    newVehiclePlateOrDriver = newPlateOrDriver,
                    newIssuingAuthority = newAuthority
                )
                selectedDocIds.clear()
                isBatchMode = false
                showBatchEditDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FleetDocumentCard(
    doc: FleetDocument,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        doc.isExpired -> Color(0xFFC62828)
        doc.isExpiringSoon -> Color(0xFFE65100)
        else -> Color(0xFF2E7D32)
    }

    val statusLabel = when {
        doc.isExpired -> "ПРОСРОЧЕН (${-doc.daysRemaining} дн. назад)"
        doc.isExpiringSoon -> "Истекает через ${doc.daysRemaining} дн."
        else -> "Действует (еще ${doc.daysRemaining} дн.)"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) onToggleSelect()
                    else onEdit()
                },
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    if (isSelectionMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when {
                                doc.isExpired -> Icons.Default.Error
                                doc.isExpiringSoon -> Icons.Default.Warning
                                else -> Icons.Default.Policy
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = doc.docType.label,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Привязка:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = doc.vehiclePlateOrDriver.ifBlank { "Не указано" },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Окончание действия:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatDate(doc.expiryDate),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (doc.isExpired) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            if (doc.seriesAndNumber.isNotBlank() || doc.issuingAuthority.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (doc.seriesAndNumber.isNotBlank()) {
                        Text(
                            text = "№ ${doc.seriesAndNumber}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (doc.issuingAuthority.isNotBlank()) {
                        Text(
                            text = doc.issuingAuthority,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (doc.cost > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Стоимость: ${Formatters.formatMoney(doc.cost)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!isSelectionMode) {
                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchEditDocumentsDialog(
    selectedCount: Int,
    vehicles: List<Vehicle>,
    drivers: List<Driver>,
    onDismiss: () -> Unit,
    onSave: (newPlateOrDriver: String?, newAuthority: String?) -> Unit
) {
    var changeTarget by remember { mutableStateOf(false) }
    var selectedTarget by remember { mutableStateOf("") }
    var targetDropdownExpanded by remember { mutableStateOf(false) }

    var changeAuthority by remember { mutableStateOf(false) }
    var authorityText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Пакетное изменение ($selectedCount док.)") },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = changeTarget, onCheckedChange = { changeTarget = it })
                    Text("Изменить привязку (ТС / Водитель)", fontSize = 13.sp)
                }
                if (changeTarget) {
                    ExposedDropdownMenuBox(
                        expanded = targetDropdownExpanded,
                        onExpandedChange = { targetDropdownExpanded = !targetDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedTarget,
                            onValueChange = { selectedTarget = it },
                            label = { Text("ТС или Водитель") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = targetDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryEditable),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = targetDropdownExpanded,
                            onDismissRequest = { targetDropdownExpanded = false }
                        ) {
                            vehicles.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text("ТС: ${v.plateNumber} (${v.model})") },
                                    onClick = {
                                        selectedTarget = v.plateNumber
                                        targetDropdownExpanded = false
                                    }
                                )
                            }
                            drivers.forEach { d ->
                                DropdownMenuItem(
                                    text = { Text("Водитель: ${d.fullName}") },
                                    onClick = {
                                        selectedTarget = d.fullName
                                        targetDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = changeAuthority, onCheckedChange = { changeAuthority = it })
                    Text("Изменить орган выдачи", fontSize = 13.sp)
                }
                if (changeAuthority) {
                    OutlinedTextField(
                        value = authorityText,
                        onValueChange = { authorityText = it },
                        label = { Text("Кем выдан") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = if (changeTarget && selectedTarget.isNotBlank()) selectedTarget else null
                    val auth = if (changeAuthority && authorityText.isNotBlank()) authorityText else null
                    onSave(target, auth)
                }
            ) {
                Text("Применить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FleetDocumentAddEditDialog(
    doc: FleetDocument?,
    vehicles: List<Vehicle> = emptyList(),
    drivers: List<Driver> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (FleetDocument) -> Unit
) {
    var docType by remember { mutableStateOf(doc?.docType ?: DocumentType.OSAGO) }
    var title by remember { mutableStateOf(doc?.title ?: docType.label) }
    var vehicleOrDriver by remember {
        mutableStateOf(doc?.vehiclePlateOrDriver ?: vehicles.firstOrNull()?.plateNumber ?: "")
    }
    var entityDropdownExpanded by remember { mutableStateOf(false) }
    var seriesAndNumber by remember { mutableStateOf(doc?.seriesAndNumber ?: "") }
    var authority by remember { mutableStateOf(doc?.issuingAuthority ?: "") }
    var costText by remember { mutableStateOf(doc?.cost?.toInt()?.toString() ?: "15000") }
    var daysValidText by remember { mutableStateOf("365") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doc == null) "Новый документ" else "Редактировать документ") },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название документа *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Document type chips (Горячие кнопки)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocumentType.values().forEach { dt ->
                        FilterChip(
                            selected = docType == dt,
                            onClick = {
                                docType = dt
                                title = dt.label
                            },
                            label = { Text(dt.label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                ExposedDropdownMenuBox(
                    expanded = entityDropdownExpanded,
                    onExpandedChange = { entityDropdownExpanded = !entityDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = vehicleOrDriver,
                        onValueChange = { vehicleOrDriver = it },
                        label = { Text("Привязка (ТС / Водитель) *") },
                        placeholder = { Text("Выберите авто или водителя...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = entityDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryEditable),
                        singleLine = true
                    )
                    ExposedDropdownMenu(
                        expanded = entityDropdownExpanded,
                        onDismissRequest = { entityDropdownExpanded = false }
                    ) {
                        if (vehicles.isEmpty() && drivers.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Нет доступных ТС и водителей") },
                                onClick = { entityDropdownExpanded = false }
                            )
                        } else {
                            if (vehicles.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("--- АВТОМОБИЛИ ---", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) },
                                    onClick = { }
                                )
                                vehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text("${v.plateNumber} (${v.model})") },
                                        onClick = {
                                            vehicleOrDriver = v.plateNumber
                                            entityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            if (drivers.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("--- ВОДИТЕЛИ ---", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) },
                                    onClick = { }
                                )
                                drivers.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.fullName) },
                                        onClick = {
                                            vehicleOrDriver = d.fullName
                                            entityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = seriesAndNumber,
                        onValueChange = { seriesAndNumber = it },
                        label = { Text("Серия и номер") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = daysValidText,
                        onValueChange = { daysValidText = it },
                        label = { Text("Срок (дней)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = authority,
                        onValueChange = { authority = it },
                        label = { Text("Кем выдан") },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Стоимость (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(0.8f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && vehicleOrDriver.isNotBlank()) {
                        val days = daysValidText.toLongOrNull() ?: 365L
                        val expiry = if (doc != null) {
                            doc.expiryDate
                        } else {
                            System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
                        }
                        val d = FleetDocument(
                            id = doc?.id ?: 0L,
                            title = title.trim(),
                            docType = docType,
                            vehiclePlateOrDriver = vehicleOrDriver.trim(),
                            seriesAndNumber = seriesAndNumber.trim(),
                            issueDate = doc?.issueDate ?: System.currentTimeMillis(),
                            expiryDate = expiry,
                            issuingAuthority = authority.trim(),
                            cost = costText.toDoubleOrNull() ?: 0.0
                        )
                        onSave(d)
                    }
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
fun FleetDocumentDeleteDialog(
    doc: FleetDocument,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Удалить документ?") },
        text = {
            Text("Вы уверены, что хотите удалить документ «${doc.title}» для ${doc.vehiclePlateOrDriver}?")
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Удалить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
