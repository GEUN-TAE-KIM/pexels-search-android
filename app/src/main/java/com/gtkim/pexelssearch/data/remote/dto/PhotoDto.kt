package com.gtkim.pexelssearch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhotoDto(
    val id: Long,
    val url: String,
    val photographer: String,
    @SerialName("photographer_url") val photographerUrl: String,
    @SerialName("avg_color") val avgColor: String? = null,
    val src: PhotoSrcDto,
)
