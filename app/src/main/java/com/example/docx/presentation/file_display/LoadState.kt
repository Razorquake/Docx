package com.example.docx.presentation.file_display

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class LoadState {
    val getErrorMessage
        get() = if(this is DocumentError) error?.message.toString() else null
    @Parcelize
    object NoDocument : LoadState(), Parcelable

    @Parcelize
    object DocumentLoading : LoadState(),Parcelable
    @Parcelize
    object DocumentImporting : LoadState(),Parcelable
    @Parcelize
    object DocumentLoaded : LoadState(),Parcelable
    @Parcelize
    data class DocumentError(val error: Throwable?) : LoadState(),Parcelable
}