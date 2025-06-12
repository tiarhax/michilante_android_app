package com.tiarhax.michilante.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tiarhax.michilante.ewm.storage.CameraRepository
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.pages.CameraListPage
import com.tiarhax.michilante.pages.CameraListPageStatus
import com.tiarhax.michilante.pages.CamerasListPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold() {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(text= "Camera List") }) }
    ) { givenPadding ->
        val viewModel = CamerasListPageViewModel(
            repository = CameraRepository()
        )
        CameraListPage(viewModel = viewModel, modifier = Modifier.fillMaxWidth().padding(givenPadding))
    }
}