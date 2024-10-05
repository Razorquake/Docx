package com.example.docx.presentation.file_display

import android.graphics.Bitmap

sealed interface PageState{
    data class LoadedState(
        val content: Bitmap
    ) : PageState

    data class BlankState(
        val width: Int,
        val height: Int
    ) : PageState
}