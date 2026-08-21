package com.gtkim.pexelssearch.domain.repository

import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage

/**
 * 詳細画面はphotoIdだけを受け取り、[getPhoto] で自分の表示データを取得する。
 */
interface PhotoRepository {
    suspend fun searchPhotos(query: String, page: Int): Outcome<PhotoPage>
    suspend fun getPhoto(id: Long): Outcome<Photo>
}
