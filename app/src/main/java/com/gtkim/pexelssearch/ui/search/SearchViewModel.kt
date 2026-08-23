package com.gtkim.pexelssearch.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SEARCH_DEBOUNCE_MILLIS = 300L
private const val FIRST_PAGE = 1

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<SearchEffect>(Channel.BUFFERED)
    val effect: Flow<SearchEffect> = effectChannel.receiveAsFlow()

    private val queryFlow = MutableStateFlow("")

    /** 検索は [distinctUntilChanged] を通るため、同一クエリを再実行する Retry は別トリガーで流す。 */
    private val retryTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** 表示中の結果を生んだクエリ。入力途中の [queryFlow] と混ざらないよう、追加読み込みはこちらを使う。 */
    private var searchedQuery: String? = null

    private var loadMoreJob: Job? = null

    init {
        val debouncedQuery = queryFlow
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            // 空クエリは Search API が 400 を返すため送らない
            .filter { it.isNotBlank() }
            .distinctUntilChanged()

        val retriedQuery = retryTrigger
            .map { queryFlow.value }
            .filter { it.isNotBlank() }

        merge(debouncedQuery, retriedQuery)
            .flatMapLatest(::searchFirstPage)
            .onEach(::applySearchResult)
            .launchIn(viewModelScope)
    }

    fun onIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                _uiState.update { it.copy(query = intent.query) }
                queryFlow.value = intent.query
            }

            is SearchIntent.PhotoClicked -> viewModelScope.launch {
                effectChannel.send(SearchEffect.NavigateToDetail(intent.photoId))
            }

            SearchIntent.LoadMore -> loadMore()

            SearchIntent.Retry -> retryTrigger.tryEmit(Unit)
        }
    }

    private fun searchFirstPage(query: String): Flow<Outcome<PhotoPage>> = flow {
        // 新しい検索が始まったら、進行中の追加読み込みは古いクエリの結果なので打ち切る
        loadMoreJob?.cancel()
        searchedQuery = query
        _uiState.update { it.copy(isLoading = true, isLoadingMore = false, error = null) }
        emit(photoRepository.searchPhotos(query, FIRST_PAGE))
    }

    private fun applySearchResult(outcome: Outcome<PhotoPage>) {
        _uiState.update { state ->
            when (outcome) {
                is Outcome.Success -> state.copy(
                    isLoading = false,
                    photos = outcome.data.photos,
                    page = outcome.data.page,
                    endReached = outcome.data.endReached,
                )

                // 結果を残すと追加読み込みの失敗と区別できなくなるため、初回検索の失敗は全画面エラーに寄せる
                is Outcome.Failure -> state.copy(
                    isLoading = false,
                    photos = emptyList(),
                    error = outcome.error,
                )
            }
        }
    }

    private fun loadMore() {
        val query = searchedQuery ?: return
        val current = _uiState.value
        if (current.isLoading || current.isLoadingMore || current.endReached) return

        _uiState.update { it.copy(isLoadingMore = true, error = null) }
        loadMoreJob = viewModelScope.launch {
            when (val outcome = photoRepository.searchPhotos(query, current.page + 1)) {
                is Outcome.Success -> _uiState.update { state ->
                    state.copy(
                        isLoadingMore = false,
                        // Pexelsはページを跨いで同じ写真を返すことがあるため、グリッドのkey衝突を防ぐ目的でIDで重複を除く
                        photos = (state.photos + outcome.data.photos).distinctBy(Photo::id),
                        page = outcome.data.page,
                        endReached = outcome.data.endReached,
                    )
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isLoadingMore = false, error = outcome.error)
                }
            }
        }
    }
}
