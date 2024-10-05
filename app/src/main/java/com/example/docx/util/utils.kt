package com.example.docx.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.docx.domain.PdfEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

fun copyPdfFileToAppDirectory(context: Context, pdfUri: Uri){
    val time = LocalDateTime.now()
    val fileName = "Docx ${time.format(
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
    )}.pdf"
    val inputStream = context.contentResolver.openInputStream(pdfUri)
    val outputFile = File(context.filesDir, fileName)
    FileOutputStream(outputFile).use { outputStream ->
        inputStream?.copyTo(outputStream)
    }
    setFileDate(context, fileName, time)
}
fun getFileUri(context: Context, fileName: String): Uri {
    val file = File(context.filesDir, fileName)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
}

fun getFile(context: Context, fileName: String): File {
    return File(context.filesDir, fileName)
}

fun getFileDate(context: Context, fileName: String): LocalDateTime {
    val file = File(context.filesDir, fileName)
    val attr: BasicFileAttributes = Files.readAttributes(file.toPath(), BasicFileAttributes::class.java)
    return LocalDateTime.ofInstant(attr.lastModifiedTime().toInstant(), ZoneId.systemDefault())
}
fun setFileDate(context: Context, fileName: String, date: LocalDateTime) {
    val file = File(context.filesDir, fileName)
    val newLastModifiedDate: FileTime = FileTime.from(date.atZone(ZoneId.systemDefault()).toInstant())
    Files.setLastModifiedTime(file.toPath(), newLastModifiedDate)
}

fun deleteFile(context: Context, fileName: String): Boolean {
    val file = File(context.filesDir, fileName)
    return file.deleteRecursively()
}

fun renameFile(context: Context, oldFileName: String, newFileName: String) {
    val oldFile = File(context.filesDir, oldFileName)
    if (oldFile.exists()) {
        val newFile = File(context.filesDir, "$newFileName.${oldFile.extension}")
        if (oldFile.renameTo(newFile)) {
            setFileDate(context, "$newFileName.pdf", LocalDateTime.now())
        } else {
            // Handle failure to rename the file
            Toast.makeText(context, "Failed to rename file", Toast.LENGTH_SHORT).show()
        }
    } else {
        // Handle file not found
        Toast.makeText(context, "File not found", Toast.LENGTH_SHORT).show()
    }
}



fun getPdfPageCount(file: File): Int {
    var pdfRenderer: PdfRenderer? = null
    try {
        val parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pdfRenderer = PdfRenderer(parcelFileDescriptor)
        return pdfRenderer.pageCount
    } catch (e: Exception) {
        // Handle exceptions like FileNotFoundException
        e.printStackTrace()
    } finally {
        pdfRenderer?.close()
    }
    // Handle pre-Lollipop devices or error cases
    return -1
}


fun loadPdfsFromDirectory(context: Context): List<PdfEntity> {
    val directory = context.filesDir
    val pdfFiles = directory.listFiles { _, name -> name.endsWith(".pdf") }
    return pdfFiles?.map { file ->
            PdfEntity(
                UUID.randomUUID().toString(),
                file.name,
                if(file.length()>(1024*1024)) "${"%.2f".format(file.length().toFloat()/1024/1024)}MB" else "${file.length() / 1024}KB",
                getFileDate(context, file.name),
                pages = getPdfPageCount(file) // get the actual number of pages
            )
    } ?: emptyList()
}


internal suspend fun addImageToPdf(
    imageFilePath: String? = null,
    bitmap: Bitmap? = null,
    pdfPath: String
) {
    withContext(Dispatchers.IO) {
        if (imageFilePath.isNullOrEmpty() && bitmap == null)
            throw Exception("Image file or bitmap required")
        val pdfFile = File.createTempFile("temp", ".pdf")
        val actualFile = File(pdfPath)
        if(actualFile.exists() && actualFile.length() != 0L)
            actualFile.copyTo(pdfFile, true)
        val pdfDocument = PdfDocument()
        val options = BitmapFactory.Options()
        val image = if (!imageFilePath.isNullOrEmpty()) BitmapFactory.decodeFile(
            imageFilePath,
            options
        ) else bitmap!!

        if (!actualFile.exists() || actualFile.length() == 0L){
            val pageInfo = PdfDocument.PageInfo.Builder(image.width, image.height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawBitmap(image, 0f, 0f, null)
            pdfDocument.finishPage(page)
            // Save the changes to the existing or new PDF document
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            image.recycle()
            if (isActive) {
                pdfFile.copyTo(actualFile, true)
            }
            return@withContext
        }
        val mrenderer = PdfRenderer(
            ParcelFileDescriptor.open(
                pdfFile,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )

        for (i in 0 until mrenderer.pageCount) {
            val originalPage = mrenderer.openPage(i)

            // Create a new bitmap to draw the contents of the original page onto
            val pageBitmap = Bitmap.createBitmap(
                originalPage.width,
                originalPage.height,
                Bitmap.Config.ARGB_8888
            )
            // Draw the contents of the original page onto the pageBitmap
            originalPage.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            // Close the original page
            originalPage.close()

            //Create new page for pageBitmap
            val pageInfo =
                PdfDocument.PageInfo.Builder(pageBitmap.width, pageBitmap.height, i + 1)
                    .create()
            val currentPage = pdfDocument.startPage(pageInfo)

            val mCanvas = currentPage.canvas


            // Draw the pageBitmap onto the canvas of the existing page
            mCanvas.drawBitmap(pageBitmap, 0f, 0f, null)
            pageBitmap.recycle()
            pdfDocument.finishPage(currentPage)
            yield()
        }

        // Create a new page in the existing
        val pageCount = mrenderer.pageCount
        val newPage = pdfDocument.startPage(
            PdfDocument.PageInfo.Builder(
                image.width,
                image.height,
                pageCount + 1
            ).create()
        )
        val canvas = newPage.canvas
        // Draw the image on the canvas of the new page
        canvas.drawBitmap(image, 0f, 0f, null)

        // Finish the new page
        pdfDocument.finishPage(newPage)

        // Save the changes to the existing or new PDF document
        val outputStream = FileOutputStream(pdfFile)
        pdfDocument.writeTo(outputStream)
        if (isActive) {
            pdfFile.copyTo(actualFile, true)
        }
        // Close the PDF document and PDF renderer
        pdfDocument.close()
        mrenderer.close()
        image.recycle()
    }
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.pinchToZoomAndDrag() = composed {
    val angle by remember { mutableStateOf(0f) }
    var zoom by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    val PI = 3.14
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value * 1.2f
    val screenHeight = configuration.screenHeightDp.dp.value * 1.2f
    DisposableEffect(key1 = Unit, effect ={
        onDispose {
            zoom = 1f
            offsetX = 0f
            offsetY = 0f
        }
    })
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = {},
        onDoubleClick = {
            zoom = if (zoom > 1f) 1f
            else 3f
        }
    )
        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        .graphicsLayer(
            scaleX = zoom,
            scaleY = zoom,
            rotationZ = angle,
        )
        .pointerInput(Unit) {
            detectTransformGestures(
                onGesture = { _, pan, gestureZoom, _ ->
                    zoom = (zoom * gestureZoom).coerceIn(1F..4F)
                    if (zoom > 1) {
                        val x = (pan.x * zoom)
                        val y = (pan.y * zoom)
                        val angleRad = angle * PI / 180.0

                        offsetX =
                            (offsetX + (x * cos(angleRad) - y * sin(angleRad)).toFloat()).coerceIn(
                                -(screenWidth * zoom)..(screenWidth * zoom)
                            )
                        offsetY =
                            (offsetY + (x * sin(angleRad) + y * cos(angleRad)).toFloat()).coerceIn(
                                -(screenHeight * zoom)..(screenHeight * zoom)
                            )
                    } else {
                        offsetX = 0F
                        offsetY = 0F
                    }
                }
            )
        }
}
internal suspend fun mergePdf(oldPdfPath: String, importedPdfPath: String) {
    withContext(Dispatchers.IO) {
        val tempOldPdf = File.createTempFile("temp_old", ".pdf")
        val importedPdf = File(importedPdfPath)
        File(oldPdfPath).copyTo(tempOldPdf, true)
        val pdfDocument = PdfDocument()
        var pdfDocumentPage = 1

        val oldRenderer = PdfRenderer(
            ParcelFileDescriptor.open(
                tempOldPdf,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )
        val newRenderer = PdfRenderer(
            ParcelFileDescriptor.open(
                importedPdf,
                ParcelFileDescriptor.MODE_READ_ONLY
            )
        )

        //Load old pdf pages
        for (i in 0 until oldRenderer.pageCount) {
            val originalPage = oldRenderer.openPage(i)

            // Create a new bitmap to draw the contents of the original page onto
            val bitmap = Bitmap.createBitmap(
                originalPage.width,
                originalPage.height,
                Bitmap.Config.ARGB_8888
            )
            // Draw the contents of the original page onto the bitmap
            originalPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            // Close the original page
            originalPage.close()

            //Create new page for bitmap
            val pageInfo =
                PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pdfDocumentPage)
                    .create()
            val currentPage = pdfDocument.startPage(pageInfo)

            val mCanvas = currentPage.canvas


            // Draw the bitmap onto the canvas of the existing page
            mCanvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            pdfDocument.finishPage(currentPage)
            pdfDocumentPage += 1
            yield()
        }
        //Load new pdf pages
        for (i in 0 until newRenderer.pageCount) {
            val originalPage = newRenderer.openPage(i)

            // Create a new bitmap to draw the contents of the original page onto
            val bitmap = Bitmap.createBitmap(
                originalPage.width,
                originalPage.height,
                Bitmap.Config.ARGB_8888
            )
            // Draw the contents of the original page onto the bitmap
            originalPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            // Close the original page
            originalPage.close()

            //Create new page for bitmap
            val pageInfo =
                PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, pdfDocumentPage)
                    .create()
            val currentPage = pdfDocument.startPage(pageInfo)

            val mCanvas = currentPage.canvas


            // Draw the bitmap onto the canvas of the existing page
            mCanvas.drawBitmap(bitmap, 0f, 0f, null)
            bitmap.recycle()
            pdfDocument.finishPage(currentPage)
            pdfDocumentPage += 1
            yield()
        }
        val outputStream = FileOutputStream(tempOldPdf)
        pdfDocument.writeTo(outputStream)
        if (isActive) {
            tempOldPdf.copyTo(File(oldPdfPath), true)
        }
        // Close the PDF document and PDF renderer
        pdfDocument.close()
        oldRenderer.close()
        newRenderer.close()
        //delete the temp file
        tempOldPdf.delete()
    }
}
fun File.share(context: Context) {
    if (exists()) {
        //call share intent to share file
        val sharingIntent = Intent(Intent.ACTION_SEND)
        val uri = FileProvider.getUriForFile(
            context,
            context.applicationContext.packageName + ".provider",
            this
        )
        if (this.name.contains("pdf"))
            sharingIntent.type = "application/pdf"
        else
            sharingIntent.type = "image/*"
        sharingIntent.putExtra(Intent.EXTRA_STREAM, uri)
        sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(
            Intent.createChooser(
                sharingIntent,
                "Share via"
            )
        )
    }
}
public fun deleteMlkitDocscanUiClientDirectory(cacheDir: File) {
    val directory = File(cacheDir, "mlkit_docscan_ui_client")
    if (directory.exists() && directory.isDirectory) {
        directory.deleteRecursively()
    }
}