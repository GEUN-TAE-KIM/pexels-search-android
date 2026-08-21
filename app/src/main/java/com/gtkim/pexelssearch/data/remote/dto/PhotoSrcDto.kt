package com.gtkim.pexelssearch.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PhotoSrcDto(
    val medium: String,
    val large2x: String,
)
