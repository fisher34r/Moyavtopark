package com.example.ui.screens.fleet

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter: All vs Expiring/Expired Only
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !expiringOnly,
                    onClick = { viewModel.docFilterExpiringOnly.value = false },
                    label = { Text("Все документы (${documents.size})", fontSize = 12.sp) }
                )
                FilterChip(
                    selected = expiringOnly,
                    onClick = { viewModel.docFilterExpiringOnly.value = true },
                    label = { Text("Требуют внимания (истекают)", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                    }
                )
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
                        FleetDocumentCard(
                            doc = doc,
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

        FloatingActionButton(
            onClick = {
                docToEdit = null
                showAddDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_doc_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Добавить документ")
        }
    }

    if (showAddDialog) {
        FleetDocumentAddEditDialog(
            doc = docToEdit,
            vehicles = vehicles,
            drivers = drivers,
            onDismiss = { showAddDialog = false },
            onSave = {
                viewModel.saveDocument(it)
                showAddDialog = false
            }
        )
    }

    docToDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { docToDelete = null },
            title = { Text("Удалить документ?") },
            text = { Text("Удалить ${d.title} (${d.vehiclePlateOrDriver})?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteDocument(d)
                        docToDelete = null
                    }
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { docToDelete = null }) { Text("Отмена") }
            }
        )
    }
}

@Composable
fun FleetDocumentCard(
    doc: FleetDocument,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        doc.isExpired -> Color(0xFFC62828) // Red
        doc.isExpiringSoon -> Color(0xFFE65100) // Orange
        else -> Color(0xFF2E7D32) // Green
    }

    val statusLabel = when {
        doc.isExpired -> "ПРОСРОЧЕН (${-doc.daysRemaining} дн. назад)"
        doc.isExpiringSoon -> "Истекает через ${doc.daysRemaining} дн."
        else -> "Действует (еще ${doc.daysRemaining} дн.)"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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

                // Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Attached Object & Number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Объект: ${doc.vehiclePlateOrDriver}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (doc.cost > 0) {
                    Text(
                        text = Formatters.formatMoney(doc.cost),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (doc.seriesAndNumber.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Серия и №: ${doc.seriesAndNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Срок: до ${Formatters.formatDate(doc.expiryDate)}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (doc.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                if (doc.issuingAuthority.isNotBlank()) {
                    Text(
                        text = doc.issuingAuthority,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Редактировать", modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
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
    var title by remember { mutableStateOf(doc?.title ?: "Полис ОСАГО") }
    var docType by remember { mutableStateOf(doc?.docType ?: DocumentType.OSAGO) }
    var vehicleOrDriver by remember {
        mutableStateOf(doc?.vehiclePlateOrDriver ?: vehicles.firstOrNull()?.plateNumber ?: "А 742 КХ 123")
    }
    var entityDropdownExpanded by remember { mutableStateOf(false) }
    var seriesAndNumber by remember { mutableStateOf(doc?.seriesAndNumber ?: "") }
    var authority by remember { mutableStateOf(doc?.issuingAuthority ?: "СПАО Ингосстрах") }
    var costText by remember { mutableStateOf(doc?.cost?.toInt()?.toString() ?: "15000") }
    var daysValidText by remember { mutableStateOf("365") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (doc == null) "Добавить документ" else "Редактировать документ") },
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

                // Document type chips
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
                                if (title == "Полис ОСАГО" || title.isBlank()) {
                                    title = dt.label
                                }
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
                        placeholder = { Text("Выберите ТС или водителя...") },
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
                                text = { Text("Нет данных в автопарке") },
                                onClick = { entityDropdownExpanded = false }
                            )
                        } else {
                            if (vehicles.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("— ТРАНСПОРТНЫЕ СРЕДСТВА —", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) },
                                    onClick = { }
                                )
                                vehicles.forEach { v ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(v.plateNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("${v.model} (${v.type.label})", fontSize = 11.sp, color = Color.Gray)
                                            }
                                        },
                                        onClick = {
                                            vehicleOrDriver = v.plateNumber
                                            entityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                            if (drivers.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("— ВОДИТЕЛИ —", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp) },
                                    onClick = { }
                                )
                                drivers.forEach { d ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(d.fullName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                if (d.assignedVehiclePlate.isNotBlank()) {
                                                    Text("Закреплен за: ${d.assignedVehiclePlate}", fontSize = 11.sp, color = Color.Gray)
                                                }
                                            }
                                        },
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
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = costText,
                        onValueChange = { costText = it },
                        label = { Text("Стоимость (₽)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = daysValidText,
                        onValueChange = { daysValidText = it },
                        label = { Text("Срок действия (дней)") },
                        placeholder = { Text("365") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = authority,
                        onValueChange = { authority = it },
                        label = { Text("Кем выдан") },
                        modifier = Modifier.weight(1f),
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
                        val expiry = System.currentTimeMillis() + days * 24L * 60 * 60 * 1000
                        val d = FleetDocument(
                            id = doc?.id ?: 0L,
                            title = title.trim(),
                            docType = docType,
                            vehiclePlateOrDriver = vehicleOrDriver.trim(),
                            seriesAndNumber = seriesAndNumber.trim(),
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
