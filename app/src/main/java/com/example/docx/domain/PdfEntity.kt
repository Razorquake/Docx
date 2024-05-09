package com.example.docx.domain

import java.time.LocalDateTime
import java.util.Date

data class PdfEntity(
    val id: String,
    val name: String,
    val size: String,
    val lastModifiedTime: LocalDateTime,
    val pages: Int
)