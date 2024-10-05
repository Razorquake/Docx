package com.example.docx.presentation.file_display

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class Renderer(
    private val fileDescriptor: ParcelFileDescriptor,
    val containerSize: IntSize,
    val isPortrait: Boolean,
    val cache:Int,
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    val pageLists: List<Page> = List(pdfRenderer.pageCount) {
        Page(
            mutex = mutex,
            index = it,
            pdfRenderer = pdfRenderer,
            coroutineScope = coroutineScope,
            containerSize = containerSize,
            isPortrait = isPortrait
        )
    }

    fun close() {
        coroutineScope.launch {
            pageLists.forEach {
                it.job?.cancelAndJoin()
                it.recycle()
            }
            pdfRenderer.close()
            fileDescriptor.close()
        }
    }

    fun loadWithCache(currentPage: Int) {
        val cacheRange = (((currentPage - cache).coerceIn(0, currentPage))..((currentPage + cache).coerceIn(
            currentPage,
            pageCount
        )))

        pageLists.forEachIndexed { index, page ->
            if(cacheRange.contains(index))
                page.load()
            else
                page.recycle()
        }
    }

    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope,
        containerSize: IntSize,
        isPortrait: Boolean
    ) {
        val dimension = pdfRenderer.openPage(index).use {
            if (isPortrait) {
                val h = it.height * (containerSize.width.toFloat() / it.width)
                val dim = Dimension(
                    height = h.toInt(),
                    width = containerSize.width
                )
                dim
            } else {
                val w = it.width * (containerSize.height.toFloat() / it.height)
                val dim = Dimension(
                    height = containerSize.height,
                    width = w.toInt()
                )
                dim
            }
        }
        var rotation: Float = 0F
        var job: Job? = null

        val stateFlow = MutableStateFlow<PageState>(
            PageState.BlankState(
                width = dimension.width,
                height = dimension.height
            )
        )

        var isLoaded = false

        fun load() {
            if (!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        var newBitmap: Bitmap
                        pdfRenderer.openPage(index).use { currentPage ->
                            newBitmap = createBlankBitmap(
                                width = dimension.width,
                                height = dimension.height
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        }
                        if (rotation != 0F) {
                            val matrix = Matrix().apply {
                                postRotate(rotation)
                            }
                            val rotatedBitmap = Bitmap.createBitmap(
                                newBitmap,
                                0,
                                0,
                                newBitmap.width,
                                newBitmap.height,
                                matrix,
                                true
                            )
                            newBitmap.recycle()
                            isLoaded = true
                            stateFlow.emit(PageState.LoadedState(rotatedBitmap))
                        } else {
                            isLoaded = true
                            stateFlow.emit(PageState.LoadedState(newBitmap))
                        }
                    }

                }
            }
        }

        fun recycle() {
            isLoaded = false
            val oldBitmap = stateFlow.value as? PageState.LoadedState
            stateFlow.tryEmit(
                PageState.BlankState(
                    width = dimension.width,
                    height = dimension.height
                )
            )
            oldBitmap?.content?.recycle()
        }

        fun refresh() {
            stateFlow.tryEmit(
                PageState.BlankState(
                    width = dimension.width,
                    height = dimension.height
                )
            )
            isLoaded = false
            val oldBitmap = stateFlow.value as? PageState.LoadedState
            oldBitmap?.content?.recycle()
            load()
        }

        private fun createBlankBitmap(
            width: Int,
            height: Int
        ): Bitmap {
            return Bitmap.createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawColor(android.graphics.Color.WHITE)
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }

        data class Dimension(
            val height: Int,
            val width: Int
        )
    }
}
