package com.gtkim.pexelssearch.ui.detail.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.gtkim.pexelssearch.R
import com.gtkim.pexelssearch.domain.model.Photo

private const val OPAQUE_ALPHA = 0xFF000000
private const val PEXELS_LABEL = "Pexels"

/**
 * 写真を画面いっぱいに収め、その下にPexelsのガイドラインが求めるクレジットを置く。
 */
@Composable
fun PhotoDetailContent(
    photo: Photo,
    onPhotographerClick: () -> Unit,
    onPexelsLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val avgColorPainter = ColorPainter(Color(OPAQUE_ALPHA or photo.avgColorRgb.toLong()))

    Column(modifier = modifier) {
        AsyncImage(
            model = photo.fullUrl,
            // 写真の内容を説明するデータを持たないため、読み上げは直下のクレジットに委ねる
            contentDescription = null,
            placeholder = avgColorPainter,
            error = avgColorPainter,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        )
        PhotoCredit(
            photographer = photo.photographer,
            onPhotographerClick = onPhotographerClick,
            onPexelsLinkClick = onPexelsLinkClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

/**
 * 撮影者名とPexelsをそれぞれ別のリンクにする。開く処理はここでは行わず、
 * 呼び出し側のIntentを通してViewModelに渡す。
 */
@Composable
private fun PhotoCredit(
    photographer: String,
    onPhotographerClick: () -> Unit,
    onPexelsLinkClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val credit = stringResource(R.string.detail_credit, photographer)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )

    Text(
        text = buildAnnotatedString {
            append(credit)
            val photographerEnd =
                addLinkTo(credit, photographer, linkStyles, onClick = onPhotographerClick)
            addLinkTo(credit, PEXELS_LABEL, linkStyles, photographerEnd, onPexelsLinkClick)
        },
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier,
    )
}

/**
 * 書式文字列に差し込まれた語の位置は実行時にしか分からないため、探して見つかった場合だけリンクにする。
 * [from] 以降だけを見るのは、撮影者名が "Pexels" を含むとリンクが重なってしまうため。
 * 戻り値は次に探し始める位置。
 */
private fun AnnotatedString.Builder.addLinkTo(
    source: String,
    label: String,
    styles: TextLinkStyles,
    from: Int = 0,
    onClick: () -> Unit,
): Int {
    val start = source.indexOf(label, from)
    if (start < 0) return from
    val end = start + label.length
    addLink(LinkAnnotation.Clickable(tag = label, styles = styles) { onClick() }, start, end)
    return end
}
