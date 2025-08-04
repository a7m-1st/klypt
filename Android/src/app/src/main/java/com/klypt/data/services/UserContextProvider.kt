package com.klypt.data.services

import com.klypt.data.DummyDataGenerator
import com.klypt.data.UserRole
import com.klypt.data.models.Student
import com.klypt.data.models.Educator
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service to provide current user context for the application
 * In a real app, this would integrate with authentication and session management
 */
@Singleton
class UserContextProvider @Inject constructor() {
    
    // For demo purposes, we'll use the first student from dummy data
    // In a real app, this would come from authentication state management
    private var currentUserId: String = "student_001"
    private var currentUserRole: UserRole = UserRole.STUDENT
    
    /**
     * Gets the current user ID
     */
    fun getCurrentUserId(): String {
        return currentUserId
    }
    
    /**
     * Gets the current user role
     */
    fun getCurrentUserRole(): UserRole {
        return currentUserRole
    }
    
    /**
     * Gets the default class code for the current user
     * For students, returns their first enrolled class
     * For educators, returns their first teaching class
     */
    fun getCurrentClassCode(): String {
        return when (currentUserRole) {
            UserRole.STUDENT -> {
                val students = DummyDataGenerator.generateSampleStudents()
                val student = students.find { it._id == currentUserId }
                student?.enrolledClassIds?.firstOrNull()?.let { classId ->
                    // Convert class ID to class code (simplified mapping)
                    when (classId) {
                        "class_cs101" -> "CS101"
                        "class_math201" -> "MATH201"
                        "class_phys101" -> "PHYS101"
                        "class_eng101" -> "ENG101"
                        "class_chem101" -> "CHEM101"
                        "class_hist101" -> "HIST101"
                        else -> "CS101" // Default fallback
                    }
                } ?: "CS101"
            }
            UserRole.EDUCATOR -> {
                val educators = DummyDataGenerator.generateSampleEducators()
                val educator = educators.find { it._id == currentUserId }
                educator?.classIds?.firstOrNull()?.let { classId ->
                    // Convert class ID to class code (simplified mapping)
                    when (classId) {
                        "class_cs101" -> "CS101"
                        "class_math201" -> "MATH201"
                        "class_phys101" -> "PHYS101"
                        "class_eng101" -> "ENG101"
                        "class_chem101" -> "CHEM101"
                        "class_hist101" -> "HIST101"
                        else -> "CS101" // Default fallback
                    }
                } ?: "CS101"
            }
        }
    }
    
    /**
     * Gets the current user's display name
     */
    fun getCurrentUserDisplayName(): String {
        return when (currentUserRole) {
            UserRole.STUDENT -> {
                val students = DummyDataGenerator.generateSampleStudents()
                val student = students.find { it._id == currentUserId }
                student?.let { "${it.firstName} ${it.lastName}" } ?: "Student"
            }
            UserRole.EDUCATOR -> {
                val educators = DummyDataGenerator.generateSampleEducators()
                val educator = educators.find { it._id == currentUserId }
                educator?.fullName ?: "Educator"
            }
        }
    }
    
    /**
     * Sets the current user context (for demo/testing purposes)
     */
    fun setCurrentUser(userId: String, userRole: UserRole) {
        currentUserId = userId
        currentUserRole = userRole
    }
    
    /**
     * Gets available classes for the current user
     */
    fun getAvailableClasses(): List<Pair<String, String>> {
        return when (currentUserRole) {
            UserRole.STUDENT -> {
                val students = DummyDataGenerator.generateSampleStudents()
                val student = students.find { it._id == currentUserId }
                student?.enrolledClassIds?.mapNotNull { classId ->
                    when (classId) {
                        "class_cs101" -> "CS101" to "Introduction to Computer Science"
                        "class_math201" -> "MATH201" to "Calculus II"
                        "class_phys101" -> "PHYS101" to "General Physics I"
                        "class_eng101" -> "ENG101" to "English Composition"
                        "class_chem101" -> "CHEM101" to "General Chemistry"
                        "class_hist101" -> "HIST101" to "World History"
                        else -> null
                    }
                } ?: listOf("CS101" to "Introduction to Computer Science")
            }
            UserRole.EDUCATOR -> {
                val educators = DummyDataGenerator.generateSampleEducators()
                val educator = educators.find { it._id == currentUserId }
                educator?.classIds?.mapNotNull { classId ->
                    when (classId) {
                        "class_cs101" -> "CS101" to "Introduction to Computer Science"
                        "class_math201" -> "MATH201" to "Calculus II"
                        "class_phys101" -> "PHYS101" to "General Physics I"
                        "class_eng101" -> "ENG101" to "English Composition"
                        "class_chem101" -> "CHEM101" to "General Chemistry"
                        "class_hist101" -> "HIST101" to "World History"
                        else -> null
                    }
                } ?: listOf("CS101" to "Introduction to Computer Science")
            }
        }
    }
}
