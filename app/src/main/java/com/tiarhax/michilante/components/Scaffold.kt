package com.tiarhax.michilante.components

import Auth0Manager
import AuthViewModel
import LoginScreenContainer
import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tiarhax.michilante.ewm.storage.CameraRepository
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.pages.CameraListPage
import com.tiarhax.michilante.pages.CameraListPageStatus
import com.tiarhax.michilante.pages.CamerasListPageViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(camerasPageViewModel: CamerasListPageViewModel, authViewModel: AuthViewModel) {
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = { TopAppBar(title = { Text(text= "Camera List") }) }
    ) { givenPadding ->




        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "login"
        ) {
            composable("login") {
                LoginScreenContainer(navController = navController, authViewModel = authViewModel)
            }
            composable("cameras-list") {
                CameraListPage(viewModel = camerasPageViewModel, modifier = Modifier.fillMaxWidth().padding(givenPadding))
            }
        }
    }
}