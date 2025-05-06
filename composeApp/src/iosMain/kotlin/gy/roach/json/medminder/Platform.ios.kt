package gy.roach.json.medminder

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication
import platform.UserNotifications.*
import platform.Foundation.NSDate
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSDateComponents
import platform.Foundation.timeIntervalSince1970

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    // Counter for generating unique notification identifiers
    private var notificationCounter = 0

    init {
        // Request notification permissions
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert + UNAuthorizationOptionSound + UNAuthorizationOptionBadge
        ) { granted, error ->
            if (granted) {
                println("Notification permission granted")
            } else {
                println("Notification permission denied: ${error?.localizedDescription}")
            }
        }

        // Register for background updates
        center.setNotificationCategories(setOf<UNNotificationCategory>())
    }

    /**
     * Sets the badge count on the app icon.
     * @param count The number to display on the badge. If 0, the badge will be hidden.
     */
    override fun setBadgeCount(count: Int) {
        UIApplication.sharedApplication.applicationIconBadgeNumber = count.toLong()

        // If count > 0, trigger a notification to remind the user to take medication
        if (count > 0) {
            sendMedicationReminder()

            // Schedule notifications for the next 24 hours
            scheduleNotificationsForNextDay()
        }
    }

    /**
     * Sends a notification to remind the user to take their medication.
     */
    private fun sendMedicationReminder() {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Create notification content
        val content = UNMutableNotificationContent().apply {
            setTitle("Medication Reminder")
            setBody("It's time to take your medication.")
            setSound(UNNotificationSound.defaultSound)
            // Set badge number
            // Note: The badge number is already set at the application level
        }

        // Create a trigger for immediate delivery
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(1.0, false)

        // Create a request with a unique identifier using a counter
        val timestamp = NSDate().timeIntervalSince1970.toLong()
        val requestIdentifier = "medication.reminder.${timestamp}.${notificationCounter++}"
        val request = UNNotificationRequest.requestWithIdentifier(
            requestIdentifier,
            content,
            trigger
        )

        // Add the request to the notification center
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error sending notification: ${error.localizedDescription}")
            }
        }
    }

    /**
     * Schedules notifications for the next 24 hours to ensure the app can notify
     * the user even when it's closed or the phone is sleeping.
     */
    private fun scheduleNotificationsForNextDay() {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // First, remove any pending notifications
        center.removeAllPendingNotificationRequests()

        // Schedule notifications at regular intervals for the next 24 hours
        // This ensures the app will check for medications even when closed
        for (hour in 0..23) {
            for (minute in listOf(0, 30)) { // Schedule every 30 minutes
                scheduleBackgroundCheck(hour, minute)
            }
        }
    }

    /**
     * Schedules a background check at a specific hour and minute.
     */
    private fun scheduleBackgroundCheck(hour: Int, minute: Int) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Create a date components object for the specified time
        val calendar = NSCalendar.currentCalendar
        val dateComponents = NSDateComponents().apply {
            setHour(hour.toLong())
            setMinute(minute.toLong())
        }

        // Create a silent notification (no alert, sound, or badge)
        val content = UNMutableNotificationContent().apply {
            // Set a category identifier to handle the notification in the background
            setCategoryIdentifier("MEDICATION_CHECK")
            // Make it a silent notification
            setSound(null)
        }

        // Create a calendar trigger
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents,
            true // Repeats daily
        )

        // Create a unique identifier for this notification
        val requestIdentifier = "medication.check.${hour}.${minute}"

        // Create the notification request
        val request = UNNotificationRequest.requestWithIdentifier(
            requestIdentifier,
            content,
            trigger
        )

        // Add the request to the notification center
        center.addNotificationRequest(request) { error ->
            if (error != null) {
                println("Error scheduling background check: ${error.localizedDescription}")
            }
        }
    }

    /**
     * Schedules a notification for a specific medication at a specific time.
     * This can be called when a new medication schedule is created.
     */
    override fun scheduleMedicationNotification(
        medicationName: String,
        hour: Int,
        minute: Int,
        daysOfWeek: List<Int>
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        // Create notification content
        val content = UNMutableNotificationContent().apply {
            setTitle("Time to take $medicationName")
            setBody("Don't forget to take your medication.")
            setSound(UNNotificationSound.defaultSound)
            // Update the badge count
            // Note: The badge number is managed at the application level
        }

        // Schedule for each day of the week
        for (dayOfWeek in daysOfWeek) {
            // Create a date components object for the specified time and day of week
            val dateComponents = NSDateComponents().apply {
                setHour(hour.toLong())
                setMinute(minute.toLong())
                setWeekday(dayOfWeek.toLong())
            }

            // Create a calendar trigger
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents,
                true // Repeats weekly
            )

            // Create a unique identifier for this notification
            val requestIdentifier = "medication.${medicationName}.${dayOfWeek}.${hour}.${minute}"

            // Create the notification request
            val request = UNNotificationRequest.requestWithIdentifier(
                requestIdentifier,
                content,
                trigger
            )

            // Add the request to the notification center
            center.addNotificationRequest(request) { error ->
                if (error != null) {
                    println("Error scheduling medication notification: ${error.localizedDescription}")
                }
            }
        }
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
