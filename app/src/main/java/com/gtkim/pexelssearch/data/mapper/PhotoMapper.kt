package com.gtkim.pexelssearch.data.mapper

import com.gtkim.pexelssearch.data.remote.dto.PhotoDto
import com.gtkim.pexelssearch.domain.model.Photo

private const val FALLBACK_AVG_COLOR = 0xE0E0E0
private const val AVG_COLOR_HEX_LENGTH = 6

/**
 * `original` は数MB規模になるため使わない。グリッドは `medium`、詳細は `large2x` で足りる。
 */
fun PhotoDto.toDomain(): Photo = Photo(
    id = id,
    photographer = photographer,
    photographerUrl = photographerUrl,
    pexelsUrl = url,
    thumbnailUrl = src.medium,
    fullUrl = src.large2x,
    avgColorRgb = avgColor.toRgbOrFallback(),
)

/**
 * 読み込み中と失敗時のプレースホルダー色。欠落や書式不正でも写真自体の表示は妨げない。
 */
private fun String?.toRgbOrFallback(): Int =
    this?.removePrefix("#")
        ?.takeIf { it.length == AVG_COLOR_HEX_LENGTH }
        ?.toIntOrNull(radix = 16)
        ?: FALLBACK_AVG_COLOR
