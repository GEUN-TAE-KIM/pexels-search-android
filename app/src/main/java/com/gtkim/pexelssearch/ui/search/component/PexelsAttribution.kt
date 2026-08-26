package com.gtkim.pexelssearch.ui.search.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import com.gtkim.pexelssearch.R

private const val LINK_TAG = "Pexels"

/**
 * PexelsのガイドラインはAPIを叩くアプリに「Pexelsへの目立つリンク」を求めるため、
 * 検索結果の有無にかかわらず常に見える位置に置く。
 */
@Composable
fun PexelsAttribution(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(R.string.powered_by_pexels)
    val linkStyles = TextLinkStyles(
        style = SpanStyle(
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
        ),
    )

    Text(
        text = buildAnnotatedString {
            append(label)
            addLink(
                LinkAnnotation.Clickable(tag = LINK_TAG, styles = linkStyles) { onClick() },
                0,
                label.length,
            )
        },
        style = MaterialTheme.typography.bodySmall,
        modifier = modifier,
    )
}
