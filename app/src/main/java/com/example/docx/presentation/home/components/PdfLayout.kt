package com.example.docx.presentation.home.components

import android.app.Activity
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.docx.R
import com.example.docx.domain.PdfEntity
import com.example.docx.presentation.home.HomeEvent
import com.example.docx.ui.theme.DocxTheme
import com.example.docx.util.getFile
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun PdfLayout(
    pdfEntity: PdfEntity,
    modifier: Modifier = Modifier,
    event: (HomeEvent) -> Unit,
    onClick: (File) -> Unit
){
    val context = LocalContext.current
//    val activity = LocalContext.current as Activity
    Row(
        modifier = modifier.clickable {
//            val getFileUri = getFileUri(
//                context = context,
//                fileName = pdfEntity.name
//            )
//            val browserIntent = Intent(Intent.ACTION_VIEW, getFileUri)
//            browserIntent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//            activity.startActivity(browserIntent)
            val file = getFile(
                context,
                fileName = pdfEntity.name
            )
            onClick(file)
        }
    ) {
        Image(
            painter = painterResource(id = R.drawable.picture_as_pdf),
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(shape = MaterialTheme.shapes.medium)
        )
        Column(
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .height(96.dp)
        ) {
            Text(
                text = pdfEntity.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colorResource(id = R.color.text_title)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_article_24),
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = colorResource(id = R.color.body)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = pdfEntity.pages.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorResource(id = R.color.body)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = pdfEntity.lastModifiedTime.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorResource(id = R.color.body)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = pdfEntity.size,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorResource(id = R.color.body)
                )
            }
        }
        IconButton(
            onClick = {
                      event(HomeEvent.ShowDialog)
                event(HomeEvent.SetSelectedPdf(pdfEntity))
            },
            modifier = Modifier.align(Alignment.CenterVertically)
        ) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = null,
                tint = colorResource(id = R.color.body),
                )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
fun PdfLayoutPreview(){
    DocxTheme {
        PdfLayout(
            pdfEntity = PdfEntity(
                id = "1",
                name = "Docx 05-05-2023.docx",
                size = "123 KB",
                lastModifiedTime = LocalDateTime.now(),
                pages = 12
            ),
            modifier = Modifier,
            event = {},
            onClick = {}
        )
    }
}

