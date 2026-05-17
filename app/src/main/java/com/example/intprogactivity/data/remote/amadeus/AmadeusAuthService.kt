package com.example.intprogactivity.data.remote.amadeus

import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST

interface AmadeusAuthService {
    @FormUrlEncoded
    @POST("v1/security/oauth2/token")
    suspend fun getToken(
        @Field("grant_type") grantType: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String
    ): AmadeusTokenResponse
}
