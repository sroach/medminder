package gy.roach.json.medminder.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import gy.roach.json.medminder.db.MedicationData


/**
 * Card that displays a single medication with swipe-to-delete functionality
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationItem(
    medication: MedicationData,
    onClick: () -> Unit,
    onEditClick: (MedicationData) -> Unit = {},
    onDeleteClick: (MedicationData) -> Unit = {},
    isSelected: Boolean = false,
    notTakenCount: Int = 0
) {
    var offsetX by remember { mutableStateOf(0f) }
    val deleteThreshold = -100f // Threshold to trigger delete action

    // Animation for the card scale when swiping
    val scale by animateFloatAsState(
        targetValue = if (offsetX < deleteThreshold) 0.95f else 1f
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Delete action background (revealed when swiping)
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(end = 16.dp)
                .clickable { onDeleteClick(medication) }, // Make the delete background clickable
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Delete",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Main card content
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent, // Use transparent as we'll use a gradient background
                contentColor = MaterialTheme.colorScheme.onSurface // Keep text color consistent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .offset(x = offsetX.dp)
                .scale(scale)
                .shadow(
                    elevation = 4.dp,
                    shape = MaterialTheme.shapes.medium
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        // Only allow swiping left (negative delta)
                        if (offsetX + delta <= 0) {
                            offsetX += delta
                        }
                    },
                    onDragStopped = {
                        // Reset position without triggering delete action
                        offsetX = 0f
                    }
                ),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                if (isSelected)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                        ),
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = medication.name,
                            style = MaterialTheme.typography.titleLarge
                        )

                        // Badge to show count of medication not taken today
                        if (notTakenCount > 0) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error,
                                        shape = MaterialTheme.shapes.small
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = notTakenCount.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                    // Description is removed from the list view and will be shown in the detail view
                }
            }
        }
    }
}