package com.gtkim.pexelssearch.data.error

import com.gtkim.pexelssearch.domain.error.PhotoError
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class PhotoErrorMapperTest {

    @Test
    fun `HTTP 429 は RateLimit に変換される`() {
        assertEquals(PhotoError.RateLimit, httpError(429).toPhotoError())
    }

    @Test
    fun `HTTP 500 番台は Server に変換される`() {
        assertEquals(PhotoError.Server, httpError(500).toPhotoError())
        assertEquals(PhotoError.Server, httpError(503).toPhotoError())
    }

    @Test
    fun `HTTP 401 は Unauthorized に変換される`() {
        assertEquals(PhotoError.Unauthorized, httpError(401).toPhotoError())
    }

    @Test
    fun `通信失敗は Network に変換される`() {
        assertEquals(PhotoError.Network, IOException().toPhotoError())
    }

    @Test
    fun `未分類のエラーは Unknown となり原因が保持される`() {
        val emptyQuery = httpError(400)
        assertEquals(PhotoError.Unknown(emptyQuery), emptyQuery.toPhotoError())

        val parseFailure = SerializationException("不正な形式")
        assertEquals(PhotoError.Unknown(parseFailure), parseFailure.toPhotoError())
    }

    private fun httpError(code: Int): HttpException =
        HttpException(Response.error<Unit>(code, "".toResponseBody(null)))
}
