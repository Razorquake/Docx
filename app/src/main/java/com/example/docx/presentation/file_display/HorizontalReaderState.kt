package com.example.docx.presentation.file_display

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import java.io.File

class HorizontalReaderState(
    resource: ResourceType
) : ReaderState(resource) {
    internal var pagerState = VuePagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        updatedPageCount = { pdfPageCount })

    override suspend fun nextPage() {
        pagerState.animateScrollToPage(pagerState.currentPage + 1)
    }

    override suspend fun prevPage() {
        pagerState.animateScrollToPage(pagerState.currentPage - 1)
    }

    override fun rotate(angle: Float) {
        renderer?.pageLists?.get(pagerState.currentPage)?.apply {
            rotation += angle % 360F
            refresh()
        }
    }

    override val currentPage: Int
        get() = pagerState.currentPage + 1
    override val isScrolling: Boolean
        get() = pagerState.isScrollInProgress
    override val TAG: String
        get() = "HorizontalVueReader"


    override fun load(
        context: Context,
        coroutineScope: CoroutineScope,
        containerSize: IntSize,
        isPortrait: Boolean,
        customResource: (suspend CoroutineScope.() -> File)?
    ) {
        this.containerSize = containerSize
        this.isPortrait = isPortrait
        loadResource(
            context = context,
            coroutineScope = coroutineScope,
            loadCustomResource = customResource
        )
    }

    companion object {
        val Saver: Saver<HorizontalReaderState, *> = listSaver(
            save = {
                it.importJob?.cancel()
                val resource =
                    it.file?.let { file ->
                        if (it.vueResource is ResourceType.BlankDocument && !it.isDocumentModified)
                            ResourceType.BlankDocument(file.toUri())
                        else
                            ResourceType.Local(
                                file,
                                it.getResourceType()
                            )
                    } ?: it.vueResource

                buildList {
                    add(resource)
                    add(it.importFile?.absolutePath)
                    add(it.pagerState.currentPage)
                    if (it.vueLoadState is LoadState.DocumentImporting)
                        add(it.vueLoadState)
                    else
                        add(LoadState.DocumentLoading)
                    add(it.vueImportState)
                    add(it.mDocumentModified)
                    add(it.cache)
                }.toList()
            },
            restore = {
                HorizontalReaderState(it[0] as ResourceType).apply {
                    //Restore file path
                    importFile = if (it[1] != null) File(it[1] as String) else null
                    //Restore Pager State
                    pagerState = VuePagerState(
                        initialPage = it[2] as Int,
                        initialPageOffsetFraction = 0F,
                        updatedPageCount = { pdfPageCount })
                    //Restoring in case it was in importing state
                    vueLoadState = it[3] as LoadState
                    //To resume importing on configuration change
                    vueImportState = it[4] as ImportState
                    //Restore document modified flag
                    mDocumentModified = it[5] as Boolean
                    //Restore cache value
                    cache = it[6] as Int
                }
            }
        )
    }
}

@Composable
fun rememberHorizontalVueReaderState(
    resource: ResourceType,
    cache: Int = 0
): HorizontalReaderState {
    return rememberSaveable(saver = HorizontalReaderState.Saver) {
        HorizontalReaderState(resource).apply { this.cache = cache }
    }
}