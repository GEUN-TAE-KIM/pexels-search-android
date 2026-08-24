package com.gtkim.pexelssearch.ui.detail

import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Photo

data class PhotoDetailUiState(
    val photo: Photo? = null,
    val isLoading: Boolean = false,
    val error: PhotoError? = null,
)

/**
 * 初回ロード用のIntentは置かない。遷移した時点で表示する写真は決まっているため、
 * 取得はViewModelのinitが行う。
 */
sealed interface PhotoDetailIntent {
    data object Retry : PhotoDetailIntent
    data object PhotographerClicked : PhotoDetailIntent
    data object PexelsLinkClicked : PhotoDetailIntent
}

sealed interface PhotoDetailEffect {
    data class OpenUrl(val url: String) : PhotoDetailEffect
}
