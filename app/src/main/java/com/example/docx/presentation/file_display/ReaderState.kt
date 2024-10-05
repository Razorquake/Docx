package com.example.docx.presentation.file_display

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.core.net.toFile
import androidx.core.net.toUri
import com.example.docx.util.addImageToPdf
import com.example.docx.util.deleteMlkitDocscanUiClientDirectory
import com.example.docx.util.mergePdf
import com.example.docx.util.share
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

abstract class ReaderState (
    val vueResource: ResourceType
) {
    abstract val TAG: String

    //View State
    var vueLoadState by mutableStateOf<LoadState>(LoadState.DocumentLoading)

    //Import State
    internal var vueImportState by mutableStateOf<ImportState>(ImportState.Ideal())

    //Document modified flag
    internal var mDocumentModified by mutableStateOf(false)
    val isDocumentModified
        get() = mDocumentModified

    //Import Job
    internal var importJob: Job? = null

    //Renderer
    internal var renderer: Renderer? = null

    //Load with cache
    var cache: Int = 0

    //Container size
    internal var containerSize: IntSize? = null

    //Device orientation
    internal var isPortrait: Boolean = true

    //PDF File
    private var mFile by mutableStateOf<File?>(null)

    val file: File?
        get() = mFile

    internal var importFile: File? = null

    //Remote download status
    private var mLoadPercent by mutableStateOf(0)

    val loadPercent: Int
        get() = mLoadPercent

    val pdfPageCount: Int
        get() = renderer?.pageCount ?: 0

    abstract val currentPage: Int

    abstract val isScrolling: Boolean
    abstract suspend fun nextPage()
    abstract suspend fun prevPage()

    abstract fun load(
        context: Context,
        coroutineScope: CoroutineScope,
        containerSize: IntSize,
        isPortrait: Boolean,
        customResource: (suspend CoroutineScope.() -> File)?
    )

    internal fun loadResource(
        context: Context,
        coroutineScope: CoroutineScope,
        loadCustomResource: (suspend CoroutineScope.() -> File)?
    ) {
        if (vueLoadState is LoadState.DocumentImporting) {
            require(vueResource is ResourceType.Local || vueResource is ResourceType.BlankDocument)
            if (vueResource is ResourceType.Local)
                mFile = vueResource.file
            if (vueResource is ResourceType.BlankDocument)
                mFile = vueResource.uri!!.toFile()
            requireNotNull(value = mFile, lazyMessage = { "Could not restore file" })
            return
        }

        vueLoadState = if (vueResource is ResourceType.BlankDocument)
            LoadState.NoDocument
        else
            LoadState.DocumentLoading

        mLoadPercent = 0
        when (vueResource) {
            is ResourceType.BlankDocument -> {
                coroutineScope.launch(Dispatchers.IO) {
                    runCatching {
                        val blankFile = File(context.filesDir, "Docx ${LocalDateTime.now().format(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        )}")
                        mFile = vueResource.uri?.toFile() ?: blankFile
                    }.onFailure {
                        vueLoadState = LoadState.DocumentError(it)
                    }
                }
            }

            is ResourceType.Asset -> {
            }

            is ResourceType.Local -> {
                coroutineScope.launch(Dispatchers.IO) {
                    runCatching {
                        with(context) {
                            grantUriPermission(
                                packageName,
                                vueResource.file.toUri(),
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        }
                        mFile = vueResource.file

                        initRenderer()
                    }.onFailure {
                        vueLoadState = LoadState.DocumentError(it)
                    }
                }
            }

            is ResourceType.Remote -> {
            }

            is ResourceType.Custom -> {
                coroutineScope.launch(Dispatchers.IO) {
                    runCatching {
                        requireNotNull(loadCustomResource,
                            lazyMessage = { "Custom resource method cannot be null" })
                        val customFile = loadCustomResource()
                        val _file = File(context.filesDir, "Docx ${LocalDateTime.now().format(
                            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                        )}")
                        customFile.copyTo(_file, true)
                        mFile = _file
                        initRenderer()
                    }.onFailure {
                        vueLoadState = LoadState.DocumentError(it)
                    }
                }
            }
        }
    }
    private fun initRenderer() {
        requireNotNull(containerSize)
        renderer = Renderer(
            fileDescriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_READ_ONLY
            ),
            containerSize = containerSize!!,
            isPortrait = isPortrait,
            cache = cache
        )
        vueLoadState = LoadState.DocumentLoaded
    }

    /**
     * Helper to launch import intent
     * the file manager will enable importing of other file types as well.
     * */

    /**
     * Intent launcher for importing
     * */
    @Composable
    fun getImportLauncher(interceptResult: suspend (File) -> Unit = {}): () -> Unit {
        val activity = LocalContext.current as Activity
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()

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
            contract = ActivityResultContracts.StartIntentSenderForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
                scanningResult?.let { gmsResult ->
                    coroutineScope.launch(Dispatchers.IO) {
                        var scannedFile: File? = null
                        try {
                            scannedFile = when {
                                gmsResult.pdf != null -> {
                                    vueLoadState = LoadState.DocumentImporting
                                    renderer?.close()
                                    val pdfFile = File(context.cacheDir, "scanned_document.pdf")
                                    context.contentResolver.openInputStream(gmsResult.pdf!!.uri)?.use { input ->
                                        pdfFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    pdfFile
                                }
                                gmsResult.pages!!.isNotEmpty() -> {
                                    vueLoadState = LoadState.DocumentImporting
                                    renderer?.close()
                                    val imageFile = File(context.cacheDir, "scanned_document.jpg")
                                    context.contentResolver.openInputStream(gmsResult.pages!!.first().imageUri)?.use { input ->
                                        imageFile.outputStream().use { output ->
                                            input.copyTo(output)
                                        }
                                    }
                                    imageFile
                                }
                                else -> null
                            }

                            scannedFile?.let { file ->
                                interceptResult(file)
                                if (file.extension.lowercase() == "pdf") {
                                    if (this@ReaderState.file != null && !this@ReaderState.file!!.exists() && this@ReaderState.file!!.length() == 0L) {
                                        file.copyTo(this@ReaderState.file!!, true)
                                    } else {
                                        mergePdf(
                                            oldPdfPath = this@ReaderState.file!!.absolutePath,
                                            importedPdfPath = file.absolutePath
                                        )
                                    }
                                } else {
                                    addImageToPdf(
                                        imageFilePath = file.absolutePath,
                                        pdfPath = this@ReaderState.file!!.absolutePath
                                    )
                                }
                                initRenderer()
                                vueImportState = ImportState.Ideal()
                                mDocumentModified = true
                                importFile = null
                            }
                        } catch (e: Exception) {
                            vueLoadState = LoadState.DocumentError(e)
                            importFile = null
                            vueImportState = ImportState.Ideal()
                        } finally {
                            // Clean up the scanned file from cache
                            scannedFile?.deleteRecursively()
                            deleteMlkitDocscanUiClientDirectory(context.cacheDir)
                        }
                    }
                }
            } else {
                vueLoadState = if (vueResource !is ResourceType.BlankDocument)
                    LoadState.DocumentLoaded
                else
                    LoadState.NoDocument
            }
        }

        return {

            scanner.getStartScanIntent(activity)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        context,
                        exception.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    /**
     * Helper intent creator for importing pdf or image from gallery/camera
     */

    fun sharePDF(context: Context) {
        file?.share(context)
    }

    abstract fun rotate(angle: Float)

    internal fun getResourceType() =
        when (vueResource) {
            is ResourceType.Asset -> vueResource.fileType
            is ResourceType.Local -> vueResource.fileType
            is ResourceType.Remote -> vueResource.fileType
            is ResourceType.BlankDocument,
            ResourceType.Custom -> VueFileType.PDF
        }
}


/**
 * Function to recursively compress an image until its size is around threshold
 * Must be called after image has been rotated because exif data would not be retained
 * */

fun File.compressImageToThreshold(threshold: Int) {
    if (exists()) {
        val tempFile = File.createTempFile("tempCompress", ".$extension")
        copyTo(tempFile, true)
        var quality = 100 // Initial quality setting
        var currentSize = tempFile.length()

        while (currentSize > (threshold * 1024 * 1024)) { // 2MB in bytes
            quality -= 5 // Reduce quality in steps of 5
            if (quality < 0) {
                break // Don't reduce quality below 0
            }

            // Compress the image and get its new size
            currentSize = tempFile.compressImage(quality)
        }

        tempFile.copyTo(this,true)
        tempFile.delete()
    }
}
/**
 * Get file from Uri
 * */
@Deprecated("Use toFile() to get file from uri")
internal fun Uri.getFile(mContext: Context): File {
    val inputStream = mContext.contentResolver?.openInputStream(this)
    var file: File
    inputStream.use { input ->
        file =
            File(mContext.cacheDir, System.currentTimeMillis().toString() + ".pdf")
        FileOutputStream(file).use { output ->
            val buffer =
                ByteArray(4 * 1024) // or other buffer size
            var read: Int = -1
            while (input?.read(buffer).also {
                    if (it != null) {
                        read = it
                    }
                } != -1) {
                output.write(buffer, 0, read)
            }
            output.flush()
        }
    }
    return file
}

/**
 * Copy Uri to another Uri
 * */
// Function to compress an image and return its size
internal fun File.compressImage(quality: Int): Long {
    try {
        val bitmap = BitmapFactory.decodeFile(absolutePath)

        val outputStream = FileOutputStream(this)
        // Compress the bitmap with the specified quality (0-100)
        if (absolutePath.contains("jpg") || absolutePath.contains("jpeg") || absolutePath.contains("JPG") || absolutePath.contains(
                "JPEG"
            )
        )
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        else
            bitmap.compress(Bitmap.CompressFormat.PNG, quality, outputStream)

        outputStream.flush()
        outputStream.close()

        // Return the size of the compressed image
        return length()
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return 0
}

