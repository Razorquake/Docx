package com.example.docx.presentation.home

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.docx.util.deleteFile
import com.example.docx.util.getFileUri
import com.example.docx.util.loadPdfsFromDirectory
import com.example.docx.util.renameFile

class HomeViewModel(private val context: Context) : ViewModel() {
    private val pdfs = mutableStateOf(loadPdfsFromDirectory(context))
    private val _state = mutableStateOf(HomeState(pdfs = pdfs.value))
    val state: State<HomeState> = _state
    private fun showDialog(){
        _state.value = _state.value.copy(
            isDialogOpen = true
        )
    }
    private fun hideDialog(){
        _state.value = _state.value.copy(
            isDialogOpen = false
        )
    }
    fun onEvent(event: HomeEvent){
        when(event){
            is HomeEvent.ShowDialog -> {
                showDialog()
            }
            is HomeEvent.HideDialog -> {
                hideDialog()
            }
            is HomeEvent.AddPdf -> {
                val updatedPdfs = loadPdfsFromDirectory(context) // reload the list of PDFs
                _state.value = _state.value.copy(pdfs = updatedPdfs)
            }
            is HomeEvent.ShowRenameDialog -> {
                _state.value = _state.value.copy(
                    isRenameDialogOpen = true
                )
            }
            is HomeEvent.HideRenameDialog -> {
                _state.value = _state.value.copy(
                    isRenameDialogOpen = false
                )
            }
            is HomeEvent.SetSelectedPdf -> {
                // Rename the PDF
                _state.value = _state.value.copy(
                    selectedPdf = event.pdf
                )
            }
            is HomeEvent.DeletePdf -> {
                if(deleteFile(context, event.pdf.name)) {
                    val updatedPdfs = loadPdfsFromDirectory(context) // reload the list of PDFs
                    _state.value = _state.value.copy(pdfs = updatedPdfs)
                    // Delete the PDF
                    _state.value = _state.value.copy(
                        selectedPdf = null
                    )
                }
                else {}
            }
            is HomeEvent.RenamePdf -> {
                // Rename the PDF
                if (event.pdf.name != event.newName){
                    renameFile(context, event.pdf.name, event.newName)
                    val updatedPdfs = loadPdfsFromDirectory(context) // reload the list of PDFs
                    _state.value = _state.value.copy(pdfs = updatedPdfs)
                }
                else {}

                _state.value = _state.value.copy(
                    selectedPdf = null,
                    isRenameDialogOpen = false
                )
            }
            is HomeEvent.SharePdf -> {
                val fileUri = getFileUri(context, event.pdf.name)
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.type = "application/pdf"
                shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)
                shareIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.startActivity(Intent.createChooser(shareIntent, "Share PDF"))

            }
        }
    }
}