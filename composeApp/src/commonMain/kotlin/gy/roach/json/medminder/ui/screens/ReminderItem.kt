package gy.roach.json.medminder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationData
import gy.roach.json.medminder.db.MedicationReminderData
import kotlinx.datetime.Clock

/**
 * Composable that displays a single medication reminder with actions to acknowledge or delete
 */
@Composable
fun ReminderItem(
    reminder: MedicationReminderData,
    medication: MedicationData?,
    onAcknowledge: () -> Unit,
    onDelete: () -> Unit
) {
    // Calculate if the reminder is overdue
    val now = Clock.System.now().epochSeconds
    val isOverdue = reminder.reminderTime < now && !reminder.acknowledged

    // Animation for hover/press effect
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f)

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Transparent, // Use transparent as we'll use a gradient background
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .scale(scale)
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium
            )
            .clickable {
                isPressed = true
                // Reset pressed state after a short delay
                isPressed = false
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            if (isOverdue) 
                                MaterialTheme.colorScheme.errorContainer
                            else if (reminder.acknowledged)
                                MaterialTheme.colorScheme.secondaryContainer
                            else
                                MaterialTheme.colorScheme.primaryContainer,
                            if (isOverdue)
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                            else if (reminder.acknowledged)
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                            else
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        )
                    ),
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Medication name and time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Medication name or placeholder if medication is null
                    Text(
                        text = medication?.name ?: "Unknown Medication",
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Format the scheduled time for display
                    Text(
                        text = reminder.scheduledTime,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Scheduled date
                Text(
                    text = "Scheduled for: ${reminder.scheduledDate}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Medication description if available
                medication?.description?.let { description ->
                    if (description.isNotBlank()) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }

                // Status and action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Status text
                    Text(
                        text = when {
                            reminder.acknowledged -> "Taken"
                            isOverdue -> "Overdue"
                            else -> "Pending"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            reminder.acknowledged -> MaterialTheme.colorScheme.secondary
                            isOverdue -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )

                    // Action buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Acknowledge button (only show if not already acknowledged)
                        if (!reminder.acknowledged) {
                            Button(
                                onClick = onAcknowledge,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Take Medication",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Take")
                            }
                        }

                        // Delete button
                        IconButton(
                            onClick = onDelete
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Reminder",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}
