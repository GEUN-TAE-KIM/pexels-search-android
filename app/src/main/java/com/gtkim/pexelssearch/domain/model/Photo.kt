package com.gtkim.pexelssearch.domain.model

/**
 * サイズ選択も `avg_color` のパースもマッピング時点で終えているため、uiは値をそのまま使う。
 */
data class Photo(
    val id: Long,
    val photographer: String,
    val photographerUrl: String,
    val pexelsUrl: String,
    val thumbnailUrl: String,
    val fullUrl: String,
    /** 読み込み中と失敗時のプレースホルダー色。 */
    val avgColorRgb: Int,
)
