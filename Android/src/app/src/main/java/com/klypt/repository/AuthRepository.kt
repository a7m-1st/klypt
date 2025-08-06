package com.klypt.repository

import com.klypt.data.UserRole
import com.klypt.data.models.Student
import com.klypt.data.models.Educator
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.network.AuthApiService
import com.klypt.network.LoginRequest
import com.klypt.network.LoginResponse
import com.klypt.storage.TokenManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val tokenManager: TokenManager,
    private val studentRepository: StudentRepository,
    private val educatorRepository: EducatorRepository
) {
    suspend fun login(firstName: String, lastName: String, userRole: UserRole = UserRole.STUDENT): Result<Unit> {
        return try {
            val response = apiService.login(LoginRequest(firstName, lastName))

            if(response.isSuccessful) {
                response.body()?.let { loginRes: LoginResponse ->
                    tokenManager.saveToken(loginRes.token)
                    
                    // Save user data to local CouchDB using appropriate repository based on role
                    when (userRole) {
                        UserRole.STUDENT -> {
                            val studentData = mapOf(
                                "_id" to "${firstName}_${lastName}",
                                "type" to "student",
                                "firstName" to firstName,
                                "lastName" to lastName,
                                "recoveryCode" to "",
                                "enrolledClassIds" to emptyList<String>(),
                                "createdAt" to System.currentTimeMillis().toString(),
                                "updatedAt" to System.currentTimeMillis().toString()
                            )
                            studentRepository.save(studentData)
                        }
                        UserRole.EDUCATOR -> {
                            val educatorData = mapOf(
                                "_id" to "${firstName}_${lastName}",
                                "type" to "educator",
                                "fullName" to "$firstName $lastName",
                                "age" to 0,
                                "currentJob" to "",
                                "instituteName" to "",
                                "phoneNumber" to "",
                                "verified" to false,
                                "recoveryCode" to "",
                                "classIds" to emptyList<String>()
                            )
                            educatorRepository.save(educatorData)
                        }
                    }
                    
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

    //TODO(); SEE WHY THIS IS NOT USED
    suspend fun logout() {
        tokenManager.clearAll()
    }
    
    /**
     * Check if user is currently logged in based on token presence
     */
    suspend fun isUserLoggedIn(): Boolean {
        return tokenManager.getToken() != null
    }
    
    /**
     * Get user from local CouchDB using appropriate repository based on role
     * For educators, uses phone number as identifier
     */
    suspend fun getUserFromCouchDB(firstName: String, lastName: String, userRole: UserRole = UserRole.STUDENT): Result<Map<String, Any>?> {
        return try {
            val userData = when (userRole) {
                UserRole.STUDENT -> {
                    val userId = "${firstName}_${lastName}"
                    var studentData = studentRepository.get(userId)
                    
                    // Check if student exists properly (has firstName and lastName)
                    // If not, create a complete student record for offline login
                    if (!studentData.containsKey("firstName") || !studentData.containsKey("lastName") || 
                        studentData["firstName"] == null || studentData["lastName"] == null) {
                        
                        // Create a complete student record for offline login
                        studentData = mapOf(
                            "_id" to userId,
                            "type" to "student",
                            "firstName" to firstName,
                            "lastName" to lastName,
                            "recoveryCode" to "",
                            "enrolledClassIds" to emptyList<String>(),
                            "createdAt" to System.currentTimeMillis().toString(),
                            "updatedAt" to System.currentTimeMillis().toString()
                        )
                        
                        // Save the complete student record
                        studentRepository.save(studentData)
                        android.util.Log.d("AuthRepository", "Created complete student record for $firstName $lastName during offline login")
                    }
                    
                    studentData
                }
                UserRole.EDUCATOR -> {
                    // For educators, firstName is actually the phone number during login
                    val phoneNumber = firstName
                    getEducatorByPhoneNumber(phoneNumber)
                }
            }
            
            if (userData.isNotEmpty() && userData["_id"] != null) {
                Result.success(userData)
            } else {
                Result.failure(Exception("User not found in local database"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get educator by phone number from the database
     */
    private suspend fun getEducatorByPhoneNumber(phoneNumber: String): Map<String, Any> {
        return try {
            // Search all educators for one with matching phone number
            val allEducators = educatorRepository.getAllEducators()
            val educator = allEducators.find { it["phoneNumber"] == phoneNumber }
            educator ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
    
    /**
     * Search users in CouchDB by name pattern based on role
     */
    suspend fun searchUsersInCouchDB(query: String, userRole: UserRole = UserRole.STUDENT): Result<List<Any>> {
        return try {
            when (userRole) {
                UserRole.STUDENT -> {
                    val studentsData = studentRepository.searchStudents(query)
                    val students = studentsData.map { studentData ->
                        Student(
                            _id = studentData["_id"] as? String ?: "",
                            firstName = studentData["firstName"] as? String ?: "",
                            lastName = studentData["lastName"] as? String ?: "",
                            recoveryCode = studentData["recoveryCode"] as? String ?: "",
                            enrolledClassIds = (studentData["enrolledClassIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            createdAt = studentData["createdAt"] as? String ?: "",
                            updatedAt = studentData["updatedAt"] as? String ?: ""
                        )
                    }
                    Result.success(students)
                }
                UserRole.EDUCATOR -> {
                    val educatorsData = educatorRepository.searchEducators(query)
                    val educators = educatorsData.map { educatorData ->
                        Educator(
                            _id = educatorData["_id"] as? String ?: "",
                            fullName = educatorData["fullName"] as? String ?: "",
                            age = educatorData["age"] as? Int ?: 0,
                            currentJob = educatorData["currentJob"] as? String ?: "",
                            instituteName = educatorData["instituteName"] as? String ?: "",
                            phoneNumber = educatorData["phoneNumber"] as? String ?: "",
                            verified = educatorData["verified"] as? Boolean ?: false,
                            recoveryCode = educatorData["recoveryCode"] as? String ?: "",
                            classIds = (educatorData["classIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        )
                    }
                    Result.success(educators)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get all users from CouchDB based on role
     */
    suspend fun getAllUsersFromCouchDB(userRole: UserRole = UserRole.STUDENT): Result<List<UserWithStatus>> {
        return try {
            when (userRole) {
                UserRole.STUDENT -> {
                    val studentsData = studentRepository.getAllStudents()
                    val users = studentsData.map { studentData ->
                        UserWithStatus(
                            firstName = studentData["firstName"] as? String ?: "",
                            lastName = studentData["lastName"] as? String ?: "",
                            isActive = true // Assume all stored students are active
                        )
                    }
                    Result.success(users)
                }
                UserRole.EDUCATOR -> {
                    val educatorsData = educatorRepository.getAllEducators()
                    val users = educatorsData.map { educatorData ->
                        UserWithStatus(
                            firstName = educatorData["fullName"] as? String ?: "",
                            lastName = "", // Educator uses fullName instead of separate first/last
                            isActive = educatorData["verified"] as? Boolean ?: false
                        )
                    }
                    Result.success(users)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

data class UserWithStatus(
    val firstName: String,
    val lastName: String,
    val isActive: Boolean
)
