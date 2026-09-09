package com.example.ruwia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ruwia.ui.dashboard.NTColors

// ─────────────────────────────────────────────────────────────
//  Empty Cases quick-add dialog
//  Shows the live Empty Cases figure and a +/− stepper to bump
//  it by a chosen number directly from the home screen.
// ─────────────────────────────────────────────────────────────

@Composable
fun EmptyCasesStepperDialog(
    currentCount: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var qty by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NTColors.Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Add Empty Cases",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = NTColors.TextPrimary,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Currently $currentCount empty case${if (currentCount != 1) "s" else ""}",
                    fontSize = 13.sp,
                    color = NTColors.TextSecondary,
                    textAlign = TextAlign.Center,
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    StepperButton(
                        icon = Icons.Rounded.Remove,
                        contentDescription = "Decrease",
                        onClick = { qty = (qty - 1).coerceAtLeast(1) },
                    )
                    Text(
                        text = "+$qty",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Black,
                        color = NTColors.Warning,
                        modifier = Modifier
                            .align(Alignment.CenterVertically)
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center,
                    )
                    StepperButton(
                        icon = Icons.Rounded.Add,
                        contentDescription = "Increase",
                        onClick = { qty = (qty + 1).coerceAtMost(9999) },
                    )
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Records ${qty} returned empty case${if (qty != 1) "s" else ""} to the live Empty Cases count.",
                    fontSize = 12.sp,
                    color = NTColors.TextTertiary,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(qty) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = NTColors.Primary),
            ) {
                Text("Add $qty Empty Case${if (qty != 1) "s" else ""}", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = NTColors.TextSecondary)
            }
        },
    )
}

@Composable
private fun StepperButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(NTColors.SurfaceVar)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = NTColors.Primary,
            modifier = Modifier.size(24.dp),
        )
    }
}