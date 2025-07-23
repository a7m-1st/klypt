package com.klypt.data.repository

import com.klypt.data.User
import com.klypt.data.database.CouchDBManager
import com.klypt.data.database.UserData
import com.klypt.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val couchDBManager: CouchDBManager
) : UserRepository {
    
    override suspend fun saveUser(user: User, firstName: String, lastName: String): kotlin.Result<String> {
        return couchDBManager.saveUser(user, firstName, lastName)
    }
    
    override suspend fun getUserByName(firstName: String, lastName: String): kotlin.Result<User?> {
        return couchDBManager.getUserByName(firstName, lastName)
    }
    
    override suspend fun updateUser(firstName: String, lastName: String, newUser: User): kotlin.Result<Boolean> {
        return couchDBManager.updateUser(firstName, lastName, newUser)
    }
    
    override suspend fun deleteUser(firstName: String, lastName: String): kotlin.Result<Boolean> {
        return couchDBManager.deleteUser(firstName, lastName)
    }
    
    override suspend fun getAllUsers(): kotlin.Result<List<UserData>> {
        return couchDBManager.getAllUsers()
    }
    
    override suspend fun searchUsers(query: String): kotlin.Result<List<UserData>> {
        // Get all users and filter by query (firstName, lastName, or name)
        return getAllUsers().map { users ->
            users.filter { userData ->
                userData.firstName.contains(query, ignoreCase = true) ||
                userData.lastName.contains(query, ignoreCase = true) ||
                userData.user.name.contains(query, ignoreCase = true)
            }
        }
    }
}
