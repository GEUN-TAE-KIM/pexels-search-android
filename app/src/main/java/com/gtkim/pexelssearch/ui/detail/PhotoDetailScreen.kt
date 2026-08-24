package com.gtkim.pexelssearch.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.net.toUri
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
import com.gtkim.pexelssearch.ui.detail.component.PhotoDetailContent
import com.gtkim.pexelssearch.ui.theme.PexelsSearchTheme
import kotlinx.coroutines.launch

@Composable
fun PhotoDetailScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PhotoDetailViewModel = hiltViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val openFailedMessage = stringResource(R.string.detail_open_url_failed)

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    // ブラウザの有無はUIしか知り得ないため、失敗の判定と通知をこの場で完結させる
                    is PhotoDetailEffect.OpenUrl -> {
                        val opened = runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, effect.url.toUri()))
                        }.isSuccess
                        // showSnackbar は表示が消えるまで suspend するため、次の Effect の収集を止めないよう切り離す
                        if (!opened) launch { snackbarHostState.showSnackbar(openFailedMessage) }
                    }
                }
            }
        }
    }

    PhotoDetailScaffold(
        state = state,
        onIntent = viewModel::onIntent,
        onBack = onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScaffold(
    state: PhotoDetailUiState,
    onIntent: (PhotoDetailIntent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> AppLoadingIndicator(modifier = Modifier.fillMaxSize())

                state.error != null -> PhotoErrorContent(
                    error = state.error,
                    onRetry = { onIntent(PhotoDetailIntent.Retry) },
                    modifier = Modifier.fillMaxSize(),
                )

                state.photo != null -> PhotoDetailContent(
                    photo = state.photo,
                    onPhotographerClick = { onIntent(PhotoDetailIntent.PhotographerClicked) },
                    onPexelsLinkClick = { onIntent(PhotoDetailIntent.PexelsLinkClicked) },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailScaffoldPreview() {
    PexelsSearchTheme {
        PhotoDetailScaffold(
            state = PhotoDetailUiState(photo = previewPhoto),
            onIntent = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailScaffoldLoadingPreview() {
    PexelsSearchTheme {
        PhotoDetailScaffold(
            state = PhotoDetailUiState(isLoading = true),
            onIntent = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PhotoDetailScaffoldErrorPreview() {
    PexelsSearchTheme {
        PhotoDetailScaffold(
            state = PhotoDetailUiState(error = PhotoError.Network),
            onIntent = {},
            onBack = {},
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}

private val previewPhoto = Photo(
    id = 1L,
    photographer = "山田太郎",
    photographerUrl = "https://www.pexels.com/@sample",
    pexelsUrl = "https://www.pexels.com/photo/1/",
    thumbnailUrl = "https://images.pexels.com/photos/1/medium.jpg",
    fullUrl = "https://images.pexels.com/photos/1/large2x.jpg",
    avgColorRgb = 0x6E7B8B,
)
