package com.gtkim.pexelssearch.di

import com.gtkim.pexelssearch.BuildConfig
import com.gtkim.pexelssearch.data.remote.PexelsApi
import com.gtkim.pexelssearch.data.remote.PexelsApiKeyInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import javax.inject.Singleton

private const val BASE_URL = "https://api.pexels.com/"
private const val CONTENT_TYPE_JSON = "application/json"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        // BASIC より詳細なレベルは Authorization ヘッダーも出力するため、APIキーがログに残る
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(PexelsApiKeyInterceptor(BuildConfig.PEXELS_API_KEY))
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory(CONTENT_TYPE_JSON.toMediaType()))
        .build()

    @Provides
    @Singleton
    fun providePexelsApi(retrofit: Retrofit): PexelsApi = retrofit.create()
}
