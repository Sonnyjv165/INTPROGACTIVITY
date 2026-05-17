package com.example.intprogactivity.data.remote.amadeus

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class AmadeusAuthInterceptor(
    private val tokenManager: AmadeusTokenManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenManager.getValidToken() }
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $token")
            .build()
        val response = chain.proceed(request)
        if (response.code == 401) {
            response.close()
            tokenManager.invalidate()
            val newToken = runBlocking { tokenManager.getValidToken() }
            val retryRequest = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $newToken")
                .build()
            return chain.proceed(retryRequest)
        }
        return response
    }
}
