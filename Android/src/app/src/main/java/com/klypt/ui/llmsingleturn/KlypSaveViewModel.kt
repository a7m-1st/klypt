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

package com.klypt.ui.llmsingleturn

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.ClassDocument
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.services.UserContextProvider
import com.klypt.data.utils.DatabaseUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.UUID
import javax.inject.Inject

data class KlypSaveUiState(
    val isLoading: Boolean = false,
    val availableClasses: List<ClassDocument> = emptyList(),
    val isLoadingClasses: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class KlypSaveViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository,
    private val userContextProvider: UserContextProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(KlypSaveUiState())
    val uiState: StateFlow<KlypSaveUiState> = _uiState.asStateFlow()

    private val tag = "KlypSaveViewModel"

    /**
     * Loads classes where the current user is the educator (author)
     */
    fun loadAvailableClasses() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoadingClasses = true, errorMessage = null)
                
                val currentUserId = userContextProvider.getCurrentUserId()
                Log.d(tag, "Loading classes for educator: $currentUserId")
                
                if (currentUserId.isEmpty()) {
                    Log.e(tag, "Current user ID is empty")
                    _uiState.value = _uiState.value.copy(
                        isLoadingClasses = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                // Get all classes and filter by educator ID
                val allClassesData = classRepository.getAllClasses()
                val allClasses = allClassesData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
                val educatorClasses = allClasses.filter { classDoc ->
                    Log.d(
                        tag,
                        "Found class: ${classDoc.classCode} - ${classDoc.classTitle}"
                    )
                    classDoc.educatorId == currentUserId
                }
                
                Log.d(tag, "Found ${educatorClasses.size} classes authored by current user")
                
                _uiState.value = _uiState.value.copy(
                    isLoadingClasses = false,
                    availableClasses = educatorClasses,
                    errorMessage = null
                )
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to load classes", e)
                _uiState.value = _uiState.value.copy(
                    isLoadingClasses = false,
                    errorMessage = "Failed to load classes: ${e.message}"
                )
            }
        }
    }

    /**
     * Saves a klyp to the specified class
     */
    fun saveKlypToClass(
        title: String,
        content: String,
        classDocument: ClassDocument,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Prevent concurrent calls - if already loading, ignore the request
        if (_uiState.value.isLoading) {
            Log.d(tag, "saveKlypToClass already in progress, ignoring duplicate call")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(tag, "Saving klyp to class: ${classDocument.classCode}")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Validate inputs
                if (title.isBlank()) {
                    onError("Title cannot be empty")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                if (content.isBlank()) {
                    onError("Content cannot be empty")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }

                // Create klyp data
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val klypId = "klyp_${UUID.randomUUID()}"
                
                val klypData = mapOf(
                    "_id" to klypId,
                    "type" to "klyp",
                    "classCode" to classDocument.classCode,
                    "title" to title,
                    "mainBody" to content,
                    "questions" to emptyList<Map<String, Any>>(), // Empty questions initially
                    "createdAt" to currentTime
                )
                
                Log.d(tag, "Creating klyp with ID: $klypId for class: ${classDocument.classCode}")
                
                // Save to repository
                val success = klypRepository.save(klypData)
                
                if (success) {
                    Log.d(tag, "Successfully saved klyp to class: ${classDocument.classCode}")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = "Klyp saved to ${classDocument.classTitle} successfully!"
                    )
                    onSuccess()
                } else {
                    val errorMsg = "Failed to save klyp to database"
                    Log.e(tag, errorMsg)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = errorMsg
                    )
                    onError(errorMsg)
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Exception while saving klyp", e)
                val errorMsg = "Error saving klyp: ${e.message}"
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = errorMsg
                )
                onError(errorMsg)
            }
        }
    }

    /**
     * Creates a new class and saves a summary to it in one operation
     */
    fun createClassAndSaveSummary(
        classCode: String,
        className: String,
        summaryTitle: String,
        summaryContent: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        // Prevent concurrent calls - if already loading, ignore the request
        if (_uiState.value.isLoading) {
            Log.d(tag, "createClassAndSaveSummary already in progress, ignoring duplicate call")
            return
        }
        
        viewModelScope.launch {
            try {
                Log.d(tag, "Creating class and saving summary: $classCode - $className")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Validate inputs
                if (className.isBlank()) {
                    onError("Class name cannot be empty")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                if (summaryTitle.isBlank()) {
                    onError("Summary title cannot be empty")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                if (summaryContent.isBlank()) {
                    onError("Summary content cannot be empty")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                val currentUserId = userContextProvider.getCurrentUserId()
                if (currentUserId.isEmpty()) {
                    onError("User not authenticated")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                // Check if class with this code already exists to prevent duplicates
                val existingClass = classRepository.getClassByCode(classCode)
                if (existingClass != null) {
                    onError("Class with this code already exists")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                // Create the class document
                val classDocument = ClassDocument(
                    _id = classCode,
                    classCode = classCode,
                    classTitle = className,
                    educatorId = currentUserId,
                    studentIds = emptyList(),
                    updatedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                    lastSyncedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                
                // Save the class first
                Log.d(tag, "Saving class document: $classCode")
                val classData = DatabaseUtils.classDocumentToMap(classDocument)
                val classSaveSuccess = classRepository.save(classData)
                
                if (!classSaveSuccess) {
                    onError("Failed to create class")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    return@launch
                }
                
                // Now save the summary as a klyp to this class
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val klypId = "klyp_${UUID.randomUUID()}"
                
                val klypData = mapOf(
                    "_id" to klypId,
                    "type" to "klyp",
                    "classCode" to classCode,
                    "title" to summaryTitle,
                    "mainBody" to summaryContent,
                    "questions" to emptyList<Map<String, Any>>(),
                    "createdAt" to currentTime
                )
                
                val klypSaveSuccess = klypRepository.save(klypData)
                
                if (klypSaveSuccess) {
                    Log.d(tag, "Successfully saved summary to new class: $classCode")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onSuccess()
                } else {
                    Log.e(tag, "Failed to save summary to new class: $classCode")
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    onError("Failed to save summary to class")
                }
                
            } catch (e: Exception) {
                Log.e(tag, "Failed to create class and save summary", e)
                _uiState.value = _uiState.value.copy(isLoading = false)
                onError("Failed to create class: ${e.message}")
            }
        }
    }

    /**
     * Clears any error or success messages
     */
    fun clearMessages() {
        _uiState.value = _uiState.value.copy(
            errorMessage = null,
            successMessage = null
        )
    }
}
