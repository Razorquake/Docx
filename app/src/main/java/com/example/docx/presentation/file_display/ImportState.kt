package com.example.docx.presentation.file_display

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class ImportState{
    @Parcelize
    data class Imported(val uri: Uri?): ImportState(), Parcelable
    @Parcelize
    data class Ideal(private val ideal:Boolean = true) : ImportState(), Parcelable
}