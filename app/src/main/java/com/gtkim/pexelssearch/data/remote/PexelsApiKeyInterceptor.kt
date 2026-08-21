package com.gtkim.pexelssearch.data.remote

import okhttp3.Interceptor
import okhttp3.Response

private const val HEADER_AUTHORIZATION = "Authorization"

class PexelsApiKeyInterceptor(private val apiKey: String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
            .newBuilder()
            .header(HEADER_AUTHORIZATION, apiKey)
            .build()
        return chain.proceed(request)
    }
}
