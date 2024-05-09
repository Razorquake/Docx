package com.example.docx.presentation.file_display

import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class PdfRender(
    private val fileDescriptor: ParcelFileDescriptor,
) {
    private val pdfRenderer = PdfRenderer(fileDescriptor)
    val pageCount get() = pdfRenderer.pageCount
    private val mutex: Mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.Default+ SupervisorJob())
    val pageList: List<Page> = List(pdfRenderer.pageCount){
        Page(
            mutex = mutex,
            index = it,
            pdfRenderer = pdfRenderer,
            coroutineScope = coroutineScope,
            pdfRender = this
        )
    }
    fun preloadPages(startIndex: Int, count: Int) {
        for (i in startIndex until startIndex + count) {
            if (i < pageCount) {
                pageList[i].load()
            }
        }
    }
    fun close(){
        pageList.forEach {
            it.recycle()
        }
        pdfRenderer.close()
        fileDescriptor.close()
    }
    init {
        // Load the first page in the background
        pageList[0].load()
    }
    class Page(
        val mutex: Mutex,
        val index: Int,
        val pdfRenderer: PdfRenderer,
        val coroutineScope: CoroutineScope,
        val pdfRender: PdfRender
    ) {
        var isLoaded = false

        var job: Job? = null

        val dimension = pdfRenderer.openPage(index).use {currentPage ->
            Dimension(
                width = currentPage.width,
                height = currentPage.height
            )
        }
        fun heightByWidth(width: Int): Int {
            val ratio = dimension.width.toFloat()/ dimension.height
            return (width/ratio).toInt()
        }
        val pageContent = MutableStateFlow<Bitmap?>(null)
        fun load(){
            if(!isLoaded) {
                job = coroutineScope.launch {
                    mutex.withLock {
                        val newBitmap: Bitmap
                        pdfRenderer.openPage(index).use {currentPage ->
                            newBitmap = createBlankBitmap(
                                width = dimension.width,
                                height = heightByWidth(dimension.width)
                            )
                            currentPage.render(
                                newBitmap,
                                null,
                                null,
                                PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY
                            )
                        }
                        isLoaded = true
                        pageContent.emit(newBitmap)
                        pdfRender.preloadPages(index + 1, 3)                    }
                }
            }
        }
        fun recycle() {
            if(index < pdfRenderer.pageCount - 3) {
                isLoaded = false
                val oldBitmap = pageContent.value
                pageContent.tryEmit(null)
                oldBitmap?.recycle()
            }
        }
        private fun createBlankBitmap(
            width: Int,
            height: Int,
        ): Bitmap {
            return createBitmap(
                width,
                height,
                Bitmap.Config.ARGB_8888
            ).apply {
                val canvas = Canvas(this)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(this, 0f, 0f, null)
            }
        }
    }

    data class Dimension(
        val width: Int,
        val height: Int
    )
}