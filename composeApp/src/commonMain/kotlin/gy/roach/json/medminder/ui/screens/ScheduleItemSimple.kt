package gy.roach.json.medminder.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationScheduleData

/**
 * Simple card that displays a schedule without edit functionality
 */
@Composable
fun ScheduleItemSimple(schedule: MedicationScheduleData) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent // Use transparent as we'll use a gradient background
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp)
        ) {
            Text(
                text = "Time: ${schedule.time}",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(4.dp))

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

            Text(
                text = "Days: $daysText",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
