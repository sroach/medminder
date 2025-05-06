package gy.roach.json.medminder

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationRepository
import gy.roach.json.medminder.db.createFileStorage
import gy.roach.json.medminder.ui.screens.AboutScreen
import gy.roach.json.medminder.ui.screens.ScheduleCalendarScreen
import gy.roach.json.medminder.ui.screens.TwoColumnScreen
import gy.roach.json.medminder.ui.theme.AppTheme
import gy.roach.json.medminder.ui.theme.ThemeMode
import gy.roach.json.medminder.ui.theme.ThemeRepository
import gy.roach.json.medminder.ui.theme.shouldUseDarkTheme
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Updates the badge count on the app icon with the number of medications not taken for today.
 * This is only implemented on iOS.
 */
private suspend fun updateBadgeCount(repository: MedicationRepository, platform: Platform) {
    val count = repository.countMedicationsNotTakenForToday()
    platform.setBadgeCount(count)
}

/**
 * Main application entry point
 */
@Composable
@Preview
fun App() {
    // Create repositories
    val fileStorage = remember { createFileStorage() }
    val platform = remember { getPlatform() }
    val repository = remember { MedicationRepository(fileStorage, platform) }
    val themeRepository = remember { ThemeRepository(fileStorage) }

    // State for navigation
    var selectedTab by remember { mutableStateOf(0) }

    // Theme state
    val themePreferences by themeRepository.themePreferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    // State for medications not taken dialog (desktop only)
    var showMedicationsNotTakenDialog by remember { mutableStateOf(false) }
    var medicationsNotTaken by remember { mutableStateOf<List<Pair<gy.roach.json.medminder.db.MedicationData, gy.roach.json.medminder.db.MedicationScheduleData>>>(emptyList()) }

    // Coroutine scope for background tasks
    val scope = rememberCoroutineScope()

    // Update badge count when app starts
    LaunchedEffect(Unit) {
        updateBadgeCount(repository, platform)

        // For desktop platform, check for medications not taken and show dialog
        if (platform.name.startsWith("Java")) {
            val notTaken = repository.getMedicationsNotTakenForToday()
            if (notTaken.isNotEmpty()) {
                medicationsNotTaken = notTaken
                showMedicationsNotTakenDialog = true
            }
        }
    }

    // Update badge count when intakes change
    val intakes by repository.getAllIntakes().collectAsState(initial = emptyList())
    LaunchedEffect(intakes) {
        updateBadgeCount(repository, platform)

        // For desktop platform, check for medications not taken and update dialog state
        if (platform.name.startsWith("Java")) {
            val notTaken = repository.getMedicationsNotTakenForToday()
            medicationsNotTaken = notTaken
            // Only show dialog if it's not already showing and there are medications not taken
            if (notTaken.isNotEmpty() && !showMedicationsNotTakenDialog) {
                showMedicationsNotTakenDialog = true
            }
        }
    }

    AppTheme(themeMode = themePreferences.themeMode) {
        Scaffold(
            topBar = { MedMinderTopBar(onThemeClick = { showThemeDialog = true }, themeMode = themePreferences.themeMode) },
            bottomBar = { MedMinderBottomBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it }, themeMode = themePreferences.themeMode) },
            content = { paddingValues ->
                Surface(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (selectedTab) {
                        0 -> TwoColumnScreen(repository)
                        1 -> ScheduleCalendarScreen(repository, onBackClick = {})
                        2 -> AboutScreen()
                        else -> TwoColumnScreen(repository)
                    }
                }
            }
        )

        // Theme selection dialog
        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentThemeMode = themePreferences.themeMode,
                onThemeModeSelected = { themeMode ->
                    scope.launch {
                        themeRepository.setThemeMode(themeMode)
                    }
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }

        // Medications not taken dialog (desktop only)
        if (showMedicationsNotTakenDialog && medicationsNotTaken.isNotEmpty()) {
            MedicationsNotTakenDialog(
                medicationsNotTaken = medicationsNotTaken,
                onDismiss = { showMedicationsNotTakenDialog = false }
            )
        }
    }
}

/**
 * Top bar with iOS look and feel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedMinderTopBar(
    onThemeClick: () -> Unit = {},
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 32.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MedMinder",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = onThemeClick) {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = "Theme Settings",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }

}

/**
 * Bottom navigation bar
 */
@Composable
fun MedMinderBottomBar(
    selectedTab: Int, 
    onTabSelected: (Int) -> Unit,
    themeMode: ThemeMode = ThemeMode.SYSTEM
) {
    val useDarkTheme = shouldUseDarkTheme(themeMode)
    NavigationBar(containerColor = if (useDarkTheme) {
        Color(0xFF1C1C1E).copy(alpha = 0.9f) // Dark mode
    } else {
        Color(0xFFF8F8F8).copy(alpha = 0.9f) // Light mode
    }) {

        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Medications") },
            label = { Text("Medications") },
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
            label = { Text("Schedule") },
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Info, contentDescription = "About") },
            label = { Text("About") },
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}

/**
 * Dialog for selecting theme mode
 */
@Composable
fun ThemeSelectionDialog(
    currentThemeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column {
                // System theme option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onThemeModeSelected(ThemeMode.SYSTEM) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeModeSelected(ThemeMode.SYSTEM) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("System Default")
                }

                // Light theme option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onThemeModeSelected(ThemeMode.LIGHT) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeModeSelected(ThemeMode.LIGHT) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Light")
                }

                // Dark theme option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .clickable { onThemeModeSelected(ThemeMode.DARK) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentThemeMode == ThemeMode.DARK,
                        onClick = { onThemeModeSelected(ThemeMode.DARK) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dark")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Dialog for showing medications not taken for today
 */
@Composable
fun MedicationsNotTakenDialog(
    medicationsNotTaken: List<Pair<gy.roach.json.medminder.db.MedicationData, gy.roach.json.medminder.db.MedicationScheduleData>>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Medications Not Taken Today") },
        text = {
            Column {
                Text(
                    "The following medications are overdue and have not been taken today:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // List of medications not taken
                medicationsNotTaken.forEach { (medication, schedule) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Medication name and dosage
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = medication.name,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (medication.description != null) {
                                Text(
                                    text = "Description: ${medication.description}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Text(
                                text = "Scheduled time: ${schedule.time}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
