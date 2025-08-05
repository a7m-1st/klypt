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

package com.klypt.ui.classes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.ClassDocument
import com.klypt.data.repository.EducationalContentRepository
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.services.UserContextProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ViewAllClassesUiState(
    val isLoading: Boolean = true,
    val classes: List<ClassDocument> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ViewAllClassesViewModel @Inject constructor(
    private val educationalContentRepository: EducationalContentRepository,
    private val classRepository: ClassDocumentRepository,
    private val userContextProvider: UserContextProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ViewAllClassesUiState())
    val uiState: StateFlow<ViewAllClassesUiState> = _uiState.asStateFlow()

    init {
        loadAllClasses()
    }

    private fun loadAllClasses() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

                //TODO(): See if this causes issues if user is educator / student
                educationalContentRepository.getClasses()
                    .catch { error ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = "Failed to load classes: ${error.message}"
                        )
                    }
                    .collect { classes ->
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            classes = classes
                        )
                    }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load classes: ${e.message}"
                )
            }
        }
    }

    fun deleteClass(classDocument: ClassDocument) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // Convert ClassDocument to Map for repository
                val classData = mapOf(
                    "_id" to classDocument._id,
                    "type" to classDocument.type,
                    "classCode" to classDocument.classCode,
                    "classTitle" to classDocument.classTitle,
                    "updatedAt" to classDocument.updatedAt,
                    "lastSyncedAt" to classDocument.lastSyncedAt,
                    "educatorId" to classDocument.educatorId,
                    "studentIds" to classDocument.studentIds
                )
                
                // Delete from database
                val documentId = "class::${classDocument._id}"
                val success = classRepository.delete(documentId)
                
                if (success) {
                    // Refresh the list
                    loadAllClasses()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Failed to delete class"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to delete class: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        loadAllClasses()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
