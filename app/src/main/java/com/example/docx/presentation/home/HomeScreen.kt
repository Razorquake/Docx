package com.example.docx.presentation.home

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.docx.R
import com.example.docx.presentation.dialogs.MoreDialog
import com.example.docx.presentation.dialogs.RenameDialog
import com.example.docx.presentation.home.components.PdfList
import com.example.docx.util.copyPdfFileToAppDirectory
import com.example.docx.util.deleteMlkitDocscanUiClientDirectory
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeState,
    event: (HomeEvent)-> Unit,
    navigateToPdfReader: (File) -> Unit
) {
    val activity = LocalContext.current as Activity
    val context = LocalContext.current
    val scanner = remember {
        GmsDocumentScanning.getClient(
            GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(true)
                .setResultFormats(
                    GmsDocumentScannerOptions.RESULT_FORMAT_PDF,
                    GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
                )
                .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
        )
    }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()) {result ->
        if (result.resultCode == RESULT_OK){
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pdf?.let {
                copyPdfFileToAppDirectory(
                    context = context,
                    pdfUri = it.uri,
                )
                event(HomeEvent.AddPdf)
                deleteMlkitDocscanUiClientDirectory(context.cacheDir)
            }
        }
    }
    MoreDialog(event = event, state = state)
    RenameDialog(event = event, state = state)
    val pdfList by state.pdfs.collectAsState(initial = emptyList())
    LaunchedEffect(key1 = pdfList) {
            event(HomeEvent.RefreshPdfs)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Docx",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener {
                        scannerLauncher.launch(
                            IntentSenderRequest.Builder(it)
                                .build()
                        )
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            it.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }) {
                Icon(painter = painterResource(id = R.drawable.outline_document_scanner_24), contentDescription = null)
            }
        }
    ) {
        PdfList(
            pdfList = pdfList,
            modifier = Modifier.padding(it).fillMaxSize(),
            event = event,
            onClick = {file ->
                navigateToPdfReader(file)
            }
        )
    }
}