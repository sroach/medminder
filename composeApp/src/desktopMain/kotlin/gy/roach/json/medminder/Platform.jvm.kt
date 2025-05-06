package gy.roach.json.medminder

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    /**
     * No-op implementation for desktop platforms.
     */
    override fun setBadgeCount(count: Int) {
        // Desktop platforms don't support badge notifications
    }

    /**
     * No-op implementation for desktop platforms.
     */
    override fun scheduleMedicationNotification(
        medicationName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>
    ) {
        // Desktop platforms handle notifications differently
        // This is primarily for iOS background notifications
    }
}

actual fun getPlatform(): Platform = JVMPlatform()
