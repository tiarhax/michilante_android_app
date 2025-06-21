package com.tiarhax.michilante.components

import android.util.Log
import androidx.compose.material.icons.filled.Call


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class Camera(
    val id: String? = "",
    val name: String = "",
    val sourceUrl: String = ""
)
enum class CameraModalState {
    LOADING,
    READY,
    ERROR
}
@Composable
fun CameraModal(
    isVisible: Boolean,
    camera: Camera? = null,
    onDismiss: () -> Unit,
    onSave: (Camera) -> Unit
) {
    if (isVisible) {
        var name by remember(camera) { mutableStateOf(camera?.name ?: "") }
        var status by remember { mutableStateOf<CameraModalState>(CameraModalState.READY)}
        var rtspUrl by remember(camera) { mutableStateOf(camera?.sourceUrl ?: "") }
        var nameError by remember { mutableStateOf<String?>(null) }
        var rtspError by remember { mutableStateOf<String?>(null) }

        val isEditing = camera != null
        val title = if (isEditing) "Edit Camera" else "Add New Camera"

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = onDismiss) {

                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Camera Name Field
                    OutlinedTextField(
                        value = name,
                        //enabled  = status == CameraModalState.LOADING,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        label = { Text("Camera Name") },
                        placeholder = { Text("Enter camera name") },
                        isError = nameError != null,
                        supportingText = nameError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // RTSP URL Field
                    OutlinedTextField(
                        value = rtspUrl,
                        //enabled  = status == CameraModalState.LOADING,
                        onValueChange = {
                            rtspUrl = it
                            rtspError = null
                        },
                        label = { Text("RTSP URL") },
                        placeholder = { Text("rtsp://192.168.1.100:554/stream") },
                        isError = rtspError != null,
                        supportingText = rtspError?.let { { Text(it) } },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = onDismiss
                        ) {
                            Text("Cancel")
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            enabled = status == CameraModalState.READY,
                            onClick = {
                                // Validate inputs
                                var hasError = false

                                if (name.isBlank()) {
                                    nameError = "Camera name is required"
                                    hasError = true
                                }

                                if (rtspUrl.isBlank()) {
                                    rtspError = "RTSP URL is required"
                                    hasError = true
                                } else if (!isValidRtspUrl(rtspUrl)) {
                                    rtspError = "Invalid RTSP URL format"
                                    hasError = true
                                }
                                Log.i("CameraModal ", "hasError: ${hasError}")
                                if (!hasError) {
                                    val newCamera = Camera(
                                        id = camera?.id,
                                        name = name.trim(),
                                        sourceUrl = rtspUrl.trim()
                                    )
                                    onSave(newCamera)
                                } else {
                                    status = CameraModalState.ERROR
                                }
                            }
                        ) {
                            Text(if (isEditing) "Update Camera" else "Add Camera")
                        }
                    }
                }
            }
        }
    }
}

// Helper function to validate RTSP URL
private fun isValidRtspUrl(url: String): Boolean {
    return url.startsWith("rtsp://", ignoreCase = true) &&
            url.length > 7 &&
            url.contains(":")
}

// Helper function to generate a simple ID
private fun generateId(): String {
    return System.currentTimeMillis().toString()
}

