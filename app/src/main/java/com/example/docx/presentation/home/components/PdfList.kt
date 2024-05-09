package com.example.docx.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.docx.domain.PdfEntity
import com.example.docx.presentation.home.HomeEvent
import java.io.File

@Composable
fun PdfList(
    modifier: Modifier,
    pdfList: List<PdfEntity>,
    event: (HomeEvent) -> Unit,
    onClick: (File) -> Unit
){
    if (pdfList.isEmpty()){
        EmptyScreen(modifier = modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        contentPadding = PaddingValues(6.dp)
    ) {
        items(pdfList.size) { index ->
            val pdf = pdfList[index]
            PdfLayout(
                pdfEntity = pdf,
                event = event,
                onClick = onClick
                )
        }

    }
}