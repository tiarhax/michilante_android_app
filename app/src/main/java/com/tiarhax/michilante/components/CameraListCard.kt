package com.tiarhax.michilante.components


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import com.tiarhax.michilante.R

data class CameraListItem(
    val id: String,
    val name: String,
    val sourceUrl: String
)

@Composable
fun CameraListCard(
    camera: CameraListItem,
    onClickInputClosure: () -> Unit,
    onEditClickClosure: () -> Unit,
    onDeleteClickClosure: () -> Unit
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        onClick = { onClickInputClosure() }
    ) {
        Card (
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ){
            Row(modifier = Modifier.border(2.dp, MaterialTheme.colorScheme.secondary).fillMaxWidth(), horizontalArrangement = Arrangement.Absolute.Left  ) {
                Box( modifier = Modifier.fillMaxWidth(0.5f))  {
                    Text(

                        text = camera.name,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Left,
                        modifier = Modifier
                            .padding(20.dp)


                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Row (modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Button(
                            onClick = onEditClickClosure,
                            modifier = Modifier.padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null
                            )
                        }


                        Button(
                            onClick = onDeleteClickClosure,
                            modifier = Modifier.padding(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null
                            )
                        }
                    }
                }

            }
        }


    }


}

@Composable
@Preview
fun CameraListCardPreview() {
    val camera = CameraListItem(id = "1", name = "Living Room 1", sourceUrl = "")
    CameraListCard(
        onClickInputClosure = {},
        camera = camera,
        onEditClickClosure = {},
        onDeleteClickClosure = {}
    )
}

