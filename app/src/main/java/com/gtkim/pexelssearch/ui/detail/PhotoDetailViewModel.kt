package com.gtkim.pexelssearch.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import com.gtkim.pexelssearch.ui.navigation.PhotoDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_PHOTO_ID = "photoId"

@HiltViewModel
class PhotoDetailViewModel @Inject constructor(
    private val photoRepository: PhotoRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /**
     * 一覧から写真を丸ごと渡さず、[PhotoDetailRoute] のidだけを受け取って自分で取得する。
     * toRoute() はBundleを経由しJVMのユニットテストで動かないため、キーで直接読む
     * — キー名は [PhotoDetailRoute.photoId] と揃える。
     */
    private val photoId: Long = checkNotNull(savedStateHandle.get<Long>(KEY_PHOTO_ID)) {
        "$KEY_PHOTO_ID がNavigationの引数に含まれていない"
    }

    private val _uiState = MutableStateFlow(PhotoDetailUiState())
    val uiState: StateFlow<PhotoDetailUiState> = _uiState.asStateFlow()

    private val effectChannel = Channel<PhotoDetailEffect>(Channel.BUFFERED)
    val effect: Flow<PhotoDetailEffect> = effectChannel.receiveAsFlow()

    init {
        loadPhoto()
    }

    fun onIntent(intent: PhotoDetailIntent) {
        when (intent) {
            PhotoDetailIntent.Retry -> loadPhoto()
            PhotoDetailIntent.PhotographerClicked -> openUrl(Photo::photographerUrl)
            PhotoDetailIntent.PexelsLinkClicked -> openUrl(Photo::pexelsUrl)
        }
    }

    private fun loadPhoto() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            when (val outcome = photoRepository.getPhoto(photoId)) {
                is Outcome.Success -> _uiState.update {
                    it.copy(isLoading = false, photo = outcome.data)
                }

                is Outcome.Failure -> _uiState.update {
                    it.copy(isLoading = false, error = outcome.error)
                }
            }
        }
    }

    /** 写真が未取得ならリンク先も存在しないため、何も起こさない。 */
    private fun openUrl(selectUrl: (Photo) -> String) {
        val photo = _uiState.value.photo ?: return
        viewModelScope.launch {
            effectChannel.send(PhotoDetailEffect.OpenUrl(selectUrl(photo)))
        }
    }
}
