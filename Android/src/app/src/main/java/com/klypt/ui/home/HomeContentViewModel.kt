/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klypt.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.UserRole
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Student
import com.klypt.data.models.GameStats
import com.klypt.data.models.QuizStats
import com.klypt.data.models.LearningProgress
import com.klypt.data.repository.EducationalContentRepository
import com.klypt.data.repositories.GameStatsRepository
import com.klypt.data.repositories.ClassDocumentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val userRole: UserRole = UserRole.STUDENT,
    val currentUser: Any? = null, // Student or Educator
    val recentKlyps: List<Klyp> = emptyList(),
    val myClasses: List<ClassDocument> = emptyList(),
    val upcomingAssignments: List<Pair<String, String>> = emptyList(), // Pair of (className, assignment)
    val featuredContent: Map<String, Any> = emptyMap(),
    val classStatistics: Map<String, Any> = emptyMap(),
    val gameStats: GameStats = GameStats(),
    val quizStats: QuizStats = QuizStats(),
    val learningProgress: List<LearningProgress> = emptyList(),
    val errorMessage: String? = null
)

/**
 * ViewModel for the Home screen that manages educational content and user-specific data.
 * Provides different content based on user role (Student vs Educator).
 */
@HiltViewModel
class HomeContentViewModel @Inject constructor(
    private val contentRepository: EducationalContentRepository,
    private val gameStatsRepository: GameStatsRepository,
    private val classRepository: ClassDocumentRepository,
    private val userContextProvider: com.klypt.data.services.UserContextProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Load data for the current user from database
        loadCurrentUserData()
    }

    /**
     * Load data for the current authenticated user
     */
    private fun loadCurrentUserData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val currentUserId = userContextProvider.getCurrentUserId()
                val currentUserRole = userContextProvider.getCurrentUserRole()
                
                when (currentUserRole) {
                    UserRole.STUDENT -> loadStudentContent(currentUserId)
                    UserRole.EDUCATOR -> loadEducatorContent(currentUserId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load user data: ${e.message}"
                )
            }
        }
    }

    /**
     * Load student-specific content
     */
    fun loadStudentContent(studentId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, userRole = UserRole.STUDENT)

                // Get student details
                val student = contentRepository.getStudentById(studentId)

                if (student != null) {
                    // Load game statistics
                    val gameStats = gameStatsRepository.getGameStats(studentId)
                    val quizStats = gameStatsRepository.getQuizStats(studentId)
                    val learningProgress = gameStatsRepository.getLearningProgress(studentId)
                    
                    // Load student's classes
                    contentRepository.getClassesForStudent(studentId)
                        .catch { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Failed to load classes: ${error.message}"
                            )
                        }
                        .collect { classes ->
                            _uiState.value = _uiState.value.copy(myClasses = classes)
                        }

                    // Load recent Klyps for student's classes
                    contentRepository.getRecentKlypsForStudent(studentId)
                        .catch { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Failed to load recent content: ${error.message}"
                            )
                        }
                        .collect { klyps ->
                            _uiState.value = _uiState.value.copy(recentKlyps = klyps)
                        }

                    // Load upcoming assignments
                    val assignments = contentRepository.getUpcomingAssignmentsForStudent(studentId)

                    _uiState.value = _uiState.value.copy(
                        currentUser = student,
                        upcomingAssignments = assignments,
                        gameStats = gameStats,
                        quizStats = quizStats,
                        learningProgress = learningProgress,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Student not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load student content: ${e.message}"
                )
            }
        }
    }

    /**
     * Load educator-specific content
     */
    fun loadEducatorContent(educatorId: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, userRole = UserRole.EDUCATOR)

                // Get educator details
                val educator = contentRepository.getEducatorById(educatorId)

                if (educator != null) {
                    // Load educator's classes
                    contentRepository.getClassesForEducator(educatorId)
                        .catch { error ->
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "Failed to load classes: ${error.message}"
                            )
                        }
                        .collect { classes ->
                            _uiState.value = _uiState.value.copy(myClasses = classes)
                        }

                    // Load class statistics
                    val statistics = contentRepository.getClassStatisticsForEducator(educatorId)

                    // Load all Klyps for educator's classes
                    val allKlyps = mutableListOf<Klyp>()
                    educator.classIds.forEach { classId ->
                        // Convert class ID to class code (simplified for dummy data)
                        val classCode =
                            _uiState.value.myClasses.find { it._id == classId }?.classCode
                        if (classCode != null) {
                            contentRepository.getKlypsForClass(classCode)
                                .catch { /* ignore errors for individual classes */ }
                                .collect { klyps ->
                                    allKlyps.addAll(klyps)
                                }
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        currentUser = educator,
                        recentKlyps = allKlyps.take(5), // Show 5 most recent
                        classStatistics = statistics,
                        isLoading = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Educator not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load educator content: ${e.message}"
                )
            }
        }
    }

    /**
     * Search for Klyps
     */
    fun searchContent(query: String) {
        if (query.isBlank()) {
            // Reset to original content by reloading current user data
            loadCurrentUserData()
            return
        }

        viewModelScope.launch {
            try {
                contentRepository.searchKlyps(query)
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "Search failed: ${error.message}"
                        )
                    }
                    .collect { searchResults ->
                        _uiState.value = _uiState.value.copy(
                            recentKlyps = searchResults
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Search failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear any error messages
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    /**
     * Refresh all data
     */
    fun refresh() {
        // Clear cache to force refresh from database
        contentRepository.clearCache()
        
        // Reload current user data
        loadCurrentUserData()
    }

    /**
     * Get quick stats for dashboard
     */
    fun getQuickStats(): Map<String, Int> {
        return mapOf(
            "totalKlyps" to _uiState.value.recentKlyps.size,
            "totalClasses" to _uiState.value.myClasses.size,
            "upcomingAssignments" to _uiState.value.upcomingAssignments.size,
            "featuredItems" to ((_uiState.value.featuredContent["recentKlyps"] as? List<*>)?.size
                ?: 0)
        )
    }
    
    /**
     * Logout the current user and clear all data
     */
    /**
     * Delete a class
     */
    fun deleteClass(classDocument: com.klypt.data.models.ClassDocument) {
        viewModelScope.launch {
            try {
                // Delete from database using the class repository
                val documentId = classDocument._id
                android.util.Log.d("HomeContentViewModel", "Attempting to delete class with ID: $documentId, title: ${classDocument.classTitle}")
                val success = classRepository.delete(documentId)
                
                if (success) {
                    android.util.Log.d("HomeContentViewModel", "Class deletion successful, updating UI state")
                    // Update UI state by removing the class from the list
                    val currentClasses = _uiState.value.myClasses.toMutableList()
                    val removedCount = currentClasses.removeAll { it._id == documentId }
                    android.util.Log.d("HomeContentViewModel", "Removed $removedCount classes from UI list")
                    
                    _uiState.value = _uiState.value.copy(
                        myClasses = currentClasses
                    )
                } else {
                    android.util.Log.e("HomeContentViewModel", "Class deletion failed")
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete class"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeContentViewModel", "Exception during class deletion: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete class: ${e.message}"
                )
            }
        }
    }

    fun logout() {
        // Clear user context which will also clear tokens and stored data
        userContextProvider.clearUserContext()
        
        // Clear repository cache
        contentRepository.clearCache()
        
        // Reset UI state to initial state
        _uiState.value = HomeUiState()
    }
}
