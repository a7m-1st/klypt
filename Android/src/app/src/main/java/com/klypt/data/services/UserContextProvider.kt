package com.klypt.data.services

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
    
    // User context state - updated on successful login
    private var currentUserId: String? = null
    private var currentUserRole: UserRole? = null
    private var currentPhoneNumber: String? = null // For educators
    
    /**
     * Gets the current user ID
     * Returns null if no user is logged in
     */
    fun getCurrentUserId(): String {
        // If we have a cached user ID, return it
        currentUserId?.let { return it }
        
        // If no cached user, check if we have a valid auth token
        val token = tokenManager.getToken()
        if (token != null) {
            // Try to restore user context from stored user data
            restoreUserContext()?.let { userId ->
                return userId
            }
        }
        
        // No authenticated user found - this should trigger login flow
        throw IllegalStateException("No authenticated user found. Please login first.")
    }
    
    /**
     * Gets the current user role
     * Returns null if no user is logged in
     */
    fun getCurrentUserRole(): UserRole {
        // If we have a cached user role, return it
        currentUserRole?.let { return it }
        
        // If no cached role, check if we have a valid auth token and restore context
        val token = tokenManager.getToken()
        if (token != null) {
            restoreUserContext()
            currentUserRole?.let { return it }
        }
        
        // No authenticated user found - this should trigger login flow
        throw IllegalStateException("No authenticated user found. Please login first.")
    }
    
    /**
     * Check if user is currently logged in
     */
    fun isLoggedIn(): Boolean {
        return tokenManager.getToken() != null && (currentUserId != null || restoreUserContext() != null)
    }
    
    /**
     * Sets the current user context after successful student login
     */
    fun setCurrentStudentUser(firstName: String, lastName: String) {
        currentUserId = "${firstName}_${lastName}"
        currentUserRole = UserRole.STUDENT
        currentPhoneNumber = null
        
        // Store user identification in token manager for persistence
        tokenManager.saveStudentIdentification(firstName, lastName)
    }
    
    /**
     * Sets the current user context after successful educator login/signup
     */
    fun setCurrentEducatorUser(phoneNumber: String, fullName: String? = null) {
        // For educators, we use phone number as the identifier
        currentUserId = phoneNumber
        currentUserRole = UserRole.EDUCATOR  
        currentPhoneNumber = phoneNumber
        
        // Store user identification in token manager for persistence
        tokenManager.saveEducatorIdentification(phoneNumber, fullName)
    }
    
    /**
     * Clears current user context (on logout)
     */
    fun clearUserContext() {
        currentUserId = null
        currentUserRole = null
        currentPhoneNumber = null
        tokenManager.clearAll()
    }
    
    /**
     * Attempts to restore user context from stored data
     */
    private fun restoreUserContext(): String? {
        return runBlocking {
            try {
                // Try to get stored student identification
                val storedStudent = tokenManager.getStudentIdentification()
                if (storedStudent != null) {
                    val (firstName, lastName) = storedStudent
                    val student = contentRepository.getStudentById("${firstName}_${lastName}")
                    if (student != null) {
                        currentUserId = "${firstName}_${lastName}"
                        currentUserRole = UserRole.STUDENT
                        return@runBlocking currentUserId
                    }
                }
                
                // Try to get stored educator identification
                val storedEducator = tokenManager.getEducatorIdentification()
                if (storedEducator != null) {
                    val (phoneNumber, fullName) = storedEducator
                    // For educators, try to find by phone number
                    val educator = findEducatorByPhone(phoneNumber)
                    if (educator != null) {
                        currentUserId = phoneNumber
                        currentUserRole = UserRole.EDUCATOR
                        currentPhoneNumber = phoneNumber
                        return@runBlocking currentUserId
                    }
                }
                
                null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Helper to find educator by phone number
     */
    private suspend fun findEducatorByPhone(phoneNumber: String): Educator? {
        return try {
            // Search for educator with this phone number
            // This is a simplified approach - in production you might have a dedicated method
            val allEducators = contentRepository.getAllEducators()
            allEducators.find { it.phoneNumber == phoneNumber }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Gets the default class code for the current user
     * For students, returns their first enrolled class
     * For educators, returns their first teaching class
     */
    fun getCurrentClassCode(): String {
        return runBlocking {
            try {
                when (getCurrentUserRole()) {
                    UserRole.STUDENT -> {
                        val student = contentRepository.getStudentById(getCurrentUserId())
                        student?.enrolledClassIds?.firstOrNull()?.let { classId ->
                            // Convert class ID to class code (simplified mapping)
                            convertClassIdToCode(classId)
                        } ?: "CS101" // Default fallback
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, we need to find the educator by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        val educator = findEducatorByPhone(phoneNumber)
                        educator?.classIds?.firstOrNull()?.let { classId ->
                            convertClassIdToCode(classId)
                        } ?: "CS101" // Default fallback
                    }
                }
            } catch (e: Exception) {
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
        return runBlocking {
            try {
                when (getCurrentUserRole()) {
                    UserRole.STUDENT -> {
                        val student = contentRepository.getStudentById(getCurrentUserId())
                        student?.let { "${it.firstName} ${it.lastName}" } ?: "Student"
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, find by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        val educator = findEducatorByPhone(phoneNumber)
                        educator?.fullName ?: "Educator"
                    }
                }
            } catch (e: Exception) {
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
        return runBlocking {
            try {
                when (getCurrentUserRole()) {
                    UserRole.STUDENT -> {
                        val student = contentRepository.getStudentById(getCurrentUserId())
                        student?.enrolledClassIds?.mapNotNull { classId ->
                            convertClassIdToNamePair(classId)
                        } ?: listOf("CS101" to "Introduction to Computer Science")
                    }
                    UserRole.EDUCATOR -> {
                        // For educators, find by phone number
                        val phoneNumber = currentPhoneNumber ?: getCurrentUserId()
                        val educator = findEducatorByPhone(phoneNumber)
                        educator?.classIds?.mapNotNull { classId ->
                            convertClassIdToNamePair(classId)
                        } ?: listOf("CS101" to "Introduction to Computer Science")
                    }
                }
            } catch (e: Exception) {
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
