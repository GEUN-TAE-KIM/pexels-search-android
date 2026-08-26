package com.gtkim.pexelssearch.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import com.gtkim.pexelssearch.ui.BaseViewModel
import com.gtkim.pexelssearch.ui.navigation.PhotoDetailRoute
import com.gtkim.pexelssearch.ui.withMinLoadingDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_PHOTO_ID = "photoId"

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PhotoDetailUiState, PhotoDetailIntent, PhotoDetailEffect>(PhotoDetailUiState()) {

    /**
     * 一覧から写真を丸ごと渡さず、[PhotoDetailRoute] のidだけを受け取って自分で取得する。
     * toRoute() はBundleを経由しJVMのユニットテストで動かないため、キーで直接読む
     * — キー名は [PhotoDetailRoute.photoId] と揃える。
     */
    private val photoId: Long = checkNotNull(savedStateHandle.get<Long>(KEY_PHOTO_ID)) {
        "$KEY_PHOTO_ID がNavigationの引数に含まれていない"
    }

    private var loadJob: Job? = null

    init {
        loadPhoto()
    }

    override fun onIntent(intent: PhotoDetailIntent) {
        when (intent) {
            PhotoDetailIntent.Retry -> loadPhoto(isRetry = true)
            PhotoDetailIntent.PhotographerClicked -> openUrl(Photo::photographerUrl)
            PhotoDetailIntent.PexelsLinkClicked -> openUrl(Photo::pexelsUrl)
        }
    }

    private fun loadPhoto(isRetry: Boolean = false) {
        // 前の取得を残すと、遅れて返った失敗が新しい結果を上書きして写真とエラーが同居する
        loadJob?.cancel()
        updateState { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            when (val outcome = getPhoto(isRetry)) {
                is Outcome.Success -> updateState {
                    it.copy(isLoading = false, photo = outcome.data, error = null)
                }

                // 写真を残したままエラーを立てると State が矛盾するため、検索画面と同じく結果を捨てる
                is Outcome.Failure -> updateState {
                    it.copy(isLoading = false, photo = null, error = outcome.error)
                }
            }
        }
    }

    private suspend fun getPhoto(isRetry: Boolean): Outcome<Photo> =
        if (isRetry) {
            withMinLoadingDuration { photoRepository.getPhoto(photoId) }
        } else {
            photoRepository.getPhoto(photoId)
        }

    /** 写真が未取得ならリンク先も存在しないため、何も起こさない。 */
    private fun openUrl(selectUrl: (Photo) -> String) {
        val photo = currentState.photo ?: return
        sendEffect(PhotoDetailEffect.OpenUrl(selectUrl(photo)))
    }
}
