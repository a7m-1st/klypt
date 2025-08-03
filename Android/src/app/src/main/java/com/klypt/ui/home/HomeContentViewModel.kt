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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.DummyDataGenerator
import com.klypt.data.UserRole
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Student
import com.klypt.data.repository.EducationalContentRepository
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
    val errorMessage: String? = null
)

/**
 * ViewModel for the Home screen that manages educational content and user-specific data.
 * Provides different content based on user role (Student vs Educator).
 */
@HiltViewModel
class HomeContentViewModel @Inject constructor(
    private val contentRepository: EducationalContentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        // Initialize dummy data when ViewModel is created
        DummyDataGenerator.initializeDummyData()
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                // Load featured content for general home page display
                contentRepository.getFeaturedContent()
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load content: ${error.message}"
                        )
                    }
                    .collect { featured ->
                        _uiState.value = _uiState.value.copy(
                            featuredContent = featured,
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load initial data: ${e.message}"
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
                        val classCode = _uiState.value.myClasses.find { it._id == classId }?.classCode
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
            // Reset to original content
            when (_uiState.value.userRole) {
                UserRole.STUDENT -> {
                    val student = _uiState.value.currentUser as? Student
                    if (student != null) {
                        loadStudentContent(student._id)
                    }
                }
                UserRole.EDUCATOR -> {
                    val educator = _uiState.value.currentUser as? Educator
                    if (educator != null) {
                        loadEducatorContent(educator._id)
                    }
                }
            }
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
        when (_uiState.value.userRole) {
            UserRole.STUDENT -> {
                val student = _uiState.value.currentUser as? Student
                if (student != null) {
                    loadStudentContent(student._id)
                } else {
                    loadInitialData()
                }
            }
            UserRole.EDUCATOR -> {
                val educator = _uiState.value.currentUser as? Educator
                if (educator != null) {
                    loadEducatorContent(educator._id)
                } else {
                    loadInitialData()
                }
            }
        }
    }

    /**
     * Get quick stats for dashboard
     */
    fun getQuickStats(): Map<String, Int> {
        return mapOf(
            "totalKlyps" to _uiState.value.recentKlyps.size,
            "totalClasses" to _uiState.value.myClasses.size,
            "upcomingAssignments" to _uiState.value.upcomingAssignments.size,
            "featuredItems" to ((_uiState.value.featuredContent["recentKlyps"] as? List<*>)?.size ?: 0)
        )
    }

    /**
     * Demo method to switch between different user roles for testing
     */
    fun switchToDemoUser(role: UserRole) {
        when (role) {
            UserRole.STUDENT -> {
                // Load first demo student
                val demoStudents = DummyDataGenerator.generateSampleStudents()
                if (demoStudents.isNotEmpty()) {
                    loadStudentContent(demoStudents.first()._id)
                }
            }
            UserRole.EDUCATOR -> {
                // Load first demo educator
                val demoEducators = DummyDataGenerator.generateSampleEducators()
                if (demoEducators.isNotEmpty()) {
                    loadEducatorContent(demoEducators.first()._id)
                }
            }
        }
    }
}
