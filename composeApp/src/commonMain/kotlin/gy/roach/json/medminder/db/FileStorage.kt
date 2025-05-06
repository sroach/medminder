package gy.roach.json.medminder.db

import gy.roach.json.medminder.Platform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Interface for file-based storage operations.
 * This replaces the SQLDelight database with a simple file-based storage solution.
 */
interface FileStorage {
    suspend fun readTextFile(fileName: String): String?
    suspend fun writeTextFile(fileName: String, content: String)
    fun getFilePath(fileName: String): String
}

/**
 * In-memory implementation of FileStorage for testing and platforms where file access is difficult.
 * This implementation stores data in memory and doesn't persist it between app restarts.
 */
class InMemoryFileStorage : FileStorage {
    private val fileMap = mutableMapOf<String, String>()

    override suspend fun readTextFile(fileName: String): String? {
        return fileMap[fileName]
    }

    override suspend fun writeTextFile(fileName: String, content: String) {
        fileMap[fileName] = content
    }

    override fun getFilePath(fileName: String): String {
        return "memory://$fileName"
    }
}

/**
 * Factory function to create the appropriate FileStorage implementation for the current platform.
 * This is implemented differently on each platform.
 */
expect fun createFileStorage(): FileStorage

/**
 * Serializable data class for Medication
 */
@Serializable
data class MedicationData(
    val id: Long,
    val name: String,
    val description: String?,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * Serializable data class for MedicationSchedule
 */
@Serializable
data class MedicationScheduleData(
    val id: Long,
    val medicationId: Long,
    val time: String, // Format: HH:MM in 24-hour format
    val daysOfWeek: String, // Comma-separated list of days (1-7, where 1 is Monday)
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Converts the time string to a LocalDateTime for the given date.
     * @param date The date to use for the LocalDateTime, in format YYYY-MM-DD
     * @return LocalDateTime object representing the schedule time on the given date
     */
    fun toLocalDateTime(date: String): LocalDateTime {
        try {
            // Parse the date
            val dateParts = date.split("-")
            if (dateParts.size != 3) {
                throw IllegalArgumentException("Invalid date format: $date")
            }

            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt()
            val day = dateParts[2].toInt()
            val localDate = LocalDate(year, month, day)

            // Parse the time
            val timeParts = time.split(":")
            if (timeParts.size != 2) {
                throw IllegalArgumentException("Invalid time format: $time")
            }

            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()
            val localTime = LocalTime(hour, minute)

            // Combine into LocalDateTime
            return LocalDateTime(localDate, localTime)
        } catch (e: Exception) {
            throw IllegalArgumentException("Error parsing date or time: ${e.message}")
        }
    }
}

/**
 * Serializable data class for MedicationIntake
 */
@Serializable
data class MedicationIntakeData(
    val id: Long,
    val medicationId: Long,
    val scheduleId: Long,
    val takenAt: Long,
    val scheduledTime: String, // The originally scheduled time (HH:MM)
    val scheduledDate: String, // The date for which this intake was scheduled (YYYY-MM-DD)
    val acknowledged: Boolean = false, // Whether the user has acknowledged taking this medication
    val taken: Boolean = true // Whether the medication was actually taken
)

/**
 * Serializable data class for MedicationReminder
 */
@Serializable
data class MedicationReminderData(
    val id: Long,
    val medicationId: Long,
    val scheduleId: Long,
    val reminderTime: Long, // When the reminder should be shown (epoch seconds)
    val scheduledTime: String, // The originally scheduled time (HH:MM)
    val scheduledDate: String, // The date for which this reminder is scheduled (YYYY-MM-DD)
    val acknowledged: Boolean = false // Whether the user has acknowledged this reminder
)

/**
 * Repository for managing medications, schedules, and intakes using file storage.
 */
class MedicationRepository(
    private val fileStorage: FileStorage,
    private val platform: Platform? = null,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val json = Json { prettyPrint = true }
    private val medicationsFileName = "medications.json"
    private val schedulesFileName = "medication_schedules.json"
    private val intakesFileName = "medication_intakes.json"
    private val remindersFileName = "medication_reminders.json"

    private val _medications = MutableStateFlow<List<MedicationData>>(emptyList())
    private val _schedules = MutableStateFlow<List<MedicationScheduleData>>(emptyList())
    private val _intakes = MutableStateFlow<List<MedicationIntakeData>>(emptyList())
    private val _reminders = MutableStateFlow<List<MedicationReminderData>>(emptyList())

    init {
        // Load data from files when repository is created
        coroutineScope.launch {
            loadMedications()
            loadSchedules()
            loadIntakes()
            loadReminders()
        }
    }

    /**
     * Load medications from file
     */
    private suspend fun loadMedications() {
        val content = fileStorage.readTextFile(medicationsFileName)
        if (content != null) {
            try {
                val medications = json.decodeFromString<List<MedicationData>>(content)
                _medications.value = medications
            } catch (e: Exception) {
                // If there's an error decoding the file, start with an empty list
                _medications.value = emptyList()
            }
        } else {
            // If the file doesn't exist, start with an empty list
            _medications.value = emptyList()
        }
    }

    /**
     * Load schedules from file
     */
    private suspend fun loadSchedules() {
        val content = fileStorage.readTextFile(schedulesFileName)
        if (content != null) {
            try {
                val schedules = json.decodeFromString<List<MedicationScheduleData>>(content)
                _schedules.value = schedules
            } catch (e: Exception) {
                _schedules.value = emptyList()
            }
        } else {
            _schedules.value = emptyList()
        }
    }

    /**
     * Load intakes from file
     */
    private suspend fun loadIntakes() {
        val content = fileStorage.readTextFile(intakesFileName)
        if (content != null) {
            try {
                val intakes = json.decodeFromString<List<MedicationIntakeData>>(content)
                _intakes.value = intakes
            } catch (e: Exception) {
                _intakes.value = emptyList()
            }
        } else {
            _intakes.value = emptyList()
        }
    }

    /**
     * Save medications to file
     */
    private suspend fun saveMedications() {
        val content = json.encodeToString(_medications.value)
        fileStorage.writeTextFile(medicationsFileName, content)
    }

    /**
     * Save schedules to file
     */
    private suspend fun saveSchedules() {
        val content = json.encodeToString(_schedules.value)
        fileStorage.writeTextFile(schedulesFileName, content)
    }

    /**
     * Save intakes to file
     */
    private suspend fun saveIntakes() {
        val content = json.encodeToString(_intakes.value)
        fileStorage.writeTextFile(intakesFileName, content)
    }

    /**
     * Load reminders from file
     */
    private suspend fun loadReminders() {
        val content = fileStorage.readTextFile(remindersFileName)
        if (content != null) {
            try {
                val reminders = json.decodeFromString<List<MedicationReminderData>>(content)
                _reminders.value = reminders
            } catch (e: Exception) {
                _reminders.value = emptyList()
            }
        } else {
            _reminders.value = emptyList()
        }
    }

    /**
     * Save reminders to file
     */
    private suspend fun saveReminders() {
        val content = json.encodeToString(_reminders.value)
        fileStorage.writeTextFile(remindersFileName, content)
    }

    // ===== Medication Methods =====

    /**
     * Get all medications as a Flow, sorted by their schedule time.
     */
    fun getAllMedications(): Flow<List<MedicationData>> {
        // Return the medications flow and map it to sort by schedule time
        // This ensures that when _medications changes, the flow will emit the new sorted list
        return _medications.asStateFlow().map { medications ->
            medications.sortedBy { medication ->
                val schedules = _schedules.value.filter { it.medicationId == medication.id }
                if (schedules.isEmpty()) {
                    // If no schedules, put at the end
                    "99:99"
                } else {
                    // Get the earliest time
                    schedules.minOfOrNull { it.time } ?: "99:99"
                }
            }
        }
    }

    /**
     * Get a medication by ID.
     */
    suspend fun getMedicationById(id: Long): MedicationData? {
        return withContext(Dispatchers.Default) {
            _medications.value.find { it.id == id }
        }
    }

    /**
     * Insert a new medication.
     */
    suspend fun insertMedication(name: String, description: String?): Long {
        return withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val newId = if (_medications.value.isEmpty()) 1L else _medications.value.maxOf { it.id } + 1

            val newMedication = MedicationData(
                id = newId,
                name = name,
                description = description,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            _medications.value = _medications.value + newMedication
            saveMedications()

            newId
        }
    }

    /**
     * Update an existing medication.
     */
    suspend fun updateMedication(id: Long, name: String, description: String?) {
        withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val updatedMedications = _medications.value.map {
                if (it.id == id) {
                    it.copy(
                        name = name,
                        description = description,
                        updatedAt = currentTime
                    )
                } else {
                    it
                }
            }

            _medications.value = updatedMedications
            saveMedications()
        }
    }

    /**
     * Delete a medication.
     */
    suspend fun deleteMedication(id: Long) {
        withContext(Dispatchers.Default) {
            _medications.value = _medications.value.filter { it.id != id }

            // Also delete associated schedules and intakes
            _schedules.value = _schedules.value.filter { it.medicationId != id }
            _intakes.value = _intakes.value.filter { it.medicationId != id }

            saveMedications()
            saveSchedules()
            saveIntakes()
        }
    }

    // ===== Schedule Methods =====

    /**
     * Get all schedules as a Flow, sorted by their time.
     */
    fun getAllSchedules(): Flow<List<MedicationScheduleData>> {
        // Return a flow that maps the _schedules flow to a sorted list
        return _schedules.asStateFlow().map { schedules ->
            schedules.sortedBy { it.time }
        }
    }

    /**
     * Get schedules for a specific medication (non-Flow version), sorted by their time.
     */
    suspend fun getSchedulesForMedicationSync(medicationId: Long): List<MedicationScheduleData> {
        return withContext(Dispatchers.Default) {
            _schedules.value
                .filter { it.medicationId == medicationId }
                .sortedBy { it.time } // Sort by time
        }
    }

    /**
     * Get a schedule by ID.
     */
    suspend fun getScheduleById(id: Long): MedicationScheduleData? {
        return withContext(Dispatchers.Default) {
            _schedules.value.find { it.id == id }
        }
    }

    /**
     * Insert a new schedule and automatically create a reminder for today.
     */
    suspend fun insertSchedule(medicationId: Long, time: String, daysOfWeek: String): Long {
        return withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val newId = if (_schedules.value.isEmpty()) 1L else _schedules.value.maxOf { it.id } + 1

            val newSchedule = MedicationScheduleData(
                id = newId,
                medicationId = medicationId,
                time = time,
                daysOfWeek = daysOfWeek,
                createdAt = currentTime,
                updatedAt = currentTime
            )

            _schedules.value = _schedules.value + newSchedule
            saveSchedules()

            // Automatically create a reminder for today
            val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
            val todayString = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-${today.dayOfMonth.toString().padStart(2, '0')}"

            // Check if today's day of week is in the schedule's days of week
            val dayOfWeek = (today.dayOfWeek.ordinal + 1) // Convert to 1-7 format where 1 is Monday
            if (daysOfWeek.split(",").map { it.trim().toInt() }.contains(dayOfWeek)) {
                createReminder(
                    medicationId = medicationId,
                    scheduleId = newId,
                    scheduledTime = time,
                    scheduledDate = todayString
                )
            }

            // Schedule notifications for iOS
            if (platform != null) {
                // Get the medication name
                val medication = _medications.value.find { it.id == medicationId }
                if (medication != null) {
                    // Parse the time
                    val timeParts = time.split(":")
                    if (timeParts.size == 2) {
                        val hour = timeParts[0].toInt()
                        val minute = timeParts[1].toInt()

                        // Parse the days of week
                        val daysOfWeekList = daysOfWeek.split(",").map { it.trim().toInt() }

                        // Schedule the notification
                        platform.scheduleMedicationNotification(
                            medicationName = medication.name,
                            hour = hour,
                            minute = minute,
                            daysOfWeek = daysOfWeekList
                        )
                    }
                }
            }

            newId
        }
    }

    /**
     * Update an existing schedule and automatically create a reminder for today if applicable.
     */
    suspend fun updateSchedule(id: Long, time: String, daysOfWeek: String) {
        withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000

            // Find the schedule to get the medication ID
            val schedule = _schedules.value.find { it.id == id }
            val medicationId = schedule?.medicationId

            val updatedSchedules = _schedules.value.map {
                if (it.id == id) {
                    it.copy(
                        time = time,
                        daysOfWeek = daysOfWeek,
                        updatedAt = currentTime
                    )
                } else {
                    it
                }
            }

            _schedules.value = updatedSchedules
            saveSchedules()

            // Automatically create a reminder for today if the medication ID is available
            if (medicationId != null) {
                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                val todayString = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-${today.dayOfMonth.toString().padStart(2, '0')}"

                // Check if today's day of week is in the schedule's days of week
                val dayOfWeek = (today.dayOfWeek.ordinal + 1) // Convert to 1-7 format where 1 is Monday
                if (daysOfWeek.split(",").map { it.trim().toInt() }.contains(dayOfWeek)) {
                    createReminder(
                        medicationId = medicationId,
                        scheduleId = id,
                        scheduledTime = time,
                        scheduledDate = todayString
                    )
                }

                // Schedule notifications for iOS
                if (platform != null) {
                    // Get the medication name
                    val medication = _medications.value.find { it.id == medicationId }
                    if (medication != null) {
                        // Parse the time
                        val timeParts = time.split(":")
                        if (timeParts.size == 2) {
                            val hour = timeParts[0].toInt()
                            val minute = timeParts[1].toInt()

                            // Parse the days of week
                            val daysOfWeekList = daysOfWeek.split(",").map { it.trim().toInt() }

                            // Schedule the notification
                            platform.scheduleMedicationNotification(
                                medicationName = medication.name,
                                hour = hour,
                                minute = minute,
                                daysOfWeek = daysOfWeekList
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete a schedule.
     */
    suspend fun deleteSchedule(id: Long) {
        withContext(Dispatchers.Default) {
            _schedules.value = _schedules.value.filter { it.id != id }

            // Also delete associated intakes
            _intakes.value = _intakes.value.filter { it.scheduleId != id }

            saveSchedules()
            saveIntakes()
        }
    }

    // ===== Intake Methods =====

    /**
     * Get all intakes as a Flow, sorted by their scheduled time.
     */
    fun getAllIntakes(): Flow<List<MedicationIntakeData>> {
        // Return a flow that maps the _intakes flow to a sorted list
        return _intakes.asStateFlow().map { intakes ->
            intakes.sortedBy { it.scheduledTime }
        }
    }

    /**
     * Get intakes for a specific date range (non-Flow version), sorted by their scheduled time.
     */
    suspend fun getIntakesForDateRangeSync(startDate: String, endDate: String): List<MedicationIntakeData> {
        return withContext(Dispatchers.Default) {
            _intakes.value
                .filter {
                    it.scheduledDate >= startDate && it.scheduledDate <= endDate
                }
                .sortedBy { it.scheduledTime } // Sort by scheduled time
        }
    }

    /**
     * Check if a medication has been taken for a specific schedule and date.
     */
    suspend fun getIntakesForScheduleAndDateSync(scheduleId: Long, date: String): List<MedicationIntakeData> {
        return withContext(Dispatchers.Default) {
            _intakes.value
                .filter {
                    it.scheduleId == scheduleId && it.scheduledDate == date
                }
                .sortedBy { it.scheduledTime }
        }
    }

    /**
     * Record a medication intake.
     * @param taken Whether the medication was actually taken (defaults to true)
     */
    suspend fun recordIntake(medicationId: Long, scheduleId: Long, scheduledTime: String, scheduledDate: String, taken: Boolean = true): Long {
        return withContext(Dispatchers.Default) {
            val takenAt = Clock.System.now().toEpochMilliseconds() / 1000
            val newId = if (_intakes.value.isEmpty()) 1L else _intakes.value.maxOf { it.id } + 1

            val newIntake = MedicationIntakeData(
                id = newId,
                medicationId = medicationId,
                scheduleId = scheduleId,
                takenAt = takenAt,
                scheduledTime = scheduledTime,
                scheduledDate = scheduledDate,
                taken = taken
            )

            _intakes.value = _intakes.value + newIntake
            saveIntakes()

            newId
        }
    }

    /**
     * Delete an intake record.
     */
    suspend fun deleteIntake(id: Long) {
        withContext(Dispatchers.Default) {
            _intakes.value = _intakes.value.filter { it.id != id }
            saveIntakes()
        }
    }

    // ===== Reminder Methods =====

    /**
     * Get all reminders as a Flow, sorted by their schedule time.
     * Filters out reminders that are more than a day old.
     */
    fun getAllReminders(): Flow<List<MedicationReminderData>> {
        // Return a flow that maps the _reminders flow to a filtered and sorted list
        return _reminders.asStateFlow().map { reminders ->
            // Get current time and calculate one day ago
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val oneDayAgo = currentTime - (24 * 60 * 60) // 24 hours in seconds

            // Filter out reminders more than a day old and sort by scheduled time
            reminders
                .filter { it.reminderTime > oneDayAgo }
                .sortedBy { it.scheduledTime }
        }
    }

    /**
     * Get active (unacknowledged) reminders for the current time, sorted by their schedule time.
     * Filters out reminders that are more than a day old.
     */
    suspend fun getActiveRemindersSync(): List<MedicationReminderData> {
        return withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val oneDayAgo = currentTime - (24 * 60 * 60) // 24 hours in seconds
            _reminders.value
                .filter {
                    !it.acknowledged && it.reminderTime <= currentTime && it.reminderTime > oneDayAgo
                }
                .sortedBy { it.scheduledTime } // Sort by scheduled time
        }
    }

    /**
     * Get all reminders for the current time (both acknowledged and unacknowledged), sorted by their schedule time.
     * Filters out reminders that are more than a day old.
     */
    suspend fun getAllRemindersForCurrentTimeSync(): List<MedicationReminderData> {
        return withContext(Dispatchers.Default) {
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000
            val oneDayAgo = currentTime - (24 * 60 * 60) // 24 hours in seconds
            _reminders.value
                .filter {
                    it.reminderTime <= currentTime && it.reminderTime > oneDayAgo
                }
                .sortedBy { it.scheduledTime } // Sort by scheduled time
        }
    }

    /**
     * Get reminders for a specific date range (non-Flow version), sorted by their schedule time.
     */
    suspend fun getRemindersForDateRangeSync(startDate: String, endDate: String): List<MedicationReminderData> {
        return withContext(Dispatchers.Default) {
            _reminders.value
                .filter {
                    it.scheduledDate >= startDate && it.scheduledDate <= endDate
                }
                .sortedBy { it.scheduledTime } // Sort by scheduled time
        }
    }

    /**
     * Create a reminder for a scheduled medication.
     * The reminder will be set for 5 minutes before the scheduled time.
     * If a duplicate reminder already exists, returns the ID of the existing reminder.
     */
    suspend fun createReminder(medicationId: Long, scheduleId: Long, scheduledTime: String, scheduledDate: String): Long {
        return withContext(Dispatchers.Default) {
            // Check if a duplicate reminder already exists
            val existingReminder = _reminders.value.find {
                it.medicationId == medicationId &&
                it.scheduleId == scheduleId &&
                it.scheduledTime == scheduledTime &&
                it.scheduledDate == scheduledDate &&
                !it.acknowledged
            }

            // If a duplicate exists, return its ID
            if (existingReminder != null) {
                return@withContext existingReminder.id
            }

            // Get the current time as a fallback
            val currentTime = Clock.System.now().toEpochMilliseconds() / 1000

            // Calculate the reminder time (5 minutes before the scheduled time)
            val reminderEpoch = try {
                // Get the schedule to use its toLocalDateTime method
                val schedule = getScheduleById(scheduleId)

                if (schedule != null) {
                    // Use the schedule's toLocalDateTime method to get a complete date time object
                    val localDateTime = schedule.toLocalDateTime(scheduledDate)

                    // Convert to Instant (epoch time)
                    val instant = localDateTime.toInstant(TimeZone.currentSystemDefault())
                    val scheduledEpoch = instant.toEpochMilliseconds() / 1000

                    // Calculate reminder time (5 minutes = 300 seconds before)
                    val calculatedReminderEpoch = scheduledEpoch - 300

                    // If the reminder time is in the past, use the current time
                    if (calculatedReminderEpoch < currentTime) {
                        currentTime
                    } else {
                        calculatedReminderEpoch
                    }
                } else {
                    // If the schedule doesn't exist, fall back to the old method
                    // Parse the scheduled date and time
                    val dateParts = scheduledDate.split("-")
                    val timeParts = scheduledTime.split(":")

                    if (dateParts.size != 3 || timeParts.size != 2) {
                        // If the date or time format is invalid, use the current time as a fallback
                        currentTime
                    } else {
                        // Create LocalDate and LocalTime objects
                        val year = dateParts[0].toInt()
                        val month = dateParts[1].toInt()
                        val day = dateParts[2].toInt()
                        val hour = timeParts[0].toInt()
                        val minute = timeParts[1].toInt()

                        val localDate = LocalDate(year, month, day)
                        val localTime = LocalTime(hour, minute)

                        // Combine into LocalDateTime
                        val localDateTime = LocalDateTime(localDate, localTime)

                        // Convert to Instant (epoch time)
                        val instant = localDateTime.toInstant(TimeZone.currentSystemDefault())
                        val scheduledEpoch = instant.toEpochMilliseconds() / 1000

                        // Calculate reminder time (5 minutes = 300 seconds before)
                        val calculatedReminderEpoch = scheduledEpoch - 300

                        // If the reminder time is in the past, use the current time
                        if (calculatedReminderEpoch < currentTime) {
                            currentTime
                        } else {
                            calculatedReminderEpoch
                        }
                    }
                }
            } catch (e: Exception) {
                // If there's an error parsing the date or time, use the current time as a fallback
                currentTime
            }

            val newId = if (_reminders.value.isEmpty()) 1L else _reminders.value.maxOf { it.id } + 1

            val newReminder = MedicationReminderData(
                id = newId,
                medicationId = medicationId,
                scheduleId = scheduleId,
                reminderTime = reminderEpoch,
                scheduledTime = scheduledTime,
                scheduledDate = scheduledDate,
                acknowledged = false
            )

            _reminders.value = _reminders.value + newReminder
            saveReminders()

            newId
        }
    }

    /**
     * Acknowledge a reminder.
     */
    suspend fun acknowledgeReminder(id: Long) {
        withContext(Dispatchers.Default) {
            val updatedReminders = _reminders.value.map {
                if (it.id == id) {
                    it.copy(acknowledged = true)
                } else {
                    it
                }
            }

            _reminders.value = updatedReminders
            saveReminders()
        }
    }

    /**
     * Delete a reminder.
     */
    suspend fun deleteReminder(id: Long) {
        withContext(Dispatchers.Default) {
            _reminders.value = _reminders.value.filter { it.id != id }
            saveReminders()
        }
    }

    /**
     * Acknowledge an intake.
     */
    suspend fun acknowledgeIntake(id: Long) {
        withContext(Dispatchers.Default) {
            val updatedIntakes = _intakes.value.map {
                if (it.id == id) {
                    it.copy(acknowledged = true)
                } else {
                    it
                }
            }

            _intakes.value = updatedIntakes
            saveIntakes()
        }
    }

    /**
     * Count medications not taken for today where the current time is at least 10 minutes
     * after the scheduled time.
     * This is used for badge notifications on iOS.
     * @return The number of medications not taken for today that are overdue by at least 10 minutes.
     */
    suspend fun countMedicationsNotTakenForToday(): Int {
        return withContext(Dispatchers.Default) {
            // Get current time in epoch seconds
            val now = Clock.System.now()
            val currentEpochSeconds = now.toEpochMilliseconds() / 1000

            // Get today's date and time
            val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val today = currentDateTime.date
            val todayString = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-${today.dayOfMonth.toString().padStart(2, '0')}"

            // Get today's day of week (1-7, where 1 is Monday)
            val dayOfWeek = (today.dayOfWeek.ordinal + 1)

            // Get all schedules for today
            val schedulesForToday = _schedules.value.filter { schedule ->
                schedule.daysOfWeek.split(",").map { it.trim().toInt() }.contains(dayOfWeek)
            }

            // Get all intakes for today
            val intakesForToday = _intakes.value.filter { it.scheduledDate == todayString }

            // Count schedules that don't have a corresponding intake and are at least 10 minutes past due
            var count = 0
            for (schedule in schedulesForToday) {
                val hasIntake = intakesForToday.any { 
                    it.scheduleId == schedule.id && it.scheduledDate == todayString 
                }

                if (!hasIntake) {
                    try {
                        // Convert schedule time to epoch seconds
                        val scheduledDateTime = schedule.toLocalDateTime(todayString)
                        val scheduledInstant = scheduledDateTime.toInstant(TimeZone.currentSystemDefault())
                        val scheduledEpochSeconds = scheduledInstant.toEpochMilliseconds() / 1000

                        // Add 10 minutes (600 seconds) to the scheduled time
                        val tenMinutesAfterScheduleEpoch = scheduledEpochSeconds + 600

                        // Only count if current time is at least 10 minutes after scheduled time
                        if (currentEpochSeconds >= tenMinutesAfterScheduleEpoch) {
                            count++
                        }
                    } catch (e: Exception) {
                        // If there's an error parsing the time, skip this schedule
                    }
                }
            }
            count
        }
    }

    /**
     * Get medications not taken for today where the current time is at least 10 minutes
     * after the scheduled time.
     * This is used for desktop notifications.
     * @return A list of medication data for medications not taken for today that are overdue by at least 10 minutes.
     */
    suspend fun getMedicationsNotTakenForToday(): List<Pair<MedicationData, MedicationScheduleData>> {
        return withContext(Dispatchers.Default) {
            // Get current time in epoch seconds
            val now = Clock.System.now()
            val currentEpochSeconds = now.toEpochMilliseconds() / 1000

            // Get today's date and time
            val currentDateTime = now.toLocalDateTime(TimeZone.currentSystemDefault())
            val today = currentDateTime.date
            val todayString = "${today.year}-${today.monthNumber.toString().padStart(2, '0')}-${today.dayOfMonth.toString().padStart(2, '0')}"

            // Get today's day of week (1-7, where 1 is Monday)
            val dayOfWeek = (today.dayOfWeek.ordinal + 1)

            // Get all schedules for today
            val schedulesForToday = _schedules.value.filter { schedule ->
                schedule.daysOfWeek.split(",").map { it.trim().toInt() }.contains(dayOfWeek)
            }

            // Get all intakes for today
            val intakesForToday = _intakes.value.filter { it.scheduledDate == todayString }

            // Get medications not taken for today
            val medicationsNotTaken = mutableListOf<Pair<MedicationData, MedicationScheduleData>>()

            for (schedule in schedulesForToday) {
                val hasIntake = intakesForToday.any { 
                    it.scheduleId == schedule.id && it.scheduledDate == todayString 
                }

                if (!hasIntake) {
                    try {
                        // Convert schedule time to epoch seconds
                        val scheduledDateTime = schedule.toLocalDateTime(todayString)
                        val scheduledInstant = scheduledDateTime.toInstant(TimeZone.currentSystemDefault())
                        val scheduledEpochSeconds = scheduledInstant.toEpochMilliseconds() / 1000

                        // Add 10 minutes (600 seconds) to the scheduled time
                        val tenMinutesAfterScheduleEpoch = scheduledEpochSeconds + 600

                        // Only include if current time is at least 10 minutes after scheduled time
                        if (currentEpochSeconds >= tenMinutesAfterScheduleEpoch) {
                            // Find the medication for this schedule
                            val medication = _medications.value.find { it.id == schedule.medicationId }
                            if (medication != null) {
                                medicationsNotTaken.add(Pair(medication, schedule))
                            }
                        }
                    } catch (e: Exception) {
                        // If there's an error parsing the time, skip this schedule
                    }
                }
            }

            medicationsNotTaken
        }
    }
}
