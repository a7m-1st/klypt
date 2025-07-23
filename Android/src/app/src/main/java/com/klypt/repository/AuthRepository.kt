package com.klypt.repository

import com.klypt.data.User
import com.klypt.network.AuthApiService
import com.klypt.network.LoginRequest
import com.klypt.network.LoginResponse
import com.klypt.storage.TokenManager
import com.klypt.domain.repository.UserRepository
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val userRepository: UserRepository
) {
    suspend fun login(firstName: String, lastName: String): kotlin.Result<Unit> {
        return try {
            val response: Response<LoginResponse> = Response.success(LoginResponse("test", User(firstName+lastName)))

            if(response.isSuccessful) {
                response.body()?.let {loginRes: LoginResponse ->
                    // Save to encrypted storage for immediate use
                    tokenManager.saveToken(loginRes.token)
                    tokenManager.saveUser(loginRes.user)
                    
                    // Save to CouchDB for offline access and data persistence
                    val saveResult = userRepository.saveUser(loginRes.user, firstName, lastName)
                    if (saveResult.isFailure) {
                        // Log the CouchDB error but don't fail the login process
                        // since the data is already saved in encrypted storage
                        saveResult.exceptionOrNull()?.printStackTrace()
                    }
                    
                    kotlin.Result.success(Unit)
                } ?: kotlin.Result.failure(Exception("Empty Response"))
            } else {
                kotlin.Result.failure(Exception("Login failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            kotlin.Result.failure(e)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }

    suspend fun logout() {
        tokenManager.clearAll()
    }
    
    /**
     * Get user data from CouchDB by firstName and lastName
     */
    suspend fun getUserFromCouchDB(firstName: String, lastName: String): kotlin.Result<com.klypt.data.User?> {
        return userRepository.getUserByName(firstName, lastName)
    }
    
    /**
     * Get all users stored in CouchDB
     */
    suspend fun getAllUsersFromCouchDB(): kotlin.Result<List<com.klypt.data.database.UserData>> {
        return userRepository.getAllUsers()
    }
    
    /**
     * Update user data in CouchDB
     */
    suspend fun updateUserInCouchDB(firstName: String, lastName: String, newUser: com.klypt.data.User): kotlin.Result<Boolean> {
        return userRepository.updateUser(firstName, lastName, newUser)
    }
    
    /**
     * Delete user data from CouchDB
     */
    suspend fun deleteUserFromCouchDB(firstName: String, lastName: String): kotlin.Result<Boolean> {
        return userRepository.deleteUser(firstName, lastName)
    }
    
    /**
     * Search users in CouchDB
     */
    suspend fun searchUsersInCouchDB(query: String): Result<List<com.klypt.data.database.UserData>> {
        return userRepository.searchUsers(query)
    }
}