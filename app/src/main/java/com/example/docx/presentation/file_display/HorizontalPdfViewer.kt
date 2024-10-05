package com.example.docx.presentation.file_display

import android.content.res.Configuration
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

@Composable
fun HorizontalPdfViewer(horizontalReaderState: HorizontalReaderState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launchScanner = horizontalReaderState.getImportLauncher(interceptResult = {
        it.compressImageToThreshold(2)
    })

    BoxWithConstraints(
        modifier = Modifier,
        contentAlignment = Alignment.Center
    ) {
        val configuration = LocalConfiguration.current
        val containerSize = remember {
            IntSize(constraints.maxWidth, constraints.maxHeight)
        }

        LaunchedEffect(Unit) {
            horizontalReaderState.load(
                context = context,
                coroutineScope = scope,
                containerSize = containerSize,
                isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
                customResource = null
            )
        }
        val loadState = horizontalReaderState.vueLoadState
        when (loadState) {
            is LoadState.NoDocument -> {
                Button(onClick = {
                    launchScanner()
                }) {
                    Text(text = "Import Document")
                }
            }

            is LoadState.DocumentError -> {
                Column {
                    Text(text = "Error:  ${horizontalReaderState.vueLoadState.getErrorMessage}")
                    Button(onClick = {
                        scope.launch {
                            horizontalReaderState.load(
                                context = context,
                                coroutineScope = scope,
                                containerSize = containerSize,
                                isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT,
                                customResource = null
                            )
                        }
                    }) {
                        Text(text = "Retry")
                    }
                }
            }

            is LoadState.DocumentImporting -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(text = "Importing...")
                }
            }

            is LoadState.DocumentLoaded -> {
                HorizontalSample(
                    horizontalReaderState = horizontalReaderState,
                    import = {launchScanner()}
                )
            }

            is LoadState.DocumentLoading -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Text(text = "Loading ${horizontalReaderState.loadPercent}")
                }
            }
        }
    }
}