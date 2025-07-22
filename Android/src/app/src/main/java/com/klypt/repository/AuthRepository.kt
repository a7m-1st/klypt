package com.klpyt.repository

import com.klpyt.network.AuthApiService
import com.klpyt.network.LoginRequest
import com.klpyt.network.LoginResponse
import com.klpyt.storage.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(firstName: String, lastName: String): Result<Unit> {
        return try {
            val response = apiService.login(LoginRequest(firstName, lastName))

            if(response.isSuccessful) {
                response.body()?.let {loginRes: LoginResponse ->
                    tokenManager.saveToken(loginRes.token)
                    tokenManager.saveUser(loginRes.user)
                    Result.success(Unit)
                } ?: Result.failure(Exception("Empty Response"))
            } else {
                Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }
}