package gy.roach.json.medminder.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Desktop implementation of FileStorage using java.io.File
 */
class DesktopFileStorage : FileStorage {
    private val appDir = System.getProperty("user.home") + File.separator + ".medminder"

    init {
        // Create app directory if it doesn't exist
        val dir = File(appDir)
        if (!dir.exists()) {
            dir.mkdirs()
        }
    }

    /**
     * Read text from a file
     */
    override suspend fun readTextFile(fileName: String): String? {
        return withContext(Dispatchers.IO) {
            val file = File(getFilePath(fileName))
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        }
    }

    /**
     * Write text to a file
     */
    override suspend fun writeTextFile(fileName: String, content: String) {
        withContext(Dispatchers.IO) {
            val file = File(getFilePath(fileName))
            file.writeText(content)
        }
    }

    /**
     * Get the full path to a file
     */
    override fun getFilePath(fileName: String): String {
        return appDir + File.separator + fileName
    }
}

/**
 * Factory function to create the appropriate FileStorage implementation for the current platform
 */
actual fun createFileStorage(): FileStorage {
    return DesktopFileStorage()
}
