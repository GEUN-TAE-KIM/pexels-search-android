package com.gtkim.pexelssearch.ui.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import com.gtkim.pexelssearch.ui.BaseViewModel
import com.gtkim.pexelssearch.ui.withMinLoadingDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val FIRST_PAGE = 1
private const val KEY_QUERY = "query"

/** Pexelsのガイドラインが求める、APIを叩く画面からの目立つリンク先。応答には含まれない固定値。 */
private const val PEXELS_URL = "https://www.pexels.com"

/** 再試行だけ最低表示時間を与えるため、どのトリガー由来かを検索の流れに乗せる。 */
private data class SearchRequest(val query: String, val isRetry: Boolean)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    private val savedStateHandle: SavedStateHandle,
) : BaseViewModel<SearchUiState, SearchIntent, SearchEffect>(
    SearchUiState(query = savedStateHandle.get<String>(KEY_QUERY).orEmpty()),
) {

    /** プロセス再生成では query だけ復元する。結果とスクロール位置は復元しない。 */
    private val queryFlow = savedStateHandle.getStateFlow(KEY_QUERY, "")

    private val submitTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 表示中の結果を生んだクエリ。入力途中の [queryFlow] と混ざらないよう、追加読み込みはこちらを使う。 */
    private var searchedQuery: String? = null

    private var loadMoreJob: Job? = null

    init {
        val submittedQuery = submitTrigger
            .map { queryFlow.value.trim() }
            .filter { it.isNotBlank() }

        val retriedQuery = retryTrigger
            .map { queryFlow.value.trim() }
            .filter { it.isNotBlank() }

        merge(
            submittedQuery.map { SearchRequest(it, isRetry = false) },
            retriedQuery.map { SearchRequest(it, isRetry = true) },
        )
            .flatMapLatest(::searchFirstPage)
            .onEach(::applySearchResult)
            .launchIn(viewModelScope)

        // プロセス再生成では復元したクエリで一度だけ自動的に引き直す — 利用者は入力し直さなくてよい
        if (queryFlow.value.isNotBlank()) submitTrigger.tryEmit(Unit)
    }

    override fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> onQueryChanged(intent.query)

            SearchIntent.SearchSubmitted -> submitTrigger.tryEmit(Unit)

            is SearchIntent.PhotoClicked ->
                sendEffect(SearchEffect.NavigateToDetail(intent.photoId))

            SearchIntent.LoadMore -> loadMore()

            SearchIntent.Retry -> retryTrigger.tryEmit(Unit)

            SearchIntent.PexelsLinkClicked -> sendEffect(SearchEffect.OpenUrl(PEXELS_URL))
        }
    }

    /**
     * 空欄かつ結果も無ければ再試行の対象がないため、エラーを畳んで案内表示に戻す。
     * 結果が残るフッターエラーは畳まない — 再試行は生きており、畳むと自動追加読み込みが再武装して無駄な要求が飛ぶ。
     */
    private fun onQueryChanged(query: String) {
        updateState { state ->
            state.copy(
                query = query,
                hasSearched = isShowingResultFor(query),
                error = state.error.takeUnless { query.isBlank() && state.photos.isEmpty() },
            )
        }
        savedStateHandle[KEY_QUERY] = query
    }

    /**
     * 入力欄のクエリが、表示中の結果を生んだ [searchedQuery] と一致するか。
     * ずれている間の「結果なし」は今のクエリの話ではないため、案内表示に戻す。
     */
    private fun isShowingResultFor(query: String): Boolean = query.trim() == searchedQuery

    private fun searchFirstPage(request: SearchRequest): Flow<Outcome<PhotoPage>> = flow {
        // 新しい検索が始まったら、進行中の追加読み込みは古いクエリの結果なので打ち切る
        loadMoreJob?.cancel()
        searchedQuery = request.query
        updateState { it.copy(isLoading = true, isLoadingMore = false, error = null) }
        emit(searchPhotos(request))
    }

    private suspend fun searchPhotos(request: SearchRequest): Outcome<PhotoPage> =
        if (request.isRetry) {
            withMinLoadingDuration { photoRepository.searchPhotos(request.query, FIRST_PAGE) }
        } else {
            photoRepository.searchPhotos(request.query, FIRST_PAGE)
        }

    private fun applySearchResult(outcome: Outcome<PhotoPage>) {
        updateState { state ->
            when (outcome) {
                is Outcome.Success -> state.copy(
                    isLoading = false,
                    hasSearched = isShowingResultFor(state.query),
                    // 打ち切った追加読み込みが遅れて書いたエラーを引きずらないよう、成功時に明示的に消す
                    error = null,
                    // ページ内に同じ写真が混ざってもグリッドの key 衝突で落ちないよう、IDで重複を除く
                    photos = outcome.data.photos.distinctBy(Photo::id),
                    page = outcome.data.page,
                    endReached = outcome.data.endReached,
                )

                // 結果を残すと追加読み込みの失敗と区別できなくなるため、初回検索の失敗は全画面エラーに寄せる
                is Outcome.Failure -> state.copy(
                    isLoading = false,
                    hasSearched = isShowingResultFor(state.query),
                    photos = emptyList(),
                    error = outcome.error,
                )
            }
        }
    }

    private fun loadMore() {
        val query = searchedQuery ?: return
        val current = currentState
        if (current.isLoading || current.isLoadingMore || current.endReached) return

        updateState { it.copy(isLoadingMore = true, error = null) }
        loadMoreJob = viewModelScope.launch {
            when (val outcome = photoRepository.searchPhotos(query, current.page + 1)) {
                is Outcome.Success -> updateState { state ->
                    state.copy(
                        isLoadingMore = false,
                        // Pexelsはページを跨いで同じ写真を返すことがあるため、グリッドのkey衝突を防ぐ目的でIDで重複を除く
                        photos = (state.photos + outcome.data.photos).distinctBy(Photo::id),
                        page = outcome.data.page,
                        endReached = outcome.data.endReached,
                    )
                }

                is Outcome.Failure -> updateState {
                    it.copy(isLoadingMore = false, error = outcome.error)
                }
            }
        }
    }
}
