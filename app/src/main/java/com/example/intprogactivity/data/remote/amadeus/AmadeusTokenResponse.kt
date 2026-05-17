package com.example.intprogactivity.data.remote.amadeus

import com.google.gson.annotations.SerializedName

data class AmadeusTokenResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("token_type") val tokenType: String,
    @SerializedName("expires_in") val expiresIn: Int
)
