package com.example.intprogactivity.util

sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val exception: Exception, val message: String? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()

    inline fun <R> fold(
        onSuccess: (T) -> R,
        onFailure: (Exception) -> R
    ): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onFailure(exception)
        is Loading -> onFailure(Exception("Loading"))
    }
}
