package com.example.docx.presentation.file_display

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.docx.util.pinchToZoomAndDrag
import kotlinx.coroutines.launch

@Composable
fun HorizontalVueReader(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    horizontalReaderState: HorizontalReaderState,
) {
    val vueRenderer = horizontalReaderState.renderer
    val currentPage = horizontalReaderState.currentPage
    val coroutineScope = rememberCoroutineScope()
    if (horizontalReaderState.cache != 0)
        LaunchedEffect(key1 = currentPage, block = {
            vueRenderer?.loadWithCache(currentPage)
        })
    if (vueRenderer != null)
        HorizontalPager(
            modifier = modifier,
            userScrollEnabled = false,
            state = horizontalReaderState.pagerState
        ) { idx ->
            val pageContent by vueRenderer.pageLists[idx].stateFlow.collectAsState()
            if (horizontalReaderState.cache == 0)
                DisposableEffect(key1 = Unit) {
                    vueRenderer.pageLists[idx].load()
                    onDispose {
                        vueRenderer.pageLists[idx].recycle()
                    }
                }
            AnimatedContent(targetState = pageContent, label = "") {
                when (it) {
                    is PageState.BlankState -> {
                        BlankPage(
                            modifier = contentModifier,
                            width = horizontalReaderState.containerSize!!.width,
                            height = horizontalReaderState.containerSize!!.height
                        )
                    }

                    is PageState.LoadedState -> {
                        Image(
                            modifier = contentModifier
                                .clipToBounds()
                                .pinchToZoomAndDrag()
                                .pointerInput(Unit) {
                                    detectHorizontalDragGestures { _, dragAmount ->
                                        coroutineScope.launch {
                                            when {
                                                dragAmount < -50 -> horizontalReaderState.nextPage()
                                                dragAmount > 50 -> horizontalReaderState.prevPage()
                                            }
                                        }
                                    }
                                },
                            bitmap = it.content.asImageBitmap(),
                            contentDescription = ""
                        )
                    }

                }
            }
        }
}

@Composable
fun BlankPage(
    modifier: Modifier = Modifier,
    width: Int,
    height: Int
) {
    Box(
        modifier = modifier
            .size(
                width = width.dp,
                height = height.dp
            )
            .background(color = Color.White)
    )
}