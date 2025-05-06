package gy.roach.json.medminder

interface Platform {
    val name: String

    /**
     * Sets the badge count on the app icon.
     * This is only implemented on iOS.
     * @param count The number to display on the badge. If 0, the badge will be hidden.
     */
    fun setBadgeCount(count: Int)

    /**
     * Schedules a notification for a specific medication at a specific time.
     * This is primarily implemented on iOS to ensure notifications work when the app is closed.
     * @param medicationName The name of the medication
     * @param hour The hour of the day (0-23)
     * @param minute The minute of the hour (0-59)
     * @param daysOfWeek List of days of the week (1-7, where 1 is Monday)
     */
    fun scheduleMedicationNotification(
        medicationName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>
    )
}

expect fun getPlatform(): Platform
