package gy.roach.json.medminder

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"

    /**
     * No-op implementation for desktop platforms.
     */
    override fun setBadgeCount(count: Int) {
        // Desktop platforms don't support badge notifications
    }
}

actual fun getPlatform(): Platform = JVMPlatform()
