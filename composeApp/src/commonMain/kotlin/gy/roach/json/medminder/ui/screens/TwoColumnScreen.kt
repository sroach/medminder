package gy.roach.json.medminder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationReminderData
import gy.roach.json.medminder.db.MedicationRepository
import gy.roach.json.medminder.db.MedicationScheduleData
import gy.roach.json.medminder.ui.theme.IOSColors
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs

/**
 * Collapsed header that shows the total medication count
 */
@Composable
fun CollapsedMedicationHeader(
    medicationCount: Int,
    onExpandClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onExpandClick() }
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Medications",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "$medicationCount total",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Screen that displays medications and reminders in a two-column layout
 * with collapsible top section
 */
@Composable
fun TwoColumnScreen(repository: MedicationRepository) {
    // State for the selected medication
    var selectedMedicationId by remember { mutableStateOf<Long?>(null) }

    // State for tracking whether the top section is collapsed
    var isTopSectionCollapsed by remember { mutableStateOf(false) }

    // Collect medications to initialize the selected medication
    val medications = repository.getAllMedications().collectAsState(initial = emptyList())

    // When medications are loaded, select the first one if none is selected
    LaunchedEffect(medications.value) {
        if (selectedMedicationId == null && medications.value.isNotEmpty()) {
            // If no medication is selected and we have medications, select the first one
            selectedMedicationId = medications.value.first().id
        }
    }

    // Animate the weight of the top section
    val topSectionWeight by animateFloatAsState(
        targetValue = if (isTopSectionCollapsed) 0.05f else 0.3f,
        label = "topSectionWeight"
    )

    // Top-bottom layout
    Column(modifier = Modifier.fillMaxSize()) {
        // Top section - Medications (horizontally scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(topSectionWeight) // Animated weight for medications
        ) {
            if (isTopSectionCollapsed) {
                // Show collapsed view with medicine count
                CollapsedMedicationHeader(
                    medicationCount = medications.value.size,
                    onExpandClick = { isTopSectionCollapsed = false }
                )
            } else {
                // Show expanded medication list
                HorizontalMedicationListContent(
                    repository = repository,
                    medications = medications.value,
                    onMedicationSelected = { selectedMedicationId = it }
                )
            }
        }

        // Divider between top and bottom sections with collapse/expand functionality
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clickable { isTopSectionCollapsed = !isTopSectionCollapsed },
            contentAlignment = Alignment.Center
        ) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().height(1.dp),
                thickness = DividerDefaults.Thickness, color = MaterialTheme.colorScheme.outlineVariant
            )

            // Add a small indicator to show it's interactive
            Icon(
                imageVector = if (isTopSectionCollapsed) Icons.Default.Add else Icons.Default.Notifications,
                contentDescription = if (isTopSectionCollapsed) "Expand" else "Collapse",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        // Bottom section - Schedule or Empty Selection
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.7f) // 70% of height for details
        ) {
            if (selectedMedicationId != null) {
                // If a medication is selected, show the schedule content
                MedicationScheduleContent(
                    repository = repository,
                    medicationId = selectedMedicationId!!
                )
            } else {
                // Otherwise show empty selection content
                EmptySelectionContent()
            }
        }
    }
}

/**
 * Content of the MedicationListScreen without the Scaffold
 */
@Composable
fun MedicationListContent(
    repository: MedicationRepository,
    onMedicationSelected: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val medications = repository.getAllMedications().collectAsState(initial = emptyList())

    // State for the add medication dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newMedicationName by remember { mutableStateOf("") }
    var newMedicationDescription by remember { mutableStateOf("") }

    // State for the edit medication dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var editMedicationId by remember { mutableStateOf<Long?>(null) }
    var editMedicationName by remember { mutableStateOf("") }
    var editMedicationDescription by remember { mutableStateOf("") }

    // Use the same content as MedicationListScreen but without the Scaffold
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Your Medications",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (medications.value.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Medication",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Welcome to MedMinder!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Get started by adding your first medication using the + button below.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Once added, you can create schedules to help you stay on track with your medication.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(medications.value) { medication ->
                    MedicationItem(
                        medication = medication,
                        onClick = { onMedicationSelected(medication.id) },
                        onEditClick = { med ->
                            editMedicationId = med.id
                            editMedicationName = med.name
                            editMedicationDescription = med.description ?: ""
                            showEditDialog = true
                        },
                        onDeleteClick = { med ->
                            // Delete the medication using the repository
                            scope.launch {
                                repository.deleteMedication(med.id)
                            }
                        }
                    )
                }
            }
        }

        // Add medication button
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medication",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    // Add medication dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Medication") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMedicationName,
                        onValueChange = { newMedicationName = it },
                        label = { Text("Medication Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newMedicationDescription,
                        onValueChange = { newMedicationDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMedicationName.isNotBlank()) {
                            scope.launch {
                                repository.insertMedication(
                                    name = newMedicationName,
                                    description = newMedicationDescription.takeIf { it.isNotBlank() }
                                )
                                showAddDialog = false
                                newMedicationName = ""
                                newMedicationDescription = ""
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newMedicationName = ""
                        newMedicationDescription = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit medication dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Medication") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editMedicationName,
                        onValueChange = { editMedicationName = it },
                        label = { Text("Medication Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editMedicationDescription,
                        onValueChange = { editMedicationDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editMedicationName.isNotBlank() && editMedicationId != null) {
                            scope.launch {
                                repository.updateMedication(
                                    id = editMedicationId!!,
                                    name = editMedicationName,
                                    description = editMedicationDescription.takeIf { it.isNotBlank() }
                                )
                                showEditDialog = false
                                editMedicationId = null
                                editMedicationName = ""
                                editMedicationDescription = ""
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editMedicationId = null
                        editMedicationName = ""
                        editMedicationDescription = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Content for displaying medications in a horizontally scrollable list
 */
@Composable
fun HorizontalMedicationListContent(
    repository: MedicationRepository,
    medications: List<MedicationData>,
    onMedicationSelected: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    // State for the add medication dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newMedicationName by remember { mutableStateOf("") }
    var newMedicationDescription by remember { mutableStateOf("") }

    // State for the edit medication dialog
    var showEditDialog by remember { mutableStateOf(false) }
    var editMedicationId by remember { mutableStateOf<Long?>(null) }
    var editMedicationName by remember { mutableStateOf("") }
    var editMedicationDescription by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)
    ) {
        // Add medication button - Always at the top for visibility, especially on iOS
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Medication",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }

        if (medications.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Medication",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Welcome to MedMinder!",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Get started by adding your first medication using the + button above.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Horizontal scrolling list of medications
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(medications) { medication ->
                    HorizontalMedicationItem(
                        medication = medication,
                        onClick = { onMedicationSelected(medication.id) },
                        onEditClick = { med ->
                            editMedicationId = med.id
                            editMedicationName = med.name
                            editMedicationDescription = med.description ?: ""
                            showEditDialog = true
                        },
                        onDeleteClick = { med ->
                            // Delete the medication using the repository
                            scope.launch {
                                repository.deleteMedication(med.id)
                            }
                        }
                    )
                }
            }
        }
    }

    // Add medication dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Medication") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newMedicationName,
                        onValueChange = { newMedicationName = it },
                        label = { Text("Medication Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newMedicationDescription,
                        onValueChange = { newMedicationDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newMedicationName.isNotBlank()) {
                            scope.launch {
                                repository.insertMedication(
                                    name = newMedicationName,
                                    description = newMedicationDescription.takeIf { it.isNotBlank() }
                                )
                                showAddDialog = false
                                newMedicationName = ""
                                newMedicationDescription = ""
                            }
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newMedicationName = ""
                        newMedicationDescription = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit medication dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Medication") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editMedicationName,
                        onValueChange = { editMedicationName = it },
                        label = { Text("Medication Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editMedicationDescription,
                        onValueChange = { editMedicationDescription = it },
                        label = { Text("Description (Optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editMedicationName.isNotBlank() && editMedicationId != null) {
                            scope.launch {
                                repository.updateMedication(
                                    id = editMedicationId!!,
                                    name = editMedicationName,
                                    description = editMedicationDescription.takeIf { it.isNotBlank() }
                                )
                                showEditDialog = false
                                editMedicationId = null
                                editMedicationName = ""
                                editMedicationDescription = ""
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDialog = false
                        editMedicationId = null
                        editMedicationName = ""
                        editMedicationDescription = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Card that displays a single medication in a horizontal layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizontalMedicationItem(
    medication: MedicationData,
    onClick: () -> Unit,
    onEditClick: (MedicationData) -> Unit = {},
    onDeleteClick: (MedicationData) -> Unit = {}
) {
    // List of colors for medication cards
    val cardColors = listOf(
        IOSColors.LightPrimary,
        IOSColors.LightSecondary,
        IOSColors.LightTertiary,
        Color(0xFFFF9500), // Orange
        Color(0xFFFF2D55), // Pink
        Color(0xFF5856D6), // Purple
        Color(0xFFAF52DE)  // Violet
    )

    // Select a color based on the medication name's hash code
    val colorIndex = abs(medication.name.hashCode()) % cardColors.size
    val cardColor = cardColors[colorIndex]

    Box(
        modifier = Modifier
            .width(180.dp)
            .height(120.dp)
    ) {
        // Main card content
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface, // Use solid background color
                contentColor = MaterialTheme.colorScheme.onSurface // Keep text color consistent
            ),
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    elevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                ),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                cardColor.copy(alpha = 0.3f),
                                cardColor.copy(alpha = 0.1f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Display medication name
                Text(
                    text = medication.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(0.7f)
                )

                // Display description if available
                medication.description?.let { description ->
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(0.3f)
                        )
                    }
                }

                // Edit button at the bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { onEditClick(medication) },
                        modifier = Modifier.size(width = 80.dp, height = 32.dp)
                    ) {
                        Text(
                            text = "Edit",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Content to display when no medication is selected
 */
@Composable
fun EmptySelectionContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Select Medication",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Select a Medication",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Please select a medication from the list above to view and manage its schedule.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Content of the RemindersScreen without the Scaffold
 */
@Composable
fun RemindersContent(repository: MedicationRepository) {
    val scope = rememberCoroutineScope()

    // State for reminders (both acknowledged and unacknowledged)
    var reminders by remember { mutableStateOf(emptyList<MedicationReminderData>()) }
    var medicationsMap by remember { mutableStateOf(emptyMap<Long, MedicationData>()) }

    // Get current date to track day changes
    var currentDate by remember { 
        mutableStateOf(
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        ) 
    }

    // Function to load reminders and medications
    suspend fun loadRemindersAndMedications() {
        reminders = repository.getAllRemindersForCurrentTimeSync()

        // Load medication data for each reminder
        val medications = mutableMapOf<Long, MedicationData>()
        reminders.forEach { reminder ->
            repository.getMedicationById(reminder.medicationId)?.let { medication ->
                medications[medication.id] = medication
            }
        }
        medicationsMap = medications
    }

    // Initial load of reminders and medications
    LaunchedEffect(Unit) {
        loadRemindersAndMedications()
    }

    // Check for day changes and reload reminders when day changes
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(60000) // Check every minute

            val newDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

            // If the day has changed, reload reminders
            if (newDate != currentDate) {
                currentDate = newDate
                loadRemindersAndMedications()
            }
        }
    }

    // Use the same content as RemindersScreen but without the Scaffold
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text(
            text = "Medication Reminders",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (reminders.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "No Reminders",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Reminders Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val medicationsExist = medicationsMap.isNotEmpty()
                    if (medicationsExist) {
                        Text(
                            text = "Your reminders will appear here when it's time to take your medications.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Add medications from the left panel to get started with reminders.",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(reminders) { reminder ->
                    ReminderItem(
                        reminder = reminder,
                        medication = medicationsMap[reminder.medicationId],
                        onAcknowledge = {
                            scope.launch {
                                repository.acknowledgeReminder(reminder.id)

                                // Record the intake
                                repository.recordIntake(
                                    medicationId = reminder.medicationId,
                                    scheduleId = reminder.scheduleId,
                                    scheduledTime = reminder.scheduledTime,
                                    scheduledDate = reminder.scheduledDate
                                )

                                // Refresh the list using the shared function
                                loadRemindersAndMedications()
                            }
                        },
                        onDelete = {
                            scope.launch {
                                repository.deleteReminder(reminder.id)

                                // Refresh the list using the shared function
                                loadRemindersAndMedications()
                            }
                        }
                    )
                }
            }
        }
    }
}


/**
 * Content of the MedicationScheduleScreen without the Scaffold
 */
@Composable
fun MedicationScheduleContent(
    repository: MedicationRepository,
    medicationId: Long
) {
    val scope = rememberCoroutineScope()
    // Add a refresh trigger to reload medication data when needed
    var refreshTrigger by remember { mutableStateOf(0) }
    val medication = produceState<MedicationData?>(initialValue = null, key1 = medicationId, key2 = refreshTrigger) {
        value = repository.getMedicationById(medicationId)
    }

    // Use a mutable state for schedules so we can update it
    var schedules by remember(medicationId) { mutableStateOf<List<MedicationScheduleData>>(emptyList()) }

    // Load schedules initially
    LaunchedEffect(medicationId) {
        schedules = repository.getSchedulesForMedicationSync(medicationId)
    }

    // State for the add schedule dialog
    var showAddDialog by remember(medicationId) { mutableStateOf(false) }
    var newScheduleTime by remember(medicationId) { mutableStateOf("12:00") }
    var newScheduleDays by remember(medicationId) { mutableStateOf("1,2,3,4,5,6,7") }

    // State for editing description
    var showEditDescriptionDialog by remember(medicationId) { mutableStateOf(false) }
    var editedDescription by remember(medicationId) { mutableStateOf("") }

    // State for delete confirmation dialog
    var showDeleteConfirmDialog by remember(medicationId) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Delete button at the top right
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            IconButton(
                onClick = { showDeleteConfirmDialog = true }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Medication",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (schedules.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Display medication name and description at the top
                    Text(
                        text = medication.value?.name ?: "Medication Schedule",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                    )

                    // Display medication description if available
                    medication.value?.description?.let { description ->
                        if (description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Schedule",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Schedules Yet",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Add a schedule with the + button to set up regular medication times.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Schedules help you create reminders for taking your medication at specific times on specific days.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                // First item in the LazyColumn is the medication name and description
                item {
                    Column {
                        // Medication name
                        Text(
                            text = medication.value?.name ?: "Medication Schedule",
                            style = MaterialTheme.typography.headlineMedium
                        )

                        // Display medication description if available
                        medication.value?.description?.let { description ->
                            if (description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Schedules",
                            style = MaterialTheme.typography.titleLarge
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Then list all schedules
                items(schedules) { schedule ->
                    ScheduleItem(
                        schedule = schedule,
                        repository = repository,
                        scope = scope,
                        onDeleted = {
                            // Refresh schedules after deletion
                            scope.launch {
                                schedules = repository.getSchedulesForMedicationSync(medicationId)
                            }
                        }
                    )
                }
            }
        }

        // Add schedule button
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Schedule",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }

    // Edit description dialog
    if (showEditDescriptionDialog && medication.value != null) {
        AlertDialog(
            onDismissRequest = { showEditDescriptionDialog = false },
            title = { Text("Edit Description") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedDescription,
                        onValueChange = { editedDescription = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (medication.value != null) {
                            scope.launch {
                                repository.updateMedication(
                                    id = medicationId,
                                    name = medication.value!!.name,
                                    description = editedDescription.takeIf { it.isNotBlank() }
                                )
                                // Refresh medication data by incrementing the trigger
                                refreshTrigger++
                                showEditDescriptionDialog = false
                            }
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showEditDescriptionDialog = false
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog && medication.value != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Medication") },
            text = { 
                Text("Are you sure you want to delete ${medication.value?.name}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch {
                            repository.deleteMedication(medicationId)
                            showDeleteConfirmDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add schedule dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Schedule") },
            text = {
                Column {
                    Text("Time (24-hour format)")
                    TimePickerWidget(
                        time = newScheduleTime,
                        onTimeChanged = { newTime -> newScheduleTime = newTime },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Days of Week (1-7, where 1 is Monday)")
                    Text("Separate multiple days with commas", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newScheduleDays,
                        onValueChange = { newScheduleDays = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // No need for regex validation as TimePickerWidget ensures proper format
                        scope.launch {
                            repository.insertSchedule(
                                medicationId = medicationId,
                                time = newScheduleTime,
                                daysOfWeek = newScheduleDays
                            )
                            // Refresh schedules
                            schedules = repository.getSchedulesForMedicationSync(medicationId)
                            showAddDialog = false
                            newScheduleTime = "12:00"
                            newScheduleDays = "1,2,3,4,5,6,7"
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddDialog = false
                        newScheduleTime = "12:00"
                        newScheduleDays = "1,2,3,4,5,6,7"
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
