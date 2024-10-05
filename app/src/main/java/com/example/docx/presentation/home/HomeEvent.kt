package com.example.docx.presentation.home

import com.example.docx.domain.PdfEntity

sealed class HomeEvent {
    data object ShowDialog : HomeEvent()
    data object AddPdf: HomeEvent()
    data object HideDialog : HomeEvent()
    data object ShowRenameDialog : HomeEvent()
    data object HideRenameDialog : HomeEvent()
    data class SetSelectedPdf(val pdf: PdfEntity) : HomeEvent()

    data object RefreshPdfs : HomeEvent()
    data class DeletePdf(val pdf: PdfEntity) : HomeEvent()

    data class RenamePdf(val pdf: PdfEntity, val newName: String) : HomeEvent()
    data class SharePdf(val pdf: PdfEntity) : HomeEvent()
}