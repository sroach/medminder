package gy.roach.json.medminder.pdf

import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationScheduleData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*
import platform.UIKit.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * iOS implementation of PdfService that generates a text file with medication information.
 * This is a simplified approach since direct PDF generation in iOS through Kotlin/Native
 * is more complex.
 */
@OptIn(ExperimentalForeignApi::class)
class IOSPdfService : PdfService {
    override suspend fun generateMedicationPdf(
        medications: List<MedicationData>,
        schedules: Map<Long, List<MedicationScheduleData>>,
        fileName: String
    ): String? = withContext(Dispatchers.Default) {
        try {
            // Create a unique file name with formatted timestamp
            val timestampFormatter = NSDateFormatter()
            timestampFormatter.setDateFormat("yyyyMMdd_HHmmss")
            val timestamp = timestampFormatter.stringFromDate(NSDate())
            val pdfFileName = "$fileName-$timestamp.pdf"

            // Get the documents directory path
            val fileManager = NSFileManager.defaultManager
            val documentsDirectory = NSSearchPathForDirectoriesInDomains(
                NSDocumentDirectory, NSUserDomainMask, true
            ).firstOrNull() as? String ?: return@withContext null

            val filePath = "$documentsDirectory/$pdfFileName"

            // Build the content as a string
            val contentBuilder = StringBuilder()
            contentBuilder.appendLine("MEDICATION REPORT")
            // Format the current date properly
            val dateFormatter = NSDateFormatter()
            dateFormatter.setDateFormat("yyyy-MM-dd HH:mm:ss")
            val formattedDate = dateFormatter.stringFromDate(NSDate())
            contentBuilder.appendLine("Generated on: $formattedDate")
            contentBuilder.appendLine()

            if (medications.isEmpty()) {
                contentBuilder.appendLine("No medications found.")
            } else {
                medications.forEach { medication ->
                    contentBuilder.appendLine("MEDICATION: ${medication.name}")

                    if (!medication.description.isNullOrBlank()) {
                        contentBuilder.appendLine("Description: ${medication.description}")
                    }

                    val medicationSchedules = schedules[medication.id] ?: emptyList()
                    if (medicationSchedules.isEmpty()) {
                        contentBuilder.appendLine("No schedules set for this medication.")
                    } else {
                        contentBuilder.appendLine("Schedules:")

                        medicationSchedules.forEach { schedule ->
                            // Format days of week
                            val daysText = schedule.daysOfWeek.split(",").joinToString(", ") { dayNumber ->
                                when (dayNumber.trim()) {
                                    "1" -> "Monday"
                                    "2" -> "Tuesday"
                                    "3" -> "Wednesday"
                                    "4" -> "Thursday"
                                    "5" -> "Friday"
                                    "6" -> "Saturday"
                                    "7" -> "Sunday"
                                    else -> dayNumber
                                }
                            }

                            contentBuilder.appendLine("  Time: ${schedule.time}, Days: $daysText")
                        }
                    }

                    contentBuilder.appendLine()
                }
            }

            // Write the content to a file
            val content = contentBuilder.toString()
            val nsContent = NSString.create(string = content)
            nsContent.writeToFile(filePath, true, NSUTF8StringEncoding, null)

            return@withContext filePath
        } catch (e: Exception) {
            println("Error generating PDF: ${e.message}")
            return@withContext null
        }
    }

    override suspend fun openPdf(filePath: String): Boolean = withContext(Dispatchers.Default) {
        try {
            val fileURL = NSURL.fileURLWithPath(filePath)
            val sharedApplication = UIApplication.sharedApplication

            if (sharedApplication.canOpenURL(fileURL)) {
                sharedApplication.openURL(fileURL)
                return@withContext true
            }
            return@withContext false
        } catch (e: Exception) {
            println("Error opening PDF: ${e.message}")
            return@withContext false
        }
    }
}

/**
 * Factory function to create the appropriate PdfService implementation for iOS platform.
 */
actual fun createPdfService(): PdfService = IOSPdfService()
