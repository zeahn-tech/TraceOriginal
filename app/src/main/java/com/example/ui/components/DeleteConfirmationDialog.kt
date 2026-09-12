package com.example.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DeleteConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String = "Confirm Deletion",
    text: String = "Are you sure you want to delete this item? This action might be irreversible or move the item to the trash."
) {
    AlertDialog(
containerColor = androidx.compose.ui.graphics.Color(0xFFEEEEEE),
titleContentColor = androidx.compose.ui.graphics.Color(0xFF212121),
textContentColor = androidx.compose.ui.graphics.Color(0xFF212121),



        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismissRequest()
                }
            ) {
                Text("DELETE", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("CANCEL")
            }
        }
    )
}
