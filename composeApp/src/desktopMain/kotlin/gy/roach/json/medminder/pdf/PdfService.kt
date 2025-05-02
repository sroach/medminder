package gy.roach.json.medminder.pdf

import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationScheduleData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.Font
import com.itextpdf.text.FontFactory
import com.itextpdf.text.Phrase
import com.itextpdf.text.Element
import com.itextpdf.text.pdf.PdfPTable
import com.itextpdf.text.pdf.PdfWriter

/**
 * Desktop implementation of PdfService using iText library.
 */
class DesktopPdfService : PdfService {
    override suspend fun generateMedicationPdf(
        medications: List<MedicationData>,
        schedules: Map<Long, List<MedicationScheduleData>>,
        fileName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Create a unique file name with timestamp
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
            val file = File(System.getProperty("user.home"), "$fileName-$timestamp.pdf")
            
            // Create PDF document
            val document = Document()
            PdfWriter.getInstance(document, FileOutputStream(file))
            document.open()
            
            // Add title
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18f)
            document.add(Paragraph("Medication Report", titleFont).apply {
                alignment = Element.ALIGN_CENTER
                spacingAfter = 20f
            })
            
            // Add generation date
            val dateFont = FontFactory.getFont(FontFactory.HELVETICA, 12f)
            document.add(Paragraph("Generated on: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())}", dateFont).apply {
                alignment = Element.ALIGN_RIGHT
                spacingAfter = 20f
            })
            
            if (medications.isEmpty()) {
                document.add(Paragraph("No medications found."))
            } else {
                // Add medications and their schedules
                medications.forEach { medication ->
                    // Medication header
                    val medFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
                    document.add(Paragraph(medication.name, medFont).apply {
                        spacingBefore = 15f
                        spacingAfter = 5f
                    })
                    
                    // Medication description
                    if (!medication.description.isNullOrBlank()) {
                        document.add(Paragraph("Description: ${medication.description}").apply {
                            spacingAfter = 10f
                        })
                    }
                    
                    // Schedules
                    val medicationSchedules = schedules[medication.id] ?: emptyList()
                    if (medicationSchedules.isEmpty()) {
                        document.add(Paragraph("No schedules set for this medication."))
                    } else {
                        document.add(Paragraph("Schedules:").apply {
                            spacingAfter = 5f
                        })
                        
                        // Create a table for schedules
                        val table = PdfPTable(2) // 2 columns
                        table.widthPercentage = 100f
                        table.setWidths(floatArrayOf(1f, 3f)) // Time column is narrower
                        
                        // Add table headers
                        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12f)
                        table.addCell(Phrase("Time", headerFont))
                        table.addCell(Phrase("Days", headerFont))
                        
                        // Add schedule rows
                        medicationSchedules.forEach { schedule ->
                            table.addCell(schedule.time)
                            
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
                            table.addCell(daysText)
                        }
                        
                        document.add(table)
                    }
                }
            }
            
            document.close()
            return@withContext file.absolutePath
        } catch (e: Exception) {
            println("Error generating PDF: ${e.message}")
            e.printStackTrace()
            return@withContext null
        }
    }
    
    override suspend fun openPdf(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(file)
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
 * Factory function to create the appropriate PdfService implementation for desktop platform.
 */
actual fun createPdfService(): PdfService = DesktopPdfService()