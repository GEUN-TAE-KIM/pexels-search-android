package com.gtkim.pexelssearch.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.gtkim.pexelssearch.R
import com.gtkim.pexelssearch.domain.error.PhotoError

/**
 * [PhotoError] は sealed のためこの `when` は網羅的になる。エラー種別を増やすとここがコンパイルエラーになり、
 * 文言の追加漏れを防ぐ。
 */
@Composable
fun PhotoError.toMessage(): String = when (this) {
    PhotoError.Network -> stringResource(R.string.error_network)
    PhotoError.RateLimit -> stringResource(R.string.error_rate_limit)
    PhotoError.Unauthorized -> stringResource(R.string.error_unauthorized)
    PhotoError.Server -> stringResource(R.string.error_server)
    is PhotoError.Unknown -> stringResource(R.string.error_unknown)
}
