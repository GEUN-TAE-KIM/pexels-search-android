package com.gtkim.pexelssearch.data.repository

import com.gtkim.pexelssearch.data.error.toPhotoError
import com.gtkim.pexelssearch.data.mapper.toDomain
import com.gtkim.pexelssearch.data.remote.PexelsApi
import com.gtkim.pexelssearch.domain.model.Outcome
import com.gtkim.pexelssearch.domain.model.Photo
import com.gtkim.pexelssearch.domain.model.PhotoPage
import com.gtkim.pexelssearch.domain.model.safeCall
import com.gtkim.pexelssearch.domain.repository.PhotoRepository
import javax.inject.Inject

private const val PER_PAGE = 20

class PhotoRepositoryImpl @Inject constructor(
    private val api: PexelsApi,
) : PhotoRepository {

    override suspend fun searchPhotos(query: String, page: Int): Outcome<PhotoPage> =
        safeCall(Throwable::toPhotoError) {
            val response = api.searchPhotos(query = query, page = page, perPage = PER_PAGE)
            PhotoPage(
                photos = response.photos.map { it.toDomain() },
                // 空ページではサーバーが page:1 を返すため、レスポンスではなく要求したページ番号を採用する
                page = page,
                // total_resultsが実データより多く、それを基に next_page が付き続けるため空ページも終端とみなす
                endReached = response.photos.isEmpty() || response.nextPage == null,
            )
        }

    override suspend fun getPhoto(id: Long): Outcome<Photo> =
        safeCall(Throwable::toPhotoError) { api.getPhoto(id).toDomain() }
}
