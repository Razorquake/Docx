package com.example.docx.presentation.home

import com.example.docx.domain.PdfEntity

data class HomeState(
    val isDialogOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val pdfs: List<PdfEntity> = emptyList(),
    val selectedPdf: PdfEntity? = null

)
