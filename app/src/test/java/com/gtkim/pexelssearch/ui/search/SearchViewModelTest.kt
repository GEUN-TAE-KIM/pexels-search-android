package com.gtkim.pexelssearch.ui.search

import androidx.lifecycle.SavedStateHandle
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import com.gtkim.pexelssearch.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val QUERY = "猫"
private const val OTHER_QUERY = "犬"
private const val BLANK_QUERY = "   "
private const val QUERY_WITH_TRAILING_SPACE = "猫 "
private const val MIN_LOADING_MILLIS = 400L
private const val LOAD_DELAY_MILLIS = 100L
private const val FIRST_PAGE = 1
private const val SECOND_PAGE = 2

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photoRepository = mockk<PhotoRepository>()

    private fun makeViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ) = SearchViewModel(photoRepository, savedStateHandle)

    /** 入力してから実行する、という利用者の操作をまとめたもの。 */
    private fun SearchViewModel.search(query: String) {
        onIntent(SearchIntent.QueryChanged(query))
        onIntent(SearchIntent.SearchSubmitted)
    }

    @Test
    fun `入力しただけでは検索されない`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(QUERY))
        advanceUntilIdle()

        // 日本語入力では変換途中の文字列も流れてくるため、入力のたびに引くと結果が安定しない
        coVerify(exactly = 0) { photoRepository.searchPhotos(any(), any()) }
        // まだ検索していないので「結果なし」ではなく案内を出し続ける
        assertEquals(false, viewModel.uiState.value.hasSearched)
    }

    @Test
    fun `検索を実行するとそのクエリで検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()

        coVerify(exactly = 1) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
        assertEquals(true, viewModel.uiState.value.hasSearched)
    }

    @Test
    fun `空白のみのクエリでは実行しても検索しない`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        // 空クエリは Search API が 400 を返すため送らない
        viewModel.search(BLANK_QUERY)
        advanceUntilIdle()

        coVerify(exactly = 0) { photoRepository.searchPhotos(any(), any()) }
    }

    @Test
    fun `前後の空白は取り除いて検索する`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.search(QUERY_WITH_TRAILING_SPACE)
        advanceUntilIdle()

        coVerify(exactly = 1) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
        coVerify(exactly = 0) { photoRepository.searchPhotos(QUERY_WITH_TRAILING_SPACE, any()) }
    }

    @Test
    fun `同じクエリでも実行するたびに検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        // 明示的に実行した以上、同じクエリでも引き直すのが利用者の期待
        viewModel.onIntent(SearchIntent.SearchSubmitted)
        advanceUntilIdle()

        coVerify(exactly = 2) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
    }

    @Test
    fun `Retry は同じクエリでも再検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
    }

    @Test
    fun `Retry も trim したクエリで再検索する`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()
        viewModel.search(QUERY_WITH_TRAILING_SPACE)
        advanceUntilIdle()

        viewModel.onIntent(SearchIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
        coVerify(exactly = 0) { photoRepository.searchPhotos(QUERY_WITH_TRAILING_SPACE, any()) }
    }

    @Test
    fun `検索中は isLoading が true になる`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } coAnswers {
            delay(LOAD_DELAY_MILLIS)
            successPage(photo(1L))
        }
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        assertEquals(true, viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `再試行は即座に失敗してもローディングが最低表示時間だけ残る`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns
            Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()
        viewModel.search(QUERY)
        advanceUntilIdle()

        // 失敗が数msで返ると、押した直後にローディングが1フレームも描かれず反応がないように見える
        viewModel.onIntent(SearchIntent.Retry)
        advanceTimeBy(MIN_LOADING_MILLIS)
        assertEquals(true, viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `初回検索の失敗では結果を空にする`() = runTest {
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns successPage(photo(1L))
        coEvery { photoRepository.searchPhotos(OTHER_QUERY, FIRST_PAGE) } returns
            Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        assertEquals(listOf(1L), viewModel.uiState.value.photos.map(Photo::id))

        // 前のクエリの結果を残すと、追加読み込みの失敗と State だけでは区別できなくなる
        viewModel.search(OTHER_QUERY)
        advanceUntilIdle()

        assertEquals(emptyList<Photo>(), viewModel.uiState.value.photos)
        assertEquals(PhotoError.Network, viewModel.uiState.value.error)
    }

    @Test
    fun `検索欄を空にするとエラー表示を畳む`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns
            Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        assertEquals(PhotoError.Network, viewModel.uiState.value.error)

        // 空欄では再試行の対象自体がないため、残すと押しても何も起きないボタンだけが居座る
        viewModel.onIntent(SearchIntent.QueryChanged(""))
        advanceUntilIdle()

        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun `結果が残っていれば検索欄を空にしてもエラーを畳まない`() = runTest {
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns successPage(photo(1L))
        coEvery { photoRepository.searchPhotos(QUERY, SECOND_PAGE) } returns
            Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        viewModel.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(PhotoError.Network, viewModel.uiState.value.error)

        // フッターの再試行は searchedQuery で動くため空欄でも生きている。畳むと自動追加読み込みが再武装する
        viewModel.onIntent(SearchIntent.QueryChanged(""))
        advanceUntilIdle()

        assertEquals(PhotoError.Network, viewModel.uiState.value.error)
        assertEquals(listOf(1L), viewModel.uiState.value.photos.map(Photo::id))
    }

    @Test
    fun `クエリを変えると前回の結果なし表示は消える`() = runTest {
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns successPage()
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        assertEquals(true, viewModel.uiState.value.hasSearched)

        // まだ検索していないクエリに「検索結果が見つかりませんでした」を出さない
        viewModel.onIntent(SearchIntent.QueryChanged(OTHER_QUERY))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.hasSearched)
    }

    @Test
    fun `初回ページ内の重複は除かれる`() = runTest {
        // Pexels の応答は信頼できないため、ページ内の重複でもグリッドの key 衝突で落ちないようにする
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns
            successPage(photo(1L), photo(1L), photo(2L))
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()

        assertEquals(listOf(1L, 2L), viewModel.uiState.value.photos.map(Photo::id))
    }

    @Test
    fun `追加読み込みは結果を蓄積し重複を除く`() = runTest {
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns
            successPage(photo(1L), photo(2L), page = FIRST_PAGE)
        coEvery { photoRepository.searchPhotos(QUERY, SECOND_PAGE) } returns
            successPage(photo(2L), photo(3L), page = SECOND_PAGE)
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        viewModel.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        // Pexelsはページを跨いで同じ写真を返すため、id 2 が二重に並ばない
        assertEquals(listOf(1L, 2L, 3L), viewModel.uiState.value.photos.map(Photo::id))
        assertEquals(SECOND_PAGE, viewModel.uiState.value.page)
    }

    @Test
    fun `endReached 以降は追加読み込みしない`() = runTest {
        coEvery { photoRepository.searchPhotos(QUERY, FIRST_PAGE) } returns
            successPage(photo(1L), endReached = true)
        val viewModel = makeViewModel()

        viewModel.search(QUERY)
        advanceUntilIdle()
        viewModel.onIntent(SearchIntent.LoadMore)
        advanceUntilIdle()

        coVerify(exactly = 1) { photoRepository.searchPhotos(any(), any()) }
    }

    @Test
    fun `SavedStateHandle のクエリが復元され自動で検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val savedStateHandle = SavedStateHandle()

        makeViewModel(savedStateHandle).search(QUERY)
        advanceUntilIdle()

        // プロセス再生成 — 同じ SavedStateHandle から作り直す
        val restored = makeViewModel(savedStateHandle)
        assertEquals(QUERY, restored.uiState.value.query)

        // 復元後は利用者が入力し直さなくても結果が戻る
        advanceUntilIdle()
        assertEquals(listOf(1L), restored.uiState.value.photos.map(Photo::id))
    }
}

private fun photo(id: Long) = Photo(
    id = id,
    photographer = "山田太郎",
    photographerUrl = "https://www.pexels.com/@yamada",
    pexelsUrl = "https://www.pexels.com/photo/$id/",
    thumbnailUrl = "https://images.pexels.com/photos/$id/medium.jpg",
    fullUrl = "https://images.pexels.com/photos/$id/large2x.jpg",
    avgColorRgb = 0x6E7B8B,
)

private fun successPage(
    vararg photos: Photo,
    page: Int = FIRST_PAGE,
    endReached: Boolean = false,
): Outcome<PhotoPage> = Outcome.Success(
    PhotoPage(photos = photos.toList(), page = page, endReached = endReached),
)
