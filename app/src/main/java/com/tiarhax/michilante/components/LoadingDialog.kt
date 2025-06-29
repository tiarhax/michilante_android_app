package com.tiarhax.michilante.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.websocket.Frame.Text

@Composable
fun LoadingDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit = {}
) {
    if (isLoading) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Loading") },
            text = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Please wait...")
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}