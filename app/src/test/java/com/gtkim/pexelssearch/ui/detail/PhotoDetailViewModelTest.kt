package com.gtkim.pexelssearch.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import com.gtkim.pexelssearch.ui.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val PHOTO_ID = 42L
private const val KEY_PHOTO_ID = "photoId"
private const val LOAD_DELAY_MILLIS = 100L

@OptIn(ExperimentalCoroutinesApi::class)
class PhotoDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val photoRepository = mockk<PhotoRepository>()

    private fun makeViewModel() = PhotoDetailViewModel(
        photoRepository,
        SavedStateHandle(mapOf(KEY_PHOTO_ID to PHOTO_ID)),
    )

    @Test
    fun `遷移で渡されたidの写真が取得され State に反映される`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Success(photo())
        val viewModel = makeViewModel()

        advanceUntilIdle()

        assertEquals(photo(), viewModel.uiState.value.photo)
        coVerify(exactly = 1) { photoRepository.getPhoto(PHOTO_ID) }
    }

    @Test
    fun `取得に失敗するとエラーが State に反映される`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()

        advanceUntilIdle()

        assertEquals(PhotoError.Network, viewModel.uiState.value.error)
        assertEquals(null, viewModel.uiState.value.photo)
    }

    @Test
    fun `Retry では同じidで取得し直しエラーを消す`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Failure(PhotoError.Network)
        val viewModel = makeViewModel()
        advanceUntilIdle()

        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Success(photo())
        viewModel.onIntent(PhotoDetailIntent.Retry)
        advanceUntilIdle()

        assertEquals(photo(), viewModel.uiState.value.photo)
        assertEquals(null, viewModel.uiState.value.error)
        coVerify(exactly = 2) { photoRepository.getPhoto(PHOTO_ID) }
    }

    @Test
    fun `取得中は isLoading が true になる`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } coAnswers {
            delay(LOAD_DELAY_MILLIS)
            Outcome.Success(photo())
        }
        val viewModel = makeViewModel()

        assertEquals(true, viewModel.uiState.value.isLoading)

        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun `撮影者をクリックすると撮影者ページのURLが Effect に流れる`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Success(photo())
        val viewModel = makeViewModel()
        advanceUntilIdle()

        viewModel.onIntent(PhotoDetailIntent.PhotographerClicked)

        assertEquals(
            PhotoDetailEffect.OpenUrl(photo().photographerUrl),
            viewModel.effect.first(),
        )
    }

    @Test
    fun `Pexelsをクリックすると写真ページのURLが Effect に流れる`() = runTest {
        coEvery { photoRepository.getPhoto(PHOTO_ID) } returns Outcome.Success(photo())
        val viewModel = makeViewModel()
        advanceUntilIdle()

        // 撮影者ページとは別のURLが流れること — クレジットの2つのリンクは行き先が違う
        viewModel.onIntent(PhotoDetailIntent.PexelsLinkClicked)

        assertEquals(
            PhotoDetailEffect.OpenUrl(photo().pexelsUrl),
            viewModel.effect.first(),
        )
    }
}

private fun photo() = Photo(
    id = PHOTO_ID,
    photographer = "山田太郎",
    photographerUrl = "https://www.pexels.com/@yamada",
    pexelsUrl = "https://www.pexels.com/photo/$PHOTO_ID/",
    thumbnailUrl = "https://images.pexels.com/photos/$PHOTO_ID/medium.jpg",
    fullUrl = "https://images.pexels.com/photos/$PHOTO_ID/large2x.jpg",
    avgColorRgb = 0x6E7B8B,
)
