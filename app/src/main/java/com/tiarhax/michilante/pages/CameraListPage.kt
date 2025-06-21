package com.tiarhax.michilante.pages

import androidx.compose.foundation.layout.Arrangement
import com.tiarhax.michilante.R
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import com.tiarhax.michilante.components.Camera
import com.tiarhax.michilante.components.CameraModal
import com.tiarhax.michilante.components.CameraModalState
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.pages.ListCameraPageReadyStatePreview
import com.tiarhax.michilante.viewmodel.CreateCameraViewModel

data class CameraUpsertUIState (
    val editingCamera: Camera? = null,
    val showCameraDialog: Boolean = false,
    val error: String? = null,
    val status: CameraModalState = CameraModalState.READY
)

data class CameraListUIState(
    val cameraUpsertUIState: CameraUpsertUIState = CameraUpsertUIState(),
    val cameras: List<CameraListItem> = emptyList(),
    val status: CameraListPageStatus = CameraListPageStatus.LOADING,
    val error: String? = null
)

enum class CameraListPageStatus {
    LOADING, READY, ERROR, EMPTY
}

class CamerasListPageViewModel(
    private val repository: ICameraRepository
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
        viewModelScope.launch {
            repository.putCamera(
                com.tiarhax.michilante.ewm.storage.PutCameraInput(
                    id = camera.id,
                    name = camera.name,
                    sourceUrl = camera.sourceUrl
                )
            ).fold(
                onSuccess = { out ->
                    loadCameras()
                    resetCameraForm()
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(

                        cameraUpsertUIState = _uiState.value.cameraUpsertUIState.copy(
                            status = CameraModalState.ERROR,
                            error = err.message

                        )
                    )
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
                    CameraListPageReadyState(cameras = uiState.cameras, viewModel = viewModel)
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
    cameras: List<CameraListItem>,
    viewModel: CamerasListPageViewModel
) {
    CameraList(cameras, viewModel)
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
                    val camera = Camera(id = c.id, name = c.name, sourceUrl = c.sourceUrl)
                    viewModel.showEditCameraDialog(camera)
                },
                onClickInputClosure = {},
                onDeleteClickClosure = {}
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


@Preview(showBackground = true)
@Composable
fun PreviewCameraListPage() {

    val dummyCameras = listOf(
        CameraListItem(id = "AAABBBCCC", name = "Front Door Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCD", name = "Backyard Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCX", name = "Garage Camera", sourceUrl = "")
    )



    CameraListPage(viewModel = CamerasListPageViewModel(repository = CameraRepositoryForPreview()), modifier = Modifier.fillMaxWidth())
}

@Preview(showBackground = true)
@Composable
fun ListCameraPageReadyStatePreview() {

    val dummyCameras = listOf(
        CameraListItem(id = "AAABBBCCC", name = "Front Door Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCD", name = "Backyard Camera", sourceUrl = ""),
        CameraListItem(id = "AAABBBCCX", name = "Garage Camera", sourceUrl = "")
    )



    CameraListPageReadyState(cameras = dummyCameras, viewModel = CamerasListPageViewModel(repository = CameraRepositoryForPreview()),)
}




