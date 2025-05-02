package gy.roach.json.medminder.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

/**
 * iOS implementation of FileStorage that uses the app's Documents directory for persistent storage.
 * This follows iOS best practices for storing user data.
 */
class IOSFileStorage : FileStorage {
    private val fileManager = NSFileManager.defaultManager
    private val documentsDirectory: String = NSSearchPathForDirectoriesInDomains(
        NSDocumentDirectory, NSUserDomainMask, true
    ).firstOrNull() as? String ?: ""

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun readTextFile(fileName: String): String? {
        val filePath = getFilePath(fileName)
        return try {
            NSString.stringWithContentsOfFile(filePath, NSUTF8StringEncoding, null)
        } catch (e: Exception) {
            null
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun writeTextFile(fileName: String, content: String) {
        val filePath = getFilePath(fileName)
        try {
            content.toNSString().writeToFile(filePath, true, NSUTF8StringEncoding, null)
        } catch (e: Exception) {
            // Handle error (could log or throw)
            println("Error writing to file: $e")
        }
    }

    override fun getFilePath(fileName: String): String {
        return "$documentsDirectory/$fileName"
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun String.toNSString(): NSString {
        return NSString.create(string = this)
    }
}

/**
 * Factory function to create the appropriate FileStorage implementation for iOS platform.
 * For iOS, we use the IOSFileStorage implementation to persist data on disk.
 */
actual fun createFileStorage(): FileStorage {
    return IOSFileStorage()
}
