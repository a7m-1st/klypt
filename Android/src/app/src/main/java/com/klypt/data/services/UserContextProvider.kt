package com.klypt.data.services

import android.util.Log
import com.klypt.data.UserRole
import com.klypt.data.models.Student
import com.klypt.data.models.Educator
import com.klypt.data.repository.EducationalContentRepository
import com.klypt.storage.TokenManager
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to provide current user context for the application
 * Integrates with authentication system to track logged-in user state
 */
@Singleton
class UserContextProvider @Inject constructor(
    private val contentRepository: EducationalContentRepository,
    private val tokenManager: TokenManager
) {
    
    companion object {
        private const val TAG = "UserContextProvider"
    }
    
    // User context state - updated on successful login
    private var currentUserId: String? = null
    private var currentUserRole: UserRole? = null
    private var currentPhoneNumber: String? = null // For educators
    
    /**
     * Gets the current user ID
     * Returns null if no user is logged in
     */
    fun getCurrentUserId(): String {
        Log.d(TAG, "getCurrentUserId() called")
        Log.d(TAG, "Cached currentUserId: $currentUserId")
        
        // If we have a cached user ID, return it
        currentUserId?.let { 
            Log.d(TAG, "Returning cached user ID: $it")
            return it 
        }
        
        // If no cached user, check if we have a valid auth token
        val token = tokenManager.getToken()
        Log.d(TAG, "Token from tokenManager: ${if (token != null) "exists" else "null"}")
        
        if (token != null) {
            // Try to restore user context from stored user data
            Log.d(TAG, "Attempting to restore user context...")
            restoreUserContext()?.let { userId ->
                Log.d(TAG, "Successfully restored user ID: $userId")
                return userId
            }
        }
        
        // No authenticated user found - this should trigger login flow
        Log.e(TAG, "No authenticated user found. Throwing IllegalStateException")
        throw IllegalStateException("No authenticated user found. Please login first.")
    }
    
    /**
     * Gets the current user role
     * Returns null if no user is logged in
     */
    fun getCurrentUserRole(): UserRole {
        Log.d(TAG, "getCurrentUserRole() called")
        Log.d(TAG, "Cached currentUserRole: $currentUserRole")
        
        // If we have a cached user role, return it
        currentUserRole?.let { 
            Log.d(TAG, "Returning cached user role: $it")
            return it 
        }
        
        // If no cached role, check if we have a valid auth token and restore context
        val token = tokenManager.getToken()
        Log.d(TAG, "Token from tokenManager: ${if (token != null) "exists" else "null"}")
        
        if (token != null) {
            Log.d(TAG, "Attempting to restore user context for role...")
            restoreUserContext()
            currentUserRole?.let { 
                Log.d(TAG, "Successfully restored user role: $it")
                return it 
            }
        }
        
        // No authenticated user found - this should trigger login flow
        Log.e(TAG, "No authenticated user role found. Throwing IllegalStateException")
        throw IllegalStateException("No authenticated user found. Please login first.")
    }
    
    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        Log.d(TAG, "isLoggedIn() called")
        val hasToken = tokenManager.getToken() != null
        val hasUserId = currentUserId != null
        Log.d(TAG, "Has token: $hasToken, Has cached userId: $hasUserId")
        
        val result = hasToken && (hasUserId || restoreUserContext() != null)
        Log.d(TAG, "isLoggedIn result: $result")
        return result
    }
    
    /**
     * Sets the current user context after successful student login
     */
    fun setCurrentStudentUser(firstName: String, lastName: String) {
        Log.d(TAG, "setCurrentStudentUser() called with firstName: $firstName, lastName: $lastName")
        
        currentUserId = "${firstName}_${lastName}"
        currentUserRole = UserRole.STUDENT
        currentPhoneNumber = null
        
        Log.d(TAG, "Set student user context - ID: $currentUserId, Role: $currentUserRole")
        
        // Store user identification in token manager for persistence
        tokenManager.saveStudentIdentification(firstName, lastName)
        Log.d(TAG, "Saved student identification to token manager")
    }
    
    /**
     * Sets the current user context after successful educator login/signup
     */
    fun setCurrentEducatorUser(phoneNumber: String, fullName: String? = null) {
        Log.d(TAG, "setCurrentEducatorUser() called with phoneNumber: $phoneNumber, fullName: $fullName")
        
        // For educators, we use phone number as the identifier
        currentUserId = phoneNumber
        currentUserRole = UserRole.EDUCATOR  
        currentPhoneNumber = phoneNumber
        
        Log.d(TAG, "Set educator user context - ID: $currentUserId, Role: $currentUserRole, Phone: $currentPhoneNumber")
        
        // Store user identification in token manager for persistence
        tokenManager.saveEducatorIdentification(phoneNumber, fullName)
        Log.d(TAG, "Saved educator identification to token manager")
    }
    
    /**
     * Clears current user context (on logout)
     */
    fun clearUserContext() {
        Log.d(TAG, "clearUserContext() called")
        Log.d(TAG, "Clearing user context - Previous state: ID=$currentUserId, Role=$currentUserRole, Phone=$currentPhoneNumber")
        
        currentUserId = null
        currentUserRole = null
        currentPhoneNumber = null
        tokenManager.clearAll()
        
        Log.d(TAG, "User context cleared successfully")
    }
    
    /**
     * Attempts to restore user context from stored data
     */
    private fun restoreUserContext(): String? {
        Log.d(TAG, "restoreUserContext() called")
        
        return runBlocking {
            try {
                // Try to get stored student identification
                Log.d(TAG, "Attempting to get stored student identification...")
                val storedStudent = tokenManager.getStudentIdentification()
                Log.d(TAG, "Stored student data: $storedStudent")
                
                if (storedStudent != null) {
                    val (firstName, lastName) = storedStudent
                    val studentId = "${firstName}_${lastName}"
                    Log.d(TAG, "Looking up student with ID: $studentId")
                    
                    val student = contentRepository.getStudentById(studentId)
                    Log.d(TAG, "Student lookup result: ${if (student != null) "found" else "not found"}")
                    
                    if (student != null) {
                        currentUserId = studentId
                        currentUserRole = UserRole.STUDENT
                        Log.d(TAG, "Successfully restored student context: ID=$currentUserId, Role=$currentUserRole")
                        return@runBlocking currentUserId
                    }
                }
                
                // Try to get stored educator identification
                Log.d(TAG, "Attempting to get stored educator identification...")
                val storedEducator = tokenManager.getEducatorIdentification()
                Log.d(TAG, "Stored educator data: $storedEducator")
                
                if (storedEducator != null) {
                    val (phoneNumber, fullName) = storedEducator
                    Log.d(TAG, "Looking up educator with phone: $phoneNumber")
                    
                    // For educators, try to find by phone number
                    val educator = findEducatorByPhone(phoneNumber)
                    Log.d(TAG, "Educator lookup result: ${if (educator != null) "found" else "not found"}")
                    
                    if (educator != null) {
                        currentUserId = phoneNumber
                        currentUserRole = UserRole.EDUCATOR
                        currentPhoneNumber = phoneNumber
                        Log.d(TAG, "Successfully restored educator context: ID=$currentUserId, Role=$currentUserRole, Phone=$currentPhoneNumber")
                        return@runBlocking currentUserId
                    }
                }
                
                Log.w(TAG, "Failed to restore user context - no valid stored data found")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Exception during user context restoration", e)
                null
            }
        }
    }
    
    /**
     * Helper to find educator by phone number
     */
    private suspend fun findEducatorByPhone(phoneNumber: String): Educator? {
        Log.d(TAG, "findEducatorByPhone() called with phoneNumber: $phoneNumber")
        
        return try {
            // Search for educator with this phone number
            // This is a simplified approach - in production you might have a dedicated method
            Log.d(TAG, "Fetching all educators from repository...")
            val allEducators = contentRepository.getAllEducators()
            Log.d(TAG, "Total educators found: ${allEducators.size}")
            
            val educator = allEducators.find { it.phoneNumber == phoneNumber }
            Log.d(TAG, "Educator with phone $phoneNumber: ${if (educator != null) "found" else "not found"}")
            
            if (educator != null) {
                Log.d(TAG, "Found educator: ${educator.fullName} with phone: ${educator.phoneNumber}")
            }
            
            educator
        } catch (e: Exception) {
            Log.e(TAG, "Exception while finding educator by phone", e)
            null
        }
    }
    
    /**
     * Gets the default class code for the current user
     * For students, returns their first enrolled class
     * For educators, returns their first teaching class
     */
    fun getCurrentClassCode(): String {
        Log.d(TAG, "getCurrentClassCode() called")
        
        return runBlocking {
            try {
                val userRole = getCurrentUserRole()
                Log.d(TAG, "Current user role: $userRole")
                
                when (userRole) {
                    UserRole.STUDENT -> {
                        val userId = getCurrentUserId()
                        Log.d(TAG, "Getting class code for student: $userId")
                        
                        val student = contentRepository.getStudentById(userId)
                        Log.d(TAG, "Student data retrieved: ${if (student != null) "found" else "not found"}")
                        
                        if (student != null) {
                            Log.d(TAG, "Student enrolled classes: ${student.enrolledClassIds}")
                        }
                        
                        student?.enrolledClassIds?.firstOrNull()?.let { classId ->
                            Log.d(TAG, "First enrolled class ID: $classId")
                            // Convert class ID to class code (simplified mapping)
                            val classCode = convertClassIdToCode(classId)
                            Log.d(TAG, "Converted to class code: $classCode")
                            classCode
                        } ?: run {
                            Log.w(TAG, "No enrolled classes found, using default: CS101")
                            "CS101" // Default fallback
                        }
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, we need to find the educator by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        Log.d(TAG, "Getting class code for educator: $phoneNumber")
                        
                        val educator = findEducatorByPhone(phoneNumber)
                        Log.d(TAG, "Educator data retrieved: ${if (educator != null) "found" else "not found"}")
                        
                        if (educator != null) {
                            Log.d(TAG, "Educator teaching classes: ${educator.classIds}")
                        }
                        
                        educator?.classIds?.firstOrNull()?.let { classId ->
                            Log.d(TAG, "First teaching class ID: $classId")
                            val classCode = convertClassIdToCode(classId)
                            Log.d(TAG, "Converted to class code: $classCode")
                            classCode
                        } ?: run {
                            Log.w(TAG, "No teaching classes found, using default: CS101")
                            "CS101" // Default fallback
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while getting current class code", e)
                "CS101" // Fallback on error
            }
        }
    }
    
    /**
     * Helper method to convert class ID to class code
     */
    private fun convertClassIdToCode(classId: String): String {
        return when (classId) {
            "class_cs101" -> "CS101"
            "class_math201" -> "MATH201" 
            "class_phys101" -> "PHYS101"
            "class_eng101" -> "ENG101"
            "class_chem101" -> "CHEM101"
            "class_hist101" -> "HIST101"
            else -> "CS101" // Default fallback
        }
    }
    
    /**
     * Gets the current user's display name
     */
    fun getCurrentUserDisplayName(): String {
        Log.d(TAG, "getCurrentUserDisplayName() called")
        
        return runBlocking {
            try {
                val userRole = getCurrentUserRole()
                Log.d(TAG, "Getting display name for user role: $userRole")
                
                when (userRole) {
                    UserRole.STUDENT -> {
                        val userId = getCurrentUserId()
                        Log.d(TAG, "Getting display name for student: $userId")
                        
                        val student = contentRepository.getStudentById(userId)
                        Log.d(TAG, "Student data for display name: ${if (student != null) "found" else "not found"}")
                        
                        student?.let { 
                            val displayName = "${it.firstName} ${it.lastName}"
                            Log.d(TAG, "Student display name: $displayName")
                            displayName
                        } ?: run {
                            Log.w(TAG, "Student not found, using default: Student")
                            "Student"
                        }
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, find by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        Log.d(TAG, "Getting display name for educator: $phoneNumber")
                        
                        val educator = findEducatorByPhone(phoneNumber)
                        Log.d(TAG, "Educator data for display name: ${if (educator != null) "found" else "not found"}")
                        
                        educator?.fullName?.let { name ->
                            Log.d(TAG, "Educator display name: $name")
                            name
                        } ?: run {
                            Log.w(TAG, "Educator name not found, using default: Educator")
                            "Educator"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while getting display name", e)
                when (currentUserRole) {
                    UserRole.STUDENT -> "Student"
                    UserRole.EDUCATOR -> "Educator"
                    null -> "User"
                }
            }
        }
    }
    
    /**
     * Sets the current user context (for testing purposes only)
     * @deprecated Use setCurrentStudentUser or setCurrentEducatorUser instead
     */
    @Deprecated("Use role-specific methods instead")
    fun setCurrentUser(userId: String, userRole: UserRole) {
        currentUserId = userId
        currentUserRole = userRole
        if (userRole == UserRole.EDUCATOR) {
            currentPhoneNumber = userId // Assume userId is phone number for educators
        }
    }
    
    /**
     * Gets available classes for the current user
     */
    fun getAvailableClasses(): List<Pair<String, String>> {
        Log.d(TAG, "getAvailableClasses() called")
        
        return runBlocking {
            try {
                val userRole = getCurrentUserRole()
                Log.d(TAG, "Getting available classes for user role: $userRole")
                
                when (userRole) {
                    UserRole.STUDENT -> {
                        val userId = getCurrentUserId()
                        Log.d(TAG, "Getting available classes for student: $userId")
                        
                        val student = contentRepository.getStudentById(userId)
                        Log.d(TAG, "Student data for classes: ${if (student != null) "found" else "not found"}")
                        
                        if (student != null) {
                            Log.d(TAG, "Student enrolled class IDs: ${student.enrolledClassIds}")
                        }
                        
                        student?.enrolledClassIds?.mapNotNull { classId ->
                            Log.d(TAG, "Converting class ID to name pair: $classId")
                            val pair = convertClassIdToNamePair(classId)
                            Log.d(TAG, "Converted to: $pair")
                            pair
                        } ?: run {
                            Log.w(TAG, "No student classes found, using default")
                            listOf("CS101" to "Introduction to Computer Science")
                        }
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, find by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        Log.d(TAG, "Getting available classes for educator: $phoneNumber")
                        
                        val educator = findEducatorByPhone(phoneNumber)
                        Log.d(TAG, "Educator data for classes: ${if (educator != null) "found" else "not found"}")
                        
                        if (educator != null) {
                            Log.d(TAG, "Educator class IDs: ${educator.classIds}")
                        }
                        
                        educator?.classIds?.mapNotNull { classId ->
                            Log.d(TAG, "Converting class ID to name pair: $classId")
                            val pair = convertClassIdToNamePair(classId)
                            Log.d(TAG, "Converted to: $pair")
                            pair
                        } ?: run {
                            Log.w(TAG, "No educator classes found, using default")
                            listOf("CS101" to "Introduction to Computer Science")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception while getting available classes", e)
                listOf("CS101" to "Introduction to Computer Science")
            }
        }
    }
    
    /**
     * Helper method to convert class ID to (code, name) pair
     */
    private fun convertClassIdToNamePair(classId: String): Pair<String, String>? {
        return when (classId) {
            "class_cs101" -> "CS101" to "Introduction to Computer Science"
            "class_math201" -> "MATH201" to "Calculus II"
            "class_phys101" -> "PHYS101" to "General Physics I"
            "class_eng101" -> "ENG101" to "English Composition"
            "class_chem101" -> "CHEM101" to "General Chemistry"
            "class_hist101" -> "HIST101" to "World History"
            else -> null
        }
    }
}
