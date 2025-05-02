package gy.roach.json.medminder.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import gy.roach.json.medminder.db.FileStorage

/**
 * Enum representing the available theme modes
 */
enum class ThemeMode {
    SYSTEM, // Follow system theme
    LIGHT,  // Always use light theme
    DARK    // Always use dark theme
}

/**
 * Data class to store theme preferences
 */
@Serializable
data class ThemePreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

/**
 * Repository for managing theme preferences
 */
class ThemeRepository(
    private val fileStorage: FileStorage,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val json = Json { prettyPrint = true }
    private val themePreferencesFileName = "theme_preferences.json"
    
    private val _themePreferences = MutableStateFlow(ThemePreferences())
    val themePreferences: StateFlow<ThemePreferences> = _themePreferences.asStateFlow()
    
    init {
        // Load theme preferences when repository is created
        coroutineScope.launch {
            loadThemePreferences()
        }
    }
    
    /**
     * Load theme preferences from file
     */
    private suspend fun loadThemePreferences() {
        val content = fileStorage.readTextFile(themePreferencesFileName)
        if (content != null) {
            try {
                val preferences = json.decodeFromString<ThemePreferences>(content)
                _themePreferences.value = preferences
            } catch (e: Exception) {
                // If there's an error decoding the file, use default preferences
                _themePreferences.value = ThemePreferences()
            }
        } else {
            // If the file doesn't exist, use default preferences
            _themePreferences.value = ThemePreferences()
        }
    }
    
    /**
     * Save theme preferences to file
     */
    private suspend fun saveThemePreferences() {
        val content = json.encodeToString(_themePreferences.value)
        fileStorage.writeTextFile(themePreferencesFileName, content)
    }
    
    /**
     * Update the theme mode
     */
    suspend fun setThemeMode(themeMode: ThemeMode) {
        _themePreferences.value = _themePreferences.value.copy(themeMode = themeMode)
        saveThemePreferences()
    }
}

/**
 * Composable function to get the current theme mode
 */
@Composable
fun rememberThemeMode(themeRepository: ThemeRepository): State<ThemeMode> {
    val themePreferences by themeRepository.themePreferences.collectAsState()
    return remember(themePreferences) { mutableStateOf(themePreferences.themeMode) }
}