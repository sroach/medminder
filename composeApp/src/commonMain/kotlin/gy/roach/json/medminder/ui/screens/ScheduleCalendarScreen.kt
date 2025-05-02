package gy.roach.json.medminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.*
import gy.roach.json.medminder.pdf.createPdfService
import gy.roach.json.medminder.ui.theme.IOSColors
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlin.math.abs

/**
 * Screen for displaying medication schedules by day
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleCalendarScreen(
    repository: MedicationRepository,
    onBackClick: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Create PDF service
    val pdfService = remember { createPdfService() }

    // State for day selection (0 = today, -1 = yesterday, 1 = tomorrow)
    var selectedDay by remember { mutableStateOf(0) }

    // State for schedules, medications, intakes, and reminders
    var schedules by remember { mutableStateOf(emptyList<MedicationScheduleData>()) }
    var medications by remember { mutableStateOf(emptyMap<Long, MedicationData>()) }

    // Collect all intakes as state to observe changes
    val allIntakes = repository.getAllIntakes().collectAsState(initial = emptyList())

    // Collect all reminders as state to observe changes
    val allReminders = repository.getAllReminders().collectAsState(initial = emptyList())

    // Filter intakes for the selected day
    var intakes by remember { mutableStateOf(emptyList<MedicationIntakeData>()) }

    // Filter reminders for the selected day
    var reminders by remember { mutableStateOf(emptyList<MedicationReminderData>()) }

    // Get the day name based on the selected day
    val dayName = when (selectedDay) {
        -1 -> "Yesterday"
        0 -> "Today"
        1 -> "Tomorrow"
        else -> "Day $selectedDay"
    }

    // Calculate current date and selected date
    val currentDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val selectedDate = currentDate.plus(DatePeriod(days = selectedDay))

    // Get the day of week (1-7, where 1 is Monday)
    val dayOfWeek = selectedDate.dayOfWeek.isoDayNumber

    // Calculate date string for the selected day
    val dateString = selectedDate.toString()

    // Collect schedules as state
    val allSchedules = repository.getAllSchedules().collectAsState(initial = emptyList())

    // Load schedules for the selected day
    LaunchedEffect(selectedDay, allSchedules.value) {
        // Filter schedules for the selected day of week
        schedules = allSchedules.value.filter { schedule ->
            schedule.daysOfWeek.split(",").map { it.trim() }.contains(dayOfWeek.toString())
        }

        // Load medication data for each schedule
        val medicationMap = mutableMapOf<Long, MedicationData>()
        schedules.forEach { schedule ->
            repository.getMedicationById(schedule.medicationId)?.let { medication ->
                medicationMap[medication.id] = medication
            }
        }
        medications = medicationMap
    }

    // Filter intakes for the selected day whenever allIntakes changes
    LaunchedEffect(selectedDay, allIntakes.value) {
        intakes = allIntakes.value.filter { intake ->
            intake.scheduledDate == dateString
        }
    }

    // Filter reminders for the selected day whenever allReminders changes
    LaunchedEffect(selectedDay, allReminders.value) {
        reminders = allReminders.value.filter { reminder ->
            reminder.scheduledDate == dateString
        }
    }

    Scaffold(
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Compact header with date navigation and Today button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left navigation button
                IconButton(
                    onClick = { selectedDay -= 1 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous Day"
                    )
                }

                // Center section with day name and Today button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Small Today button
                    TextButton(
                        onClick = { selectedDay = 0 },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Today",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Today", style = MaterialTheme.typography.labelMedium)
                    }
                }

                // Right navigation button
                IconButton(
                    onClick = { selectedDay += 1 },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next Day"
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Schedule list
            if (schedules.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "No Schedules",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "No Schedules for $dayName",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Check if there are any medications
                        val hasMedications = allSchedules.value.isNotEmpty()

                        if (hasMedications) {
                            Text(
                                text = "Try selecting a different day or add more medication schedules.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        } else {
                            Text(
                                text = "Add medications and create schedules to see them in the calendar.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Go to the Medications tab to get started.",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Schedules",
                        style = MaterialTheme.typography.titleMedium
                    )

                    // PDF Generation button
                    IconButton(
                        onClick = {
                            scope.launch {
                                // Generate PDF with current medications and schedules
                                val medicationMap = mutableMapOf<Long, List<MedicationScheduleData>>()
                                medications.keys.forEach { medicationId ->
                                    medicationMap[medicationId] = schedules.filter { it.medicationId == medicationId }
                                }

                                val pdfPath = pdfService.generateMedicationPdf(
                                    medications = medications.values.toList(),
                                    schedules = medicationMap,
                                    fileName = "MedMinder-Schedule-$dayName"
                                )

                                // Open the generated PDF
                                pdfPath?.let { path ->
                                    pdfService.openPdf(path)
                                }
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PictureAsPdf,
                            contentDescription = "Generate PDF",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()  // Changed to fillMaxWidth instead of fillMaxHeight
                        .height(1.dp),   // Changed to height instead of width
                    thickness = DividerDefaults.Thickness, color = Color.Gray.copy(alpha = 0.5f)
                )

                LazyColumn {
                    items(schedules.sortedBy { it.time }) { schedule ->
                        // Date string is already calculated above

                        DayScheduleItem(
                            schedule = schedule,
                            medication = medications[schedule.medicationId],
                            date = dateString,
                            intakes = intakes,
                            reminders = reminders,
                            onTakeMedication = { medicationId, scheduleId, scheduledTime, scheduledDate ->
                                scope.launch {
                                    repository.recordIntake(
                                        medicationId = medicationId,
                                        scheduleId = scheduleId,
                                        scheduledTime = scheduledTime,
                                        scheduledDate = scheduledDate
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Card that displays a single schedule for a day
 */
@Composable
fun DayScheduleItem(
    schedule: MedicationScheduleData,
    medication: MedicationData?,
    date: String,
    intakes: List<MedicationIntakeData> = emptyList(),
    reminders: List<MedicationReminderData> = emptyList(),
    onTakeMedication: (Long, Long, String, String) -> Unit = { _, _, _, _ -> }
) {
    // Check if medication has been taken
    val isTaken = intakes.any { 
        it.medicationId == schedule.medicationId && 
        it.scheduleId == schedule.id && 
        it.scheduledDate == date &&
        it.taken
    }

    // Check if there's a reminder for this medication and schedule
    val hasReminder = reminders.any {
        it.medicationId == schedule.medicationId &&
        it.scheduleId == schedule.id &&
        it.scheduledDate == date
    }

    // Get the reminder if it exists
    val reminder = reminders.find {
        it.medicationId == schedule.medicationId &&
        it.scheduleId == schedule.id &&
        it.scheduledDate == date
    }

    // List of colors for medication cards - same as in HorizontalMedicationItem
    val cardColors = listOf(
        IOSColors.LightPrimary,
        IOSColors.LightSecondary,
        IOSColors.LightTertiary,
        Color(0xFFFF9500), // Orange
        Color(0xFFFF2D55), // Pink
        Color(0xFF5856D6), // Purple
        Color(0xFFAF52DE)  // Violet
    )

    // Select a color based on the medication name's hash code - same logic as in HorizontalMedicationItem
    val colorIndex = if (medication != null) {
        abs(medication.name.hashCode()) % cardColors.size
    } else {
        // Fallback to using medicationId if medication name is not available
        abs(schedule.medicationId.hashCode()) % cardColors.size
    }
    val cardColor = cardColors[colorIndex]

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            cardColor.copy(alpha = 0.3f),
                            cardColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            // Medication name and taken status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = medication?.name ?: "Unknown Medication",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                // Display taken/not taken status
                if (isTaken) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Taken",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Taken",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Not Taken",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onTakeMedication(
                                    schedule.medicationId,
                                    schedule.id,
                                    schedule.time,
                                    date
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Take",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Date and time information
            Text(
                text = "Date: $date",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Time: ${schedule.time}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Optional description
            if (medication?.description?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = medication.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // Display reminder information if available
            if (hasReminder && reminder != null) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp),
                    thickness = DividerDefaults.Thickness, 
                    color = Color.Gray.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reminder: ${reminder.scheduledTime}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    // Display reminder status
                    Text(
                        text = if (reminder.acknowledged) "Acknowledged" else "Pending",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (reminder.acknowledged) 
                            MaterialTheme.colorScheme.primary 
                        else 
                            MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
