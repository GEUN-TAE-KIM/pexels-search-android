package com.gtkim.pexelssearch.ui.search.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gtkim.pexelssearch.domain.model.Photo

private const val OPAQUE_ALPHA = 0xFF000000

@Composable
fun PhotoGridItem(
    photo: Photo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 読み込み中と失敗時の両方に使う。失敗を空白のままにすると撮影者名だけが浮いた壊れた見た目になる
    val avgColorPainter = ColorPainter(Color(OPAQUE_ALPHA or photo.avgColorRgb.toLong()))

    Column(modifier = modifier.clickable(onClick = onClick)) {
        AsyncImage(
            model = photo.thumbnailUrl,
            // 撮影者名は直下の Text が読み上げるため、画像は装飾扱いにして重複を避ける
            contentDescription = null,
            placeholder = avgColorPainter,
            error = avgColorPainter,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp)),
        )
        Text(
            text = photo.photographer,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
