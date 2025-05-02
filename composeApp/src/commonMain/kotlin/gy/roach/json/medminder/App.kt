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
    val repository = remember { MedicationRepository(fileStorage) }
    val themeRepository = remember { ThemeRepository(fileStorage) }
    val platform = remember { getPlatform() }

    // State for navigation
    var selectedTab by remember { mutableStateOf(0) }

    // Theme state
    val themePreferences by themeRepository.themePreferences.collectAsState()
    var showThemeDialog by remember { mutableStateOf(false) }

    // Coroutine scope for background tasks
    val scope = rememberCoroutineScope()

    // Update badge count when app starts
    LaunchedEffect(Unit) {
        updateBadgeCount(repository, platform)
    }

    // Update badge count when intakes change
    val intakes by repository.getAllIntakes().collectAsState(initial = emptyList())
    LaunchedEffect(intakes) {
        updateBadgeCount(repository, platform)
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
    val useDarkTheme = shouldUseDarkTheme(themeMode)
    val iosTopBarColor = if (useDarkTheme) {
        Color(0xFF1C1C1E).copy(alpha = 0.9f) // Dark mode
    } else {
        Color(0xFFF8F8F8).copy(alpha = 0.9f) // Light mode
    }
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "MedMinder",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        actions = {
            // Theme toggle button
            IconButton(onClick = onThemeClick) {
                Icon(
                    imageVector = Icons.Outlined.DarkMode,
                    contentDescription = "Theme Settings",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = iosTopBarColor,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
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
