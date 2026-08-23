package com.gtkim.pexelssearch.ui.search

import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val QUERY = "猫"
private const val OTHER_QUERY = "犬"
private const val BLANK_QUERY = "   "
private const val DEBOUNCE_MILLIS = 300L
private const val FIRST_PAGE = 1

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photoRepository = mockk<PhotoRepository>()

    private fun makeViewModel() = SearchViewModel(photoRepository)

    @Test
    fun `入力が止まってから一度だけ検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(QUERY))
        // advanceTimeBy は指定時刻ちょうどのタスクを実行しないため、この時点ではまだ発火していない
        advanceTimeBy(DEBOUNCE_MILLIS)
        coVerify(exactly = 0) { photoRepository.searchPhotos(any(), any()) }

        advanceUntilIdle()
        coVerify(exactly = 1) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
    }

    @Test
    fun `連続入力では最後のクエリだけが検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(QUERY))
        advanceTimeBy(DEBOUNCE_MILLIS)
        viewModel.onIntent(SearchIntent.QueryChanged(OTHER_QUERY))
        advanceUntilIdle()

        coVerify(exactly = 0) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
        coVerify(exactly = 1) { photoRepository.searchPhotos(OTHER_QUERY, FIRST_PAGE) }
    }

    @Test
    fun `空白のみのクエリでは検索しない`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(BLANK_QUERY))
        advanceUntilIdle()

        coVerify(exactly = 0) { photoRepository.searchPhotos(any(), any()) }
    }

    @Test
    fun `Retry は同じクエリでも再検索される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns successPage(photo(1L))
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(QUERY))
        advanceUntilIdle()

        // distinctUntilChanged の後段で merge しているため、同一クエリでも流れ直す
        viewModel.onIntent(SearchIntent.Retry)
        advanceUntilIdle()

        coVerify(exactly = 2) { photoRepository.searchPhotos(QUERY, FIRST_PAGE) }
    }

    @Test
    fun `検索に失敗するとエラーが State に反映される`() = runTest {
        coEvery { photoRepository.searchPhotos(any(), any()) } returns
            Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()

        viewModel.onIntent(SearchIntent.QueryChanged(QUERY))
        advanceUntilIdle()

        assertEquals(PhotoError.Network, viewModel.uiState.value.error)
        assertEquals(false, viewModel.uiState.value.isLoading)
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
