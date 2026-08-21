package com.gtkim.pexelssearch.domain.model

import com.gtkim.pexelssearch.domain.error.PhotoError
import kotlin.coroutines.cancellation.CancellationException

sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: PhotoError) : Outcome<Nothing>
}

/**
 * [CancellationException] を握り潰すと flatMapLatest による検索の打ち切りが機能しなくなる。
 */
inline fun <T> safeCall(
    onError: (Throwable) -> PhotoError,
    block: () -> T,
): Outcome<T> = try {
    Outcome.Success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Outcome.Failure(onError(e))
}
