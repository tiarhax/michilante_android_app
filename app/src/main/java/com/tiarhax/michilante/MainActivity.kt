package com.tiarhax.michilante

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.jakewharton.threetenabp.AndroidThreeTen
import com.tiarhax.michilante.components.AppScaffold
import com.tiarhax.michilante.ewm.storage.CameraRepositoryForPreview
import com.tiarhax.michilante.pages.CameraListPage
import com.tiarhax.michilante.pages.CameraListPageStatus
import com.tiarhax.michilante.pages.CamerasListPageViewModel
import com.tiarhax.michilante.ui.theme.MichilanteTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidThreeTen.init(this);
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MichilanteTheme {
                AppScaffold(context = this)
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    val viewModel = CamerasListPageViewModel(
        repository = CameraRepositoryForPreview(),
        context = null
    )
    CameraListPage(viewModel = viewModel, modifier = Modifier.fillMaxSize())
}