package com.tiarhax.michilante.pages

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiarhax.michilante.ewm.storage.BlockedUser
import com.tiarhax.michilante.ewm.storage.CameraTempBlockingItem
import com.tiarhax.michilante.ewm.storage.CreateCameraTempBlockingInput
import com.tiarhax.michilante.ewm.storage.ICameraRepositoryV2
import com.tiarhax.michilante.ewm.storage.CameraRepositoryV2ForPreview
import com.tiarhax.michilante.ewm.storage.CameraListItemV2
import com.tiarhax.michilante.ewm.storage.ICameraRepository
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.ewm.storage.BlockableUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CameraDetailsPageStatus {
    LOADING, READY, ERROR
}

data class CameraDetailsUIState(
    val status: CameraDetailsPageStatus = CameraDetailsPageStatus.LOADING,
    val cameraId: String = "",
    val cameraName: String = "",
    val cameraUrl: String = "",
    val tempBlockings: List<CameraTempBlockingItem> = emptyList(),
    val error: String? = null,
    val showBlockUserModal: Boolean = false,
    val availableUsers: List<BlockableUser> = emptyList(),
    val isLoadingUsers: Boolean = false,
    val selectedUserId: String? = null,
    val showDatePicker: Boolean = false,
    val isBlocking: Boolean = false
)

class CameraDetailsPageViewModel(
    private val repository: ICameraRepositoryV2,
    private val cameraRepository: ICameraRepository,
    private val cameraId: String,
    private val cameraName: String,
    private val cameraUrl: String
) : ViewModel() {
    private val _uiState = MutableStateFlow(CameraDetailsUIState(
        cameraId = cameraId,
        cameraName = cameraName,
        cameraUrl = cameraUrl
    ))
    val uiState: StateFlow<CameraDetailsUIState> = _uiState.asStateFlow()

    init {
        loadTempBlockings()
    }

    fun loadTempBlockings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status = CameraDetailsPageStatus.LOADING)

            repository.listCameraTempBlockings(cameraId).fold(
                onSuccess = { blockings ->
                    _uiState.value = _uiState.value.copy(
                        status = CameraDetailsPageStatus.READY,
                        tempBlockings = blockings
                    )
                },
                onFailure = { error ->
                    Log.e("CameraDetailsPageViewModel", error.stackTraceToString())
                    _uiState.value = _uiState.value.copy(
                        status = CameraDetailsPageStatus.ERROR,
                        error = error.message
                    )
                }
            )
        }
    }

    fun showBlockUserModal() {
        _uiState.value = _uiState.value.copy(
            showBlockUserModal = true,
            isLoadingUsers = true,
            selectedUserId = null
        )
        loadAvailableUsers()
    }

    fun hideBlockUserModal() {
        _uiState.value = _uiState.value.copy(
            showBlockUserModal = false,
            selectedUserId = null,
            showDatePicker = false
        )
    }

    private fun loadAvailableUsers() {
        viewModelScope.launch {
            cameraRepository.listBlockableUsers(cameraId).fold(
                onSuccess = { users ->
                    _uiState.value = _uiState.value.copy(
                        availableUsers = users,
                        isLoadingUsers = false
                    )
                },
                onFailure = { error ->
                    Log.e("CameraDetailsPageViewModel", "Failed to load blockable users: ${error.message}")
                    _uiState.value = _uiState.value.copy(isLoadingUsers = false)
                }
            )
        }
    }

    fun selectUser(userId: String) {
        _uiState.value = _uiState.value.copy(
            selectedUserId = userId,
            showDatePicker = true
        )
    }

    fun blockUser(endDateMillis: Long) {
        val userId = _uiState.value.selectedUserId ?: return
        _uiState.value = _uiState.value.copy(isBlocking = true)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        val startTime = dateFormat.format(Date())
        val endTime = dateFormat.format(Date(endDateMillis))

        viewModelScope.launch {
            val input = CreateCameraTempBlockingInput(
                cameraId = cameraId,
                startTime = startTime,
                endTime = endTime,
                userIds = listOf(userId)
            )
            repository.createCameraTempBlocking(input).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isBlocking = false,
                        showBlockUserModal = false,
                        showDatePicker = false,
                        selectedUserId = null
                    )
                    loadTempBlockings()
                },
                onFailure = { error ->
                    Log.e("CameraDetailsPageViewModel", "Failed to block user: ${error.message}")
                    _uiState.value = _uiState.value.copy(isBlocking = false)
                }
            )
        }
    }

    fun unblockUser(userId: String) {
        viewModelScope.launch {
            repository.deleteCameraTempBlocking(cameraId, userId).fold(
                onSuccess = {
                    loadTempBlockings()
                },
                onFailure = { error ->
                    Log.e("CameraDetailsPageViewModel", "Failed to unblock user: ${error.message}")
                }
            )
        }
    }
}

@Composable
fun CameraDetailsPage(
    viewModel: CameraDetailsPageViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = uiState.cameraName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "URL: ${uiState.cameraUrl}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        HorizontalDivider()

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blocked Users",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = { viewModel.showBlockUserModal() }) {
                Icon(Icons.Default.Add, contentDescription = "Block User")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Block User")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        when (uiState.status) {
            CameraDetailsPageStatus.LOADING -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            CameraDetailsPageStatus.ERROR -> {
                Text(
                    text = uiState.error ?: "An error occurred",
                    color = MaterialTheme.colorScheme.error
                )
            }
            CameraDetailsPageStatus.READY -> {
                if (uiState.tempBlockings.isEmpty()) {
                    Text(
                        text = "No blocked users",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.tempBlockings) { blocking ->
                            BlockedUserCard(
                                blocking = blocking,
                                onDeleteClick = { viewModel.unblockUser(blocking.blockedUser.userId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Block User Modal
    if (uiState.showBlockUserModal) {
        BlockUserModal(
            users = uiState.availableUsers,
            isLoading = uiState.isLoadingUsers,
            selectedUserId = uiState.selectedUserId,
            showDatePicker = uiState.showDatePicker,
            isBlocking = uiState.isBlocking,
            onUserSelect = { viewModel.selectUser(it) },
            onDateConfirm = { viewModel.blockUser(it) },
            onDismiss = { viewModel.hideBlockUserModal() }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockUserModal(
    users: List<BlockableUser>,
    isLoading: Boolean,
    selectedUserId: String?,
    showDatePicker: Boolean,
    isBlocking: Boolean,
    onUserSelect: (String) -> Unit,
    onDateConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    if (showDatePicker && selectedUserId != null) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis() + 86400000L // Tomorrow
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { onDateConfirm(it) }
                    },
                    enabled = !isBlocking
                ) {
                    if (isBlocking) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp))
                    } else {
                        Text("Confirm")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Select User to Block") },
            text = {
                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (users.isEmpty()) {
                    Text("No users available")
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp)
                    ) {
                        items(users) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onUserSelect(user.userId) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedUserId == user.userId,
                                    onClick = { onUserSelect(user.userId) }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = user.name,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = user.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun BlockedUserCard(
    blocking: CameraTempBlockingItem,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = blocking.blockedUser.userName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Blocked until: ${blocking.endDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Unblock User",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CameraDetailsPagePreview() {
    val viewModel = CameraDetailsPageViewModel(
        repository = CameraRepositoryV2ForPreview(),
        cameraRepository = CameraRepositoryForPreview(),
        cameraId = "1",
        cameraName = "Front Door Camera",
        cameraUrl = "rtsp://example.com/stream1"
    )
    CameraDetailsPage(viewModel = viewModel)
}
