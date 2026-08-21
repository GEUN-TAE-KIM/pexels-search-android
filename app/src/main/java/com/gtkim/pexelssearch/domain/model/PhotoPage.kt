package com.gtkim.pexelssearch.domain.model

/**
 * 終端判定はdata層で済ませ、ドメインには結果だけを渡す。
 *
 * [page] はサーバー応答ではなく要求したページ番号。
 */
data class PhotoPage(
    val photos: List<Photo>,
    val page: Int,
    val endReached: Boolean,
)
