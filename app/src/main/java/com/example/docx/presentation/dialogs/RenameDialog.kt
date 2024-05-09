package com.example.docx.presentation.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.docx.R
import com.example.docx.presentation.home.HomeEvent
import com.example.docx.presentation.home.HomeState
import com.example.docx.ui.theme.DocxTheme

@Composable
fun RenameDialog(
    event: (HomeEvent) -> Unit,
    state: HomeState
) {
    var newName by remember { mutableStateOf("") }
    LaunchedEffect(state.selectedPdf) {
        newName = state.selectedPdf?.name?.substringBeforeLast(".") ?: ""
    }
    if (state.isRenameDialogOpen) {
        Dialog(onDismissRequest = { event(HomeEvent.HideRenameDialog) }) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Rename PDF",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = colorResource(id = R.color.text_title)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.align(Alignment.End),
                    ) {
                        OutlinedButton(onClick = { event(HomeEvent.HideRenameDialog) }) {
                            Text(text = "Cancel")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(onClick = {
                            event(HomeEvent.RenamePdf(state.selectedPdf!!, newName))
                        }) {
                            Text(text = "Rename")
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RenameDialogPreview() {
    DocxTheme {

        RenameDialog(event = {}, state = HomeState(isRenameDialogOpen = true))
    }
}