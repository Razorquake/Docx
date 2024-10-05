package com.example.docx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.docx.presentation.home.HomeScreen
import com.example.docx.presentation.home.HomeViewModel
import com.example.docx.presentation.navigator.Navigator
import com.example.docx.ui.theme.DocxTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen().apply {
            setKeepOnScreenCondition{
                viewModel.splashCondition
            }
        }
//        val options = GmsDocumentScannerOptions.Builder()
//            .setGalleryImportAllowed(true)
//            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
//            .setResultFormats(
//                GmsDocumentScannerOptions.RESULT_FORMAT_JPEG,
//                GmsDocumentScannerOptions.RESULT_FORMAT_PDF
//            )
//            .build()
//        val scanner = GmsDocumentScanning.getClient(options)
        setContent {
            DocxTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Navigator(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

