package com.gtkim.pexelssearch.ui.search

import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Photo

data class SearchUiState(
    val query: String = "",
    val photos: List<Photo> = emptyList(),
    val isLoading: Boolean = false,
    val error: PhotoError? = null,
)

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data class PhotoClicked(val photoId: Long) : SearchIntent
    data object LoadMore : SearchIntent
    data object Retry : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val photoId: Long) : SearchEffect
}
