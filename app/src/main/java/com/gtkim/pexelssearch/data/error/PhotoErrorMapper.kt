package com.gtkim.pexelssearch.data.error

import com.gtkim.pexelssearch.domain.error.PhotoError
import retrofit2.HttpException
import java.io.IOException

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERRORS = 500..599

fun Throwable.toPhotoError(): PhotoError = when (this) {
    is HttpException -> when (code()) {
        HTTP_UNAUTHORIZED -> PhotoError.Unauthorized
        HTTP_TOO_MANY_REQUESTS -> PhotoError.RateLimit
        in HTTP_SERVER_ERRORS -> PhotoError.Server
        else -> PhotoError.Unknown(this)
    }

    is IOException -> PhotoError.Network
    else -> PhotoError.Unknown(this)
}
