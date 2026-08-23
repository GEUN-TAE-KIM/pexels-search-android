package com.gtkim.pexelssearch.ui.search.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.ui.common.AppLoadingIndicator
import com.gtkim.pexelssearch.ui.common.PhotoErrorContent

private const val LOAD_MORE_THRESHOLD = 6

/**
 * 末尾が近づくと [onLoadMore] を発火する。[loadMoreError] は既読分を残したままフッターで知らせる
 * 追加読み込みの失敗で、初回検索の失敗は呼び出し側が全画面で扱う。
 */
@Composable
fun PhotoGrid(
    photos: List<Photo>,
    endReached: Boolean,
    isLoadingMore: Boolean,
    loadMoreError: PhotoError?,
    onPhotoClick: (Long) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gridState = rememberLazyGridState()

    // しきい値を跨いだ瞬間だけ true になるよう derivedStateOf でまとめ、LaunchedEffect の再実行を抑える
    val shouldLoadMore by remember(endReached, isLoadingMore, loadMoreError) {
        derivedStateOf {
            val layoutInfo = gridState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: return@derivedStateOf false
            !endReached &&
                !isLoadingMore &&
                loadMoreError == null &&
                lastVisible >= layoutInfo.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        state = gridState,
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items = photos, key = { it.id }) { photo ->
            PhotoGridItem(
                photo = photo,
                onClick = { onPhotoClick(photo.id) },
            )
        }

        if (isLoadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                AppLoadingIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        if (loadMoreError != null) {
            // 再試行は Retry ではなく onLoadMore — スクロール位置と既読分を捨てないため
            item(span = { GridItemSpan(maxLineSpan) }) {
                PhotoErrorContent(
                    error = loadMoreError,
                    onRetry = onLoadMore,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
