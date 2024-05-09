package com.example.docx.presentation.dialogs

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.docx.R
import com.example.docx.presentation.home.HomeEvent
import com.example.docx.presentation.home.HomeState
import com.example.docx.ui.theme.DocxTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreDialog(
    event: (HomeEvent) -> Unit,
    state: HomeState,
    modifier: Modifier = Modifier
){
    val sheetState = rememberModalBottomSheetState()
    if(
        state.isDialogOpen
    ) {
        ModalBottomSheet(
            sheetState = sheetState,
            onDismissRequest = { event(HomeEvent.HideDialog) },
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier.clickable {
                        event(HomeEvent.ShowRenameDialog)
                        event(HomeEvent.HideDialog)
                    }
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = null,
                        tint = colorResource(id = R.color.body),
                        modifier = modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Rename",
                        color = colorResource(id = R.color.body),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(
                    modifier = Modifier.clickable {
                        state.selectedPdf?.let {
                            event(HomeEvent.SharePdf(it))
                            event(HomeEvent.HideDialog)
                        }
                    }
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = colorResource(id = R.color.body),
                        modifier = modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Share",
                        color = colorResource(id = R.color.body),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Row(
                    modifier = Modifier.clickable {
                        state.selectedPdf?.let {
                            event(HomeEvent.DeletePdf(it))
                            event(HomeEvent.HideDialog)
                        }
                    }
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically

                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = colorResource(id = R.color.body),
                        modifier = modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Delete",
                        color = colorResource(id = R.color.body),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun MoreDialogPreview() {
    DocxTheme {
        MoreDialog(event = {}, modifier = Modifier, state = HomeState(isDialogOpen = true))
    }
}