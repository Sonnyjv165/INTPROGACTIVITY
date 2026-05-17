package com.example.intprogactivity.data.remote.amadeus

class AmadeusTokenManager(
    private val authService: AmadeusAuthService,
    private val clientId: String,
    private val clientSecret: String
) {
    private var token: String? = null
    private var tokenExpiry: Long = 0L

    suspend fun getValidToken(): String {
        if (token == null || System.currentTimeMillis() >= tokenExpiry - 60_000L) {
            refreshToken()
        }
        return token ?: error("Failed to obtain Amadeus token")
    }

    fun invalidate() {
        token = null
        tokenExpiry = 0L
    }

    private suspend fun refreshToken() {
        val response = authService.getToken(
            grantType = "client_credentials",
            clientId = clientId,
            clientSecret = clientSecret
        )
        token = response.accessToken
        tokenExpiry = System.currentTimeMillis() + (response.expiresIn * 1000L)
    }
}
