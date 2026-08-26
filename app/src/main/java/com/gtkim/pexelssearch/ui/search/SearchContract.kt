package com.gtkim.pexelssearch.ui.search

import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Photo

data class SearchUiState(
    val query: String = "",
    val photos: List<Photo> = emptyList(),
    val page: Int = 1,
    val endReached: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasSearched: Boolean = false,
    val error: PhotoError? = null,
)

sealed interface SearchIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object SearchSubmitted : SearchIntent
    data class PhotoClicked(val photoId: Long) : SearchIntent
    data object LoadMore : SearchIntent
    data object Retry : SearchIntent
    data object PexelsLinkClicked : SearchIntent
}

sealed interface SearchEffect {
    data class NavigateToDetail(val photoId: Long) : SearchEffect
    data class OpenUrl(val url: String) : SearchEffect
}
