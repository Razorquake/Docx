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
//                    var imageUris by remember {
//                        mutableStateOf<List<Uri>>(emptyList())
//                    }
//                    val scannerLauncher = rememberLauncherForActivityResult(
//                        contract = ActivityResultContracts.StartIntentSenderForResult()
//                    ) {
//                        if (it.resultCode == RESULT_OK) {
//                            val result = GmsDocumentScanningResult.fromActivityResultIntent(it.data)
//                            imageUris = result?.pages?.map { it.imageUri } ?: emptyList()
//                            result?.pdf?.let { pdf ->
//                                val fos = FileOutputStream(File(filesDir, "scan.pdf"))
//                                contentResolver.openInputStream(pdf.uri)?.use { inputStream ->
//                                    inputStream.copyTo(fos)
//                                }
//                            }
//                        }
//                    }
//                    Box(modifier = Modifier.fillMaxSize()) {
//
//                        LazyColumn(
//                            modifier = Modifier.fillMaxSize(),
//                            verticalArrangement = Arrangement.Center,
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            items(imageUris.size) { index ->
//                                AsyncImage(
//                                    model = imageUris[index],
//                                    contentDescription = null,
//                                    contentScale = ContentScale.FillWidth,
//                                    modifier = Modifier.fillMaxWidth()
//                                )
//
//                            }
//
//                        }
//                        FloatingActionButton(onClick = {
//                            scanner.getStartScanIntent(this@MainActivity)
//                                .addOnSuccessListener {
//                                    scannerLauncher.launch(
//                                        IntentSenderRequest.Builder(it)
//                                            .build()
//                                    )
//                                }
//                                .addOnFailureListener {
//                                    Toast.makeText(
//                                        applicationContext,
//                                        it.message,
//                                        Toast.LENGTH_SHORT
//                                    ).show()
//                                }
//                            },
//                            modifier = Modifier.align(Alignment.BottomEnd)
//                                .padding(16.dp),
//                        ) {
//                            Icon(imageVector = Icons.Default.Add, contentDescription = null)
//                        }
//                    }
                    Navigator()
                }
            }
        }
    }
}

