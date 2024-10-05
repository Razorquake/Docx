package com.example.docx.presentation.home

import com.example.docx.domain.PdfEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

data class HomeState(
    val isDialogOpen: Boolean = false,
    val isRenameDialogOpen: Boolean = false,
    val pdfs: Flow<List<PdfEntity>> = emptyFlow(),
    val selectedPdf: PdfEntity? = null

)
