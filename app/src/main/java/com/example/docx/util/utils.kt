package com.example.docx.util

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.docx.domain.PdfEntity
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileTime
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.UUID

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
                "${file.length() / 1024}KB",
                getFileDate(context, file.name),
                pages = getPdfPageCount(file) // get the actual number of pages
            )
    } ?: emptyList()
}