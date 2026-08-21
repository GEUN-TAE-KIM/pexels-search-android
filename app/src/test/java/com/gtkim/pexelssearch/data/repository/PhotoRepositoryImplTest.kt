package com.gtkim.pexelssearch.data.repository

import com.gtkim.pexelssearch.data.remote.PexelsApi
import com.gtkim.pexelssearch.data.remote.dto.PhotoDto
import com.gtkim.pexelssearch.data.remote.dto.PhotoSrcDto
import com.gtkim.pexelssearch.data.remote.dto.SearchPhotosResponse
import com.gtkim.pexelssearch.domain.error.PhotoError
import com.gtkim.pexelssearch.domain.model.Outcome
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

private const val QUERY = "猫"
private const val NEXT_PAGE_URL = "https://api.pexels.com/v1/search?page=2&per_page=20&query=猫"

class PhotoRepositoryImplTest {

    private val api = mockk<PexelsApi>()
    private val repository = PhotoRepositoryImpl(api)

    @Test
    fun `次ページが存在する場合は endReached が false になる`() = runTest {
        coEvery { api.searchPhotos(any(), any(), any()) } returns
            SearchPhotosResponse(photos = listOf(photoDto(1)), nextPage = NEXT_PAGE_URL)

        val page = repository.searchPhotos(QUERY, page = 1).successData()

        assertEquals(false, page.endReached)
        assertEquals(1, page.photos.size)
    }

    @Test
    fun `空ページを受け取った場合は endReached が true になる`() = runTest {
        // next_pageが付いていても写真が0件なら終端とみなす
        coEvery { api.searchPhotos(any(), any(), any()) } returns
            SearchPhotosResponse(photos = emptyList(), nextPage = NEXT_PAGE_URL)

        val page = repository.searchPhotos(QUERY, page = 2).successData()

        assertTrue(page.endReached)
    }

    @Test
    fun `nextPage が null の場合は endReached が true になる`() = runTest {
        coEvery { api.searchPhotos(any(), any(), any()) } returns
            SearchPhotosResponse(photos = listOf(photoDto(1)), nextPage = null)

        val page = repository.searchPhotos(QUERY, page = 3).successData()

        assertTrue(page.endReached)
    }

    @Test
    fun `空ページでも page は要求したページ番号になる`() = runTest {
        // サーバーは空ページで page:1 を返すため、応答ではなく要求値を採用している
        coEvery { api.searchPhotos(any(), any(), any()) } returns
            SearchPhotosResponse(photos = emptyList(), nextPage = null)

        val page = repository.searchPhotos(QUERY, page = 7).successData()

        assertEquals(7, page.page)
        coVerify(exactly = 1) { api.searchPhotos(QUERY, 7, any()) }
    }

    @Test
    fun `例外は Outcome の Failure に変換される`() = runTest {
        coEvery { api.searchPhotos(any(), any(), any()) } throws IOException()

        val error = repository.searchPhotos(QUERY, page = 1).failureError()

        assertEquals(PhotoError.Network, error)
    }
}

private fun photoDto(id: Long) = PhotoDto(
    id = id,
    url = "https://www.pexels.com/photo/$id/",
    photographer = "山田太郎",
    photographerUrl = "https://www.pexels.com/@yamada",
    avgColor = "#6E7B8B",
    src = PhotoSrcDto(
        medium = "https://images.pexels.com/photos/$id/medium.jpg",
        large2x = "https://images.pexels.com/photos/$id/large2x.jpg",
    ),
)

private fun <T> Outcome<T>.successData(): T = when (this) {
    is Outcome.Success -> data
    is Outcome.Failure -> throw AssertionError("Successを期待したが $this だった")
}

private fun Outcome<*>.failureError(): PhotoError = when (this) {
    is Outcome.Failure -> error
    is Outcome.Success -> throw AssertionError("Failureを期待したが $this だった")
}
