package com.klypt.network

import android.service.autofill.UserData
import com.klypt.data.User
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApiService {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterRequest>
}

data class LoginRequest(
    val firstName: String,
    val lastName: String
)

data class RegisterRequest(
    val firstName: String,
    val lastName: String
)

data class LoginResponse(
    val token: String,
    val user: User
)