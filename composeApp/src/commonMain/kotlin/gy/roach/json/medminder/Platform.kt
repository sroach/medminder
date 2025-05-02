package gy.roach.json.medminder

interface Platform {
    val name: String

    /**
     * Sets the badge count on the app icon.
     * This is only implemented on iOS.
     * @param count The number to display on the badge. If 0, the badge will be hidden.
     */
    fun setBadgeCount(count: Int)
}

expect fun getPlatform(): Platform
