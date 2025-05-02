package gy.roach.json.medminder.pdf

import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationScheduleData

/**
 * Interface for generating PDF documents of medications and schedules.
 * This is implemented differently on each platform.
 */
interface PdfService {
    /**
     * Generate a PDF document containing medication information.
     * 
     * @param medications List of medications to include in the PDF
     * @param schedules Map of medication ID to list of schedules for that medication
     * @param fileName Name to use for the generated file (without extension)
     * @return The path to the generated PDF file, or null if generation failed
     */
    suspend fun generateMedicationPdf(
        medications: List<MedicationData>,
        schedules: Map<Long, List<MedicationScheduleData>>,
        fileName: String = "medications"
    ): String?
    
    /**
     * Open a PDF file with the platform's default PDF viewer.
     * 
     * @param filePath Path to the PDF file to open
     * @return true if the file was opened successfully, false otherwise
     */
    suspend fun openPdf(filePath: String): Boolean
}

/**
 * Factory function to create the appropriate PdfService implementation for the current platform.
 * This is implemented differently on each platform.
 */
expect fun createPdfService(): PdfService