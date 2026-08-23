package com.gtkim.pexelssearch.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.gtkim.pexelssearch.R
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.ui.common.AppLoadingIndicator
import com.gtkim.pexelssearch.ui.common.PhotoErrorContent
import com.gtkim.pexelssearch.ui.search.component.PhotoGrid
import com.gtkim.pexelssearch.ui.search.component.SearchField
import com.gtkim.pexelssearch.ui.search.component.SearchMessage
import com.gtkim.pexelssearch.ui.theme.PexelsSearchTheme

@Composable
fun SearchScreen(
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: SearchViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    // 停止中に流すと遷移先が受け取れないため、STARTED の間だけ購読する。その間の Effect は Channel が保持する
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    is SearchEffect.NavigateToDetail -> onNavigateToDetail(effect.photoId)
                }
            }
        }
    }

    SearchScaffold(
        state = state,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

@Composable
fun SearchScaffold(
    state: SearchUiState,
    onIntent: (SearchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = state.query,
                onQueryChange = { onIntent(SearchIntent.QueryChanged(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when {
                state.isLoading -> AppLoadingIndicator(modifier = Modifier.fillMaxSize())

                state.error != null -> PhotoErrorContent(
                    error = state.error,
                    onRetry = { onIntent(SearchIntent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )

                state.photos.isNotEmpty() -> PhotoGrid(
                    photos = state.photos,
                    onPhotoClick = { onIntent(SearchIntent.PhotoClicked(it)) },
                    modifier = Modifier.fillMaxSize(),
                )

                state.query.isNotBlank() -> SearchMessage(
                    message = stringResource(R.string.search_empty_message),
                    modifier = Modifier.fillMaxSize(),
                )

                else -> SearchMessage(
                    message = stringResource(R.string.search_idle_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScaffoldPreview() {
    PexelsSearchTheme {
        SearchScaffold(
            state = SearchUiState(query = "猫", photos = previewPhotos),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScaffoldLoadingPreview() {
    PexelsSearchTheme {
        SearchScaffold(
            state = SearchUiState(query = "猫", isLoading = true),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScaffoldEmptyPreview() {
    PexelsSearchTheme {
        SearchScaffold(
            state = SearchUiState(query = "猫"),
            onIntent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SearchScaffoldErrorPreview() {
    PexelsSearchTheme {
        SearchScaffold(
            state = SearchUiState(query = "猫", error = PhotoError.Network),
            onIntent = {},
        )
    }
}

private val previewPhotographers = listOf("山田太郎", "佐藤花子", "鈴木一郎", "高橋美咲")
private val previewColors = listOf(0x6E7B8B, 0x8B6E7B, 0x7B8B6E, 0xB0A38F)

private val previewPhotos = List(8) { index ->
    Photo(
        id = index.toLong(),
        photographer = previewPhotographers[index % previewPhotographers.size],
        photographerUrl = "https://www.pexels.com/@sample",
        pexelsUrl = "https://www.pexels.com/photo/$index/",
        thumbnailUrl = "https://images.pexels.com/photos/$index/medium.jpg",
        fullUrl = "https://images.pexels.com/photos/$index/large2x.jpg",
        avgColorRgb = previewColors[index % previewColors.size],
    )
}
