package gy.roach.json.medminder.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationIntakeData
import gy.roach.json.medminder.db.MedicationRepository
import gy.roach.json.medminder.db.MedicationScheduleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Screen for managing medication schedules
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScheduleScreen(
    repository: MedicationRepository,
    medicationId: Long,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val medication = produceState<MedicationData?>(initialValue = null) {
        value = repository.getMedicationById(medicationId)
    }

    // Use a mutable state for schedules so we can update it
    var schedules by remember { mutableStateOf(emptyList<MedicationScheduleData>()) }

    // Load schedules initially
    LaunchedEffect(medicationId) {
        schedules = repository.getSchedulesForMedicationSync(medicationId)
    }

    // State for the add schedule dialog
    var showAddDialog by remember { mutableStateOf(false) }
    var newScheduleTime by remember { mutableStateOf("12:00") }
    var newScheduleDays by remember { mutableStateOf("1,2,3,4,5,6,7") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(medication.value?.name ?: "Medication Schedule") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
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
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)
        ) {
            Text(
                text = "Schedules",
                style = MaterialTheme.typography.headlineSmall
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (schedules.isEmpty()) {
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
                            contentDescription = "Add Schedule",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "No Schedules Yet",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Add a schedule with the + button to set up regular medication times.",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Schedules help you track your medication at specific times on specific days.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn {
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
                            onTimeChanged = { newScheduleTime = it },
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
}

/**
 * A custom time picker widget for selecting hours and minutes
 */
@Composable
fun TimePickerWidget(
    time: String,
    onTimeChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Parse the initial time
    val timeParts = time.split(":")
    var hours by remember { mutableStateOf(timeParts.getOrNull(0)?.toIntOrNull() ?: 12) }
    var minutes by remember { mutableStateOf(timeParts.getOrNull(1)?.toIntOrNull() ?: 0) }

    // Update the time whenever hours or minutes change
    LaunchedEffect(hours, minutes) {
        val formattedHours = hours.toString().padStart(2, '0')
        val formattedMinutes = minutes.toString().padStart(2, '0')
        onTimeChanged("$formattedHours:$formattedMinutes")
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Hours picker
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text("Hours", style = MaterialTheme.typography.bodyMedium)
            IconButton(
                onClick = { 
                    hours = if (hours >= 23) 0 else hours + 1 
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase hours")
            }

            OutlinedTextField(
                value = hours.toString().padStart(2, '0'),
                onValueChange = { newValue ->
                    val newHours = newValue.toIntOrNull() ?: 0
                    hours = when {
                        newHours > 23 -> 23
                        newHours < 0 -> 0
                        else -> newHours
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )

            IconButton(
                onClick = { 
                    hours = if (hours <= 0) 23 else hours - 1 
                }
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease hours")
            }
        }

        Text(":", style = MaterialTheme.typography.headlineLarge)

        // Minutes picker
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text("Minutes", style = MaterialTheme.typography.bodyMedium)
            IconButton(
                onClick = { 
                    minutes = if (minutes >= 59) 0 else minutes + 1 
                }
            ) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Increase minutes")
            }

            OutlinedTextField(
                value = minutes.toString().padStart(2, '0'),
                onValueChange = { newValue ->
                    val newMinutes = newValue.toIntOrNull() ?: 0
                    minutes = when {
                        newMinutes > 59 -> 59
                        newMinutes < 0 -> 0
                        else -> newMinutes
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.width(80.dp),
                textStyle = MaterialTheme.typography.bodyLarge,
                singleLine = true
            )

            IconButton(
                onClick = { 
                    minutes = if (minutes <= 0) 59 else minutes - 1 
                }
            ) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Decrease minutes")
            }
        }
    }
}

/**
 * Card that displays a single schedule
 */
@Composable
fun ScheduleItem(
    schedule: MedicationScheduleData,
    repository: MedicationRepository,
    scope: CoroutineScope,
    onDeleted: () -> Unit
) {
    var isEditing by remember(schedule.id) { mutableStateOf(false) }
    var currentTime by remember(schedule.time) { mutableStateOf(schedule.time) }

    // Check if medication has been taken today
    val today = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }

    // State to track if medication has been taken
    var isTaken by remember { mutableStateOf(false) }
    var intakes by remember { mutableStateOf<List<MedicationIntakeData>>(emptyList()) }

    // Load intake data for this schedule and today's date
    LaunchedEffect(schedule.id, today) {
        intakes = repository.getIntakesForScheduleAndDateSync(schedule.id, today)
        isTaken = intakes.any { 
            it.medicationId == schedule.medicationId && 
            it.scheduleId == schedule.id && 
            it.scheduledDate == today &&
            it.taken
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isTaken) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Always show the regular view
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Time: ${schedule.time}",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isTaken) Color.Gray else MaterialTheme.colorScheme.onPrimaryContainer,
                        textDecoration = if (isTaken) TextDecoration.LineThrough else TextDecoration.None
                    )

                    if (isTaken) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "✓ Taken",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            // Undo button
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        // Find the intake record for this schedule and today
                                        val intakeToDelete = intakes.firstOrNull { 
                                            it.medicationId == schedule.medicationId && 
                                            it.scheduleId == schedule.id && 
                                            it.scheduledDate == today &&
                                            it.taken
                                        }

                                        // Delete the intake record if found
                                        if (intakeToDelete != null) {
                                            repository.deleteIntake(intakeToDelete.id)

                                            // Refresh the intake status
                                            intakes = repository.getIntakesForScheduleAndDateSync(schedule.id, today)
                                            isTaken = intakes.any { 
                                                it.medicationId == schedule.medicationId && 
                                                it.scheduleId == schedule.id && 
                                                it.scheduledDate == today &&
                                                it.taken
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Undo")
                            }
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Edit button
                    IconButton(
                        onClick = { 
                            isEditing = !isEditing
                            currentTime = schedule.time
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Time"
                        )
                    }

                    // Delete button
                    IconButton(
                        onClick = {
                            scope.launch {
                                repository.deleteSchedule(schedule.id)
                                // Notify parent component to refresh the schedules list
                                onDeleted()
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Schedule"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            val daysText = schedule.daysOfWeek.split(",").joinToString(", ") { dayNumber ->
                when (dayNumber.trim()) {
                    "1" -> "Monday"
                    "2" -> "Tuesday"
                    "3" -> "Wednesday"
                    "4" -> "Thursday"
                    "5" -> "Friday"
                    "6" -> "Saturday"
                    "7" -> "Sunday"
                    else -> dayNumber
                }
            }

            Text(
                text = "Days: $daysText",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isTaken) Color.Gray else MaterialTheme.colorScheme.onPrimaryContainer
            )

            // Add Take Medication button if not taken
            if (!isTaken) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            // Record the intake
                            repository.recordIntake(
                                medicationId = schedule.medicationId,
                                scheduleId = schedule.id,
                                scheduledTime = schedule.time,
                                scheduledDate = today,
                                taken = true
                            )

                            // Refresh the intake status
                            intakes = repository.getIntakesForScheduleAndDateSync(schedule.id, today)
                            isTaken = intakes.any { 
                                it.medicationId == schedule.medicationId && 
                                it.scheduleId == schedule.id && 
                                it.scheduledDate == today &&
                                it.taken
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Take Medication",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Take Medication")
                }
            }

            // Show time picker below the schedule when editing
            if (isEditing) {
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Edit Time:",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                TimePickerWidget(
                    time = currentTime,
                    onTimeChanged = { newTime -> 
                        currentTime = newTime
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { 
                            isEditing = false
                            currentTime = schedule.time // Reset to original time if canceled
                        }
                    ) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            scope.launch {
                                repository.updateSchedule(
                                    id = schedule.id,
                                    time = currentTime,
                                    daysOfWeek = schedule.daysOfWeek
                                )
                                isEditing = false
                                onDeleted() // Refresh the list
                            }
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
