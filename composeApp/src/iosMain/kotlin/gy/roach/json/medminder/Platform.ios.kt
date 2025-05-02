package gy.roach.json.medminder

import platform.UIKit.UIDevice
import platform.UIKit.UIApplication

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion

    /**
     * Sets the badge count on the app icon.
     * @param count The number to display on the badge. If 0, the badge will be hidden.
     */
    override fun setBadgeCount(count: Int) {
        UIApplication.sharedApplication.applicationIconBadgeNumber = count.toLong()
    }
}

actual fun getPlatform(): Platform = IOSPlatform()
