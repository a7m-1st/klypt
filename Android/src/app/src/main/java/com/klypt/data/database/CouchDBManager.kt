package com.klypt.data.database

import android.content.Context
import com.couchbase.lite.*
import com.klypt.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CouchDBManager @Inject constructor(
    private val context: Context
) {
    private lateinit var database: Database
    
    init {
        initializeDatabase()
    }
    
    private fun initializeDatabase() {
        CouchbaseLite.init(context)
        
        val config = DatabaseConfigurationFactory.create()
        database = Database("klypt_db", config)
        
        // Create indexes for better query performance
        createIndexes()
    }
    
    private fun createIndexes() {
        // Create index for user queries
        val userIndex = IndexBuilder.valueIndex(
            ValueIndexItem.property("type"),
            ValueIndexItem.property("firstName"),
            ValueIndexItem.property("lastName")
        )
        database.createIndex("user_index", userIndex)
    }
    
    suspend fun saveUser(user: User, firstName: String, lastName: String): kotlin.Result<String> = withContext(Dispatchers.IO) {
        try {
            val document = MutableDocument()
            document.setString("type", "user")
            document.setString("firstName", firstName)
            document.setString("lastName", lastName)
            document.setString("name", user.name)
            document.setLong("createdAt", System.currentTimeMillis())
            document.setLong("updatedAt", System.currentTimeMillis())
            
            database.save(document)
            kotlin.Result.success(document.id)
        } catch (e: Exception) {
            kotlin.Result.failure(CouchDBException("Failed to save user: ${e.message}", e))
        }
    }
    
    suspend fun getUserByName(firstName: String, lastName: String): kotlin.Result<User?> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.database(database))
                .where(
                    Expression.property("type").equalTo(Expression.string("user"))
                        .and(Expression.property("firstName").equalTo(Expression.string(firstName)))
                        .and(Expression.property("lastName").equalTo(Expression.string(lastName)))
                )
                .orderBy(Ordering.property("updatedAt").descending())
                .limit(Expression.intValue(1))
            
            val resultSet = query.execute()
            val result = resultSet.next()
            
            if (result != null) {
                val dict = result.getDictionary(database.name)
                val user = User(
                    name = dict?.getString("name") ?: ""
                )
                kotlin.Result.success(user)
            } else {
                kotlin.Result.success(null)
            }
        } catch (e: Exception) {
            kotlin.Result.failure(CouchDBException("Failed to query user: ${e.message}", e))
        }
    }
    
    suspend fun updateUser(firstName: String, lastName: String, newUser: User): kotlin.Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(
                    Expression.property("type").equalTo(Expression.string("user"))
                        .and(Expression.property("firstName").equalTo(Expression.string(firstName)))
                        .and(Expression.property("lastName").equalTo(Expression.string(lastName)))
                )
                .limit(Expression.intValue(1))
            
            val resultSet = query.execute()
            val result = resultSet.next()
            
            if (result != null) {
                val docId = result.getString(0)
                val document = database.getDocument(docId ?: "")?.toMutable()
                
                document?.let { doc ->
                    doc.setString("name", newUser.name)
                    doc.setLong("updatedAt", System.currentTimeMillis())
                    database.save(doc)
                    kotlin.Result.success(true)
                } ?: kotlin.Result.success(false)
            } else {
                kotlin.Result.success(false)
            }
        } catch (e: Exception) {
            kotlin.Result.failure(CouchDBException("Failed to update user: ${e.message}", e))
        }
    }
    
    suspend fun deleteUser(firstName: String, lastName: String): kotlin.Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.expression(Meta.id))
                .from(DataSource.database(database))
                .where(
                    Expression.property("type").equalTo(Expression.string("user"))
                        .and(Expression.property("firstName").equalTo(Expression.string(firstName)))
                        .and(Expression.property("lastName").equalTo(Expression.string(lastName)))
                )
            
            val resultSet = query.execute()
            var deletedCount = 0
            
            for (result in resultSet) {
                val docId = result.getString(0)
                val document = database.getDocument(docId ?: "")
                document?.let { 
                    database.delete(it)
                    deletedCount++
                }
            }
            
            kotlin.Result.success(deletedCount > 0)
        } catch (e: Exception) {
            kotlin.Result.failure(CouchDBException("Failed to delete user: ${e.message}", e))
        }
    }
    
    suspend fun getAllUsers(): kotlin.Result<List<UserData>> = withContext(Dispatchers.IO) {
        try {
            val query = QueryBuilder
                .select(SelectResult.all())
                .from(DataSource.database(database))
                .where(Expression.property("type").equalTo(Expression.string("user")))
                .orderBy(Ordering.property("createdAt").descending())
            
            val resultSet = query.execute()
            val users = mutableListOf<UserData>()
            
            for (result in resultSet) {
                val dict = result.getDictionary(database.name)
                dict?.let { d ->
                    users.add(
                        UserData(
                            firstName = d.getString("firstName") ?: "",
                            lastName = d.getString("lastName") ?: "",
                            user = User(name = d.getString("name") ?: ""),
                            createdAt = d.getLong("createdAt"),
                            updatedAt = d.getLong("updatedAt")
                        )
                    )
                }
            }
            
            kotlin.Result.success(users)
        } catch (e: Exception) {
            kotlin.Result.failure(CouchDBException("Failed to get all users: ${e.message}", e))
        }
    }
    
    fun close() {
        database.close()
    }
}

data class UserData(
    val firstName: String,
    val lastName: String,
    val user: User,
    val createdAt: Long,
    val updatedAt: Long
)

class CouchDBException(message: String, cause: Throwable? = null) : Exception(message, cause)
