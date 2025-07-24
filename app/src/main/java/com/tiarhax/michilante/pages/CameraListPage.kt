package com.tiarhax.michilante.pages

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import com.tiarhax.michilante.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.tiarhax.michilante.components.CameraListItem
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tiarhax.michilante.components.CameraListCard
import com.tiarhax.michilante.ewm.storage.ICameraRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import com.tiarhax.michilante.activity.VideoStreamActivity
import com.tiarhax.michilante.components.Camera
import com.tiarhax.michilante.components.CameraModal
import com.tiarhax.michilante.components.CameraModalState
import com.tiarhax.michilante.components.LoadingDialog
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.ewm.storage.CreateCameraInput
import com.tiarhax.michilante.ewm.storage.PutCameraInput
import com.tiarhax.michilante.pages.ListCameraPageReadyStatePreview
import com.tiarhax.michilante.viewmodel.CreateCameraViewModel
import kotlin.Int
import kotlin.time.Duration

data class CameraUpsertUIState (
    val editingCamera: Camera? = null,
    val showCameraDialog: Boolean = false,
    val error: String? = null,
    val status: CameraModalState = CameraModalState.READY
)

data class CameraListUIState(
    val cameraIdToDelete: String? = null,
    val showDeleteCameraDialog: Boolean = false,
    val showDeleteFailedDialog: Boolean = false,
    val showLoadingStreamDialog: Boolean = false,
    val cameraUpsertUIState: CameraUpsertUIState = CameraUpsertUIState(),
    val cameras: List<CameraListItem> = emptyList(),
    val status: CameraListPageStatus = CameraListPageStatus.LOADING,
    val error: String? = null
)

enum class CameraListPageStatus {
    LOADING, READY, ERROR, EMPTY
}

class CamerasListPageViewModel(
    private val repository: ICameraRepository,
    private val context: Context?
): ViewModel() {
    private val _uiState = MutableStateFlow(CameraListUIState())
    val uiState: StateFlow<CameraListUIState> = _uiState.asStateFlow()
    init {
        loadCameras()
    }

    fun loadCameras() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(status=CameraListPageStatus.LOADING, error = null)

            repository.listCameras().fold(
                onSuccess = { cameras ->
                    if (cameras.isEmpty()){
                        _uiState.value = _uiState.value.copy(status = CameraListPageStatus.EMPTY, cameras = emptyList())
                    } else {
                        val mappedCameras = cameras.map { c ->
                            CameraListItem(
                                id = c.id,
                                name = c.name,
                                sourceUrl = c.sourceUrl
                            )
                        }
                        _uiState.value = _uiState.value.copy(
                            cameras = mappedCameras,
                            status = CameraListPageStatus.READY
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(status = CameraListPageStatus.ERROR, cameras = emptyList(), error = error.message)
                }
            );
        }
    }

    fun retry() {
        loadCameras()
    }

    fun showAddCameraDialog() {
        _uiState.value = _uiState.value.copy(
            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                showCameraDialog = true
            )
        )
    }

    fun showEditCameraDialog(camera: Camera) {
        _uiState.value = _uiState.value.copy(
            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                showCameraDialog = true,
                editingCamera = camera
            )
        )
    }
    fun resetCameraForm() {
        _uiState.value = _uiState.value.copy(
            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                editingCamera = null,
                showCameraDialog = false,

            )
        )
    }
    fun dismiss() {
        resetCameraForm()
    }



    fun saveCamera(camera: Camera) {
        _uiState.value = _uiState.value.copy(
            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                status = CameraModalState.LOADING
            )
        )
        Log.d("CamerasListPageViewModel", "updating camera")
        if (_uiState.value.cameraUpsertUIState.editingCamera != null) {
            viewModelScope.launch {
                val editingCamera = _uiState.value.cameraUpsertUIState.editingCamera!!;
                _uiState.value = _uiState.value.copy(

                    cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                        status = CameraModalState.LOADING,

                        )
                )
                repository.putCamera(
                    editingCamera.id!!,
                    com.tiarhax.michilante.ewm.storage.PutCameraInput(
                        name = camera.name,
                        sourceUrl = camera.sourceUrl
                    )
                ).fold(
                    onSuccess = { out ->
                        loadCameras()
                        resetCameraForm()
                    },
                    onFailure = { err ->
                        Log.d("CameraListPage", "Failure executed")
                        if (context != null) {
                            Log.d("CameraListPage", "Should make toast")

                            Toast.makeText(context, err.message, Toast.LENGTH_LONG).show();
                        }
                        _uiState.value = _uiState.value.copy(
                            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                                status = CameraModalState.ERROR,
                                error = err.message,
                                showCameraDialog = false
                            )

                        )
                    }
                )

            }
        } else {
            Log.d("CamerasListPageViewModel", "creating camera")
            viewModelScope.launch {
                _uiState.value = _uiState.value.copy(

                    cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                        status = CameraModalState.LOADING,

                    )
                )
                repository.createCamera(
                    CreateCameraInput(
                        name = camera.name,
                        sourceUrl = camera.sourceUrl
                    )
                ).fold(
                    onSuccess = { out ->
                        loadCameras()
                        resetCameraForm()
                    },
                    onFailure = { err ->
                        Log.d("CameraListPage", "Failure executed")
                        if (context != null) {
                            Log.d("CameraListPage", "Should make toast")

                            Toast.makeText(context, err.message, Toast.LENGTH_LONG).show();
                        }
                        _uiState.value = _uiState.value.copy(

                            cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                                status = CameraModalState.ERROR,
                                error = err.message,
                                showCameraDialog = false
                            )
                        )
                    }
                )

            }
        }


    }

    fun deleteCamera(id: String) {
        Log.d("CamerasListPageViewModel", "deleting camera $id");

        viewModelScope.launch {
            repository.deleteCamera(id).fold(
                onSuccess = {
                    dismissCameraDeleteDialog()
                    loadCameras()


                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        showDeleteCameraDialog = false,
                        showDeleteFailedDialog = true
                    )
                    _uiState.value = _uiState.value.copy(
                        error = err.message
                    )
                }
            )
        }
    }

    fun dismissCameraDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteCameraDialog = false,
            cameraIdToDelete = null
        )
    }

    fun dismissCameraDeleteErrorDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteFailedDialog = false,
            cameraIdToDelete = null
        )
    }

    fun showCameraDeleteDialog(id: String) {
        _uiState.value = _uiState.value.copy(
            showDeleteCameraDialog = true,
            cameraIdToDelete = id
        )
    }

    fun goToCameraVideo(id: String) {
        _uiState.value = _uiState.value.copy(
            showLoadingStreamDialog = true,
        )
        viewModelScope.launch {
            repository.getCameraStream(id).fold(
                onSuccess = { stream ->
                    _uiState.value = _uiState.value.copy(
                        showLoadingStreamDialog = false,
                    )
                    Log.d("getCameraStream", "success allegedly")
                    if (context != null) {
                        val intent = Intent(context, VideoStreamActivity::class.java);
                        intent.putExtra("rtspUrl", stream.tempRtspUrl)
                        context.startActivity(intent)
                    }

                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        showLoadingStreamDialog = false,
                    )
                    if (context != null) {

                        Toast.makeText(context, error.message, Toast.LENGTH_LONG).show()
                    }
                    Log.e("getCameraStream", error.toString())
                }
            )
        }
    }

}



@Composable
fun CameraListPage(
    modifier: Modifier,
    viewModel: CamerasListPageViewModel
) {
    val scrollState = rememberScrollState()
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier) {
        Column (modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
            Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End){
                Button(
                    onClick = {
                        viewModel.showAddCameraDialog()
                    },
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Gray,
                        containerColor = Color.Transparent
                    )) {
                    Icon(
                        painter = painterResource(R.drawable.plus),
                        contentDescription = null
                    )
                }

                Button(
                    onClick = {
                        viewModel.retry()
                    },
                    enabled = uiState.status != CameraListPageStatus.LOADING,
                    colors = ButtonDefaults.buttonColors(
                        contentColor = Color.Gray,
                        containerColor = Color.Transparent
                    )) {
                    Icon(
                        painter = painterResource(R.drawable.fame_reload),
                        contentDescription = null
                    )
                }
            }
            when (uiState.status) {
                CameraListPageStatus.LOADING -> {
                    CameraListPageLoadingState()
                }

                CameraListPageStatus.READY -> {
                    CameraListPageReadyState(cameras = uiState.cameras, viewModel = viewModel, showCameraDeleteDialog = uiState.showDeleteCameraDialog, cameraIdToDelete = uiState.cameraIdToDelete, showCameraDeleteErrorDialog = uiState.showDeleteFailedDialog, showLoadingStreamDialog = uiState.showLoadingStreamDialog)
                }

                CameraListPageStatus.ERROR -> {
                    CameraListPageErrorState()
                }

                CameraListPageStatus.EMPTY -> {
                    CameraListPageEmptyState()
                }
            }
        }
    }

    CameraModal(
        isVisible = uiState.cameraUpsertUIState.showCameraDialog,
        camera = uiState.cameraUpsertUIState.editingCamera,
        onDismiss = {
            viewModel.dismiss()
        },
        onSave = { camera ->
            viewModel.saveCamera(camera)
        }
    )
}


@Composable
fun CameraListPageLoadingState(

) {
    Box (modifier= Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(text = "Loading...", modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun CameraListPageReadyState(
    showLoadingStreamDialog: Boolean,
    showCameraDeleteDialog: Boolean,
    showCameraDeleteErrorDialog: Boolean,
    cameraIdToDelete: String?,
    cameras: List<CameraListItem>,
    viewModel: CamerasListPageViewModel
) {
    CameraList(cameras, viewModel)
    if (showCameraDeleteDialog) {
        DeleteCameraDialog(viewModel, cameraIdToDelete!!)
    }
    if (showCameraDeleteErrorDialog) {
        ErrorDeletingCameraDialog(viewModel)
    }

    LoadingDialog(
        isLoading = showLoadingStreamDialog,
        onDismiss = {}
    )
}

@Composable
fun CameraList(
    cameras: List<CameraListItem>,
    viewModel: CamerasListPageViewModel
) {
    Column (modifier = Modifier.fillMaxWidth()) {
        cameras.map { c ->
            CameraListCard(
                camera = c,
                onEditClickClosure = {
                    Log.d("CameraID", c.id)
                    val camera = Camera(id = c.id, name = c.name, sourceUrl = c.sourceUrl)
                    viewModel.showEditCameraDialog(camera)
                },
                onClickInputClosure = {
                    viewModel.goToCameraVideo(c.id)
                },
                onDeleteClickClosure = {
                    viewModel.showCameraDeleteDialog(c.id)
                }
            )
        }
    }
}

@Composable
fun CameraListPageErrorState(

) {
    Column (modifier = Modifier.fillMaxWidth()) {
        Text(text = "Failed to load cameras", modifier = Modifier.fillMaxSize())
        Button(
            onClick = { loadCameras() }
        ) {
            Text(text = "Retry")
        }
    }
}

@Composable
fun CameraListPageEmptyState(

) {
    Column (modifier = Modifier.fillMaxWidth()) {
        Text(text = "No cameras found", modifier = Modifier.fillMaxSize())
        Text(text = "...")
    }
}

fun loadCameras(): List<CameraListItem> {
    return emptyList()
}

@Composable
fun ErrorDeletingCameraDialog(
    viewModel: CamerasListPageViewModel,
) {
    AlertDialog(
        onDismissRequest = {
            viewModel.dismissCameraDeleteDialog()
        },
        title = {
            Text("Error deleting camera")
        },
        text = {
            Text("There was an error while trying to delete the camera")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.dismissCameraDeleteErrorDialog()
                }
            ) {
                Text("Accept", color = MaterialTheme.colorScheme.primary)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.dismissCameraDeleteErrorDialog()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteCameraDialog(
    viewModel: CamerasListPageViewModel,
    cameraIdToDelete: String
) {
    AlertDialog(
        onDismissRequest = {
            viewModel.dismissCameraDeleteDialog()
        },
        title = {
            Text("Delete Camera")
        },
        text = {
            Text("Are you sure you want to delete this camera? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.deleteCamera(cameraIdToDelete)
                }
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    viewModel.dismissCameraDeleteDialog()
                }
            ) {
                Text("Cancel")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun PreviewCameraListPage() {

    val dummyCameras = listOf(
        CameraListItem(id = "AAABBBCCC", name = "Front Door Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCD", name = "Backyard Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCX", name = "Garage Camera", sourceUrl = "")
    )



    CameraListPage(viewModel = CamerasListPageViewModel(repository = CameraRepositoryForPreview(), context = null), modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
fun ListCameraPageReadyStatePreview() {

    val dummyCameras = listOf(
        CameraListItem(id = "AAABBBCCC", name = "Front Door Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCD", name = "Backyard Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCX", name = "Garage Camera", sourceUrl = "")
    )



    CameraListPageReadyState(cameras = dummyCameras, viewModel = CamerasListPageViewModel(repository = CameraRepositoryForPreview(), context = null), showCameraDeleteDialog = false, cameraIdToDelete = null, showLoadingStreamDialog = false, showCameraDeleteErrorDialog = false)
}






