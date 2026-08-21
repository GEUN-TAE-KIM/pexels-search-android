package com.gtkim.pexelssearch.data.remote

import com.gtkim.pexelssearch.data.remote.dto.PhotoDto
import com.gtkim.pexelssearch.data.remote.dto.SearchPhotosResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface PexelsApi {

    @GET("v1/search")
    suspend fun searchPhotos(
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): SearchPhotosResponse

    @GET("v1/photos/{id}")
    suspend fun getPhoto(@Path("id") id: Long): PhotoDto
}
