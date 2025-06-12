package com.tiarhax.michilante.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

data class CameraListItem(
    val id: String,
    val name: String
)

@Composable
fun CameraListCard(
    camera: CameraListItem,
    onClickInputClosure: () -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        onClick = { onClickInputClosure() }
    ) {
        Card (
            colors = CardDefaults.cardColors(
                containerColor = Color.Gray
            )
        ){
            Text(
                text = camera.name,
                textAlign = TextAlign.Left,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)

            )
        }


    }


}

@Composable
@Preview
fun CameraListCardPreview() {
    val camera = CameraListItem(id = "1", name = "Living Room 1")
    CameraListCard(
        onClickInputClosure = {},
        camera = camera,
    )
}

