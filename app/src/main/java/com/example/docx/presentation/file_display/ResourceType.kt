package com.example.docx.presentation.file_display

import android.net.Uri
import android.os.Parcelable
import androidx.annotation.RawRes
import kotlinx.parcelize.Parcelize
import java.io.File

@Parcelize
enum class VueFileType: Parcelable {
    PDF,IMAGE,BASE64
}
sealed class ResourceType{

    /**
     * @param uri If null then internally an empty file would be create otherwise param uri will be used as file
     * */
    @Parcelize
    data class BlankDocument(val uri:Uri? = null): ResourceType(), Parcelable

    /**
     * @param uri Source file uri
     * @param fileType Source file type
     * @see VueFileType
     */
    @Parcelize
    data class Local(val file: File, val fileType:VueFileType = VueFileType.PDF) : ResourceType(), Parcelable

    /**
     * @param url Source file url (Method type GET)
     * @param fileType Source file type
     * @see VueFileType
     * @param headers Headers if required when fetching from url
     */
    @Parcelize
    data class Remote(
        val url: String,
        val headers: HashMap<String,String> = hashMapOf(),
        val fileType:VueFileType
    ) : ResourceType(), Parcelable

    /**
     * @param assetId Source asset id
     * @param fileType Source file type
     * @see VueFileType
     */
    @Parcelize
    data class Asset(@RawRes val assetId: Int, val fileType:VueFileType) : ResourceType(), Parcelable

    @Parcelize
    data object Custom : ResourceType(), Parcelable
}
