package com.klypt.domain.repository

import com.klypt.data.User
import com.klypt.data.database.UserData

interface UserRepository {
    suspend fun saveUser(user: User, firstName: String, lastName: String): kotlin.Result<String>
    suspend fun getUserByName(firstName: String, lastName: String): kotlin.Result<User?>
    suspend fun updateUser(firstName: String, lastName: String, newUser: User): kotlin.Result<Boolean>
    suspend fun deleteUser(firstName: String, lastName: String): kotlin.Result<Boolean>
    suspend fun getAllUsers(): kotlin.Result<List<UserData>>
    suspend fun searchUsers(query: String): kotlin.Result<List<UserData>>
}
