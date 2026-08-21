package com.gtkim.pexelssearch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `page` は持たない — 空ページでサーバーが `page:1` / `per_page:1` にリセットするため信頼できない。
 */
@Serializable
data class SearchPhotosResponse(
    val photos: List<PhotoDto>,
    @SerialName("next_page") val nextPage: String? = null,
)
