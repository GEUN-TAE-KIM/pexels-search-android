package com.gtkim.pexelssearch.data.mapper

import com.gtkim.pexelssearch.data.remote.dto.PhotoDto
import com.gtkim.pexelssearch.data.remote.dto.PhotoSrcDto
import org.junit.Assert.assertEquals
import org.junit.Test

private const val PHOTO_ID = 42L
private const val MEDIUM_URL = "https://images.pexels.com/photos/42/medium.jpg"
private const val LARGE2X_URL = "https://images.pexels.com/photos/42/large2x.jpg"
private const val FALLBACK_AVG_COLOR = 0xE0E0E0

class PhotoMapperTest {

    @Test
    fun `グリッドと詳細で使うURLはマッピング時点で選び分けられる`() {
        val photo = photoDto(avgColor = "#6E7B8B").toDomain()

        assertEquals(MEDIUM_URL, photo.thumbnailUrl)
        assertEquals(LARGE2X_URL, photo.fullUrl)
    }

    @Test
    fun `avg_color は数値に変換されて渡る`() {
        val photo = photoDto(avgColor = "#6E7B8B").toDomain()

        assertEquals(0x6E7B8B, photo.avgColorRgb)
    }

    @Test
    fun `avg_color が欠落していればフォールバック色になる`() {
        val photo = photoDto(avgColor = null).toDomain()

        assertEquals(FALLBACK_AVG_COLOR, photo.avgColorRgb)
    }

    @Test
    fun `avg_color が16進として読めなければフォールバック色になる`() {
        val photo = photoDto(avgColor = "#ZZZZZZ").toDomain()

        assertEquals(FALLBACK_AVG_COLOR, photo.avgColorRgb)
    }

    @Test
    fun `avg_color が6桁でなければフォールバック色になる`() {
        // 3桁でも16進としては読めてしまうため、桁数で弾かないと別の色になる
        val photo = photoDto(avgColor = "#FFF").toDomain()

        assertEquals(FALLBACK_AVG_COLOR, photo.avgColorRgb)
    }
}

private fun photoDto(avgColor: String?) = PhotoDto(
    id = PHOTO_ID,
    url = "https://www.pexels.com/photo/$PHOTO_ID/",
    photographer = "山田太郎",
    photographerUrl = "https://www.pexels.com/@yamada",
    avgColor = avgColor,
    src = PhotoSrcDto(medium = MEDIUM_URL, large2x = LARGE2X_URL),
)
