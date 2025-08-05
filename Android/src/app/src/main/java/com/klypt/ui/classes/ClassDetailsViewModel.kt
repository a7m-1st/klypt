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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
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
import javax.inject.Inject

data class ClassDetailsUiState(
    val isLoading: Boolean = true,
    val classDocument: ClassDocument? = null,
    val klyps: List<Klyp> = emptyList(),
    val errorMessage: String? = null
)

@HiltViewModel
class ClassDetailsViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository,
    private val userContextProvider: UserContextProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClassDetailsUiState())
    val uiState: StateFlow<ClassDetailsUiState> = _uiState.asStateFlow()

    fun initializeWithClass(classDocument: ClassDocument) {
        Log.d("ClassDetailsViewModel", "Initializing with class document: $classDocument")
        Log.d("ClassDetailsViewModel", "Class code: '${classDocument.classCode}' (length: ${classDocument.classCode.length})")
        _uiState.value = _uiState.value.copy(
            classDocument = classDocument,
            isLoading = true,
            errorMessage = null
        )
        loadKlypsForClass(classDocument.classCode)
    }

    fun initializeWithClassId(classId: String) {
        viewModelScope.launch {
            try {
                Log.d("ClassDetailsViewModel", "Initializing with classId: '$classId'")
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                // First try to get by ID
                val classData = classRepository.get(classId)
                Log.d("ClassDetailsViewModel", "Got class data by ID: $classData")
                var classDocument = DatabaseUtils.mapToClassDocument(classData)
                Log.d("ClassDetailsViewModel", "Mapped to class document: $classDocument")
                
                // If not found by ID, try to get by class code (in case classId is actually a class code)
                if (classDocument == null) {
                    Log.d("ClassDetailsViewModel", "Class document is null, trying by class code")
                    val classDataByCode = classRepository.getClassByCode(classId)
                    Log.d("ClassDetailsViewModel", "Got class data by code: $classDataByCode")
                    if (classDataByCode != null) {
                        classDocument = DatabaseUtils.mapToClassDocument(classDataByCode)
                        Log.d("ClassDetailsViewModel", "Mapped class data by code to document: $classDocument")
                    }
                }
                
                if (classDocument != null) {
                    Log.d("ClassDetailsViewModel", "Found class document with code: '${classDocument.classCode}'")
                    _uiState.value = _uiState.value.copy(classDocument = classDocument)
                    loadKlypsForClass(classDocument.classCode)
                } else {
                    Log.e("ClassDetailsViewModel", "Class document is still null after all attempts")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Class not found"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load class: ${e.message}"
                )
            }
        }
    }

    private fun loadKlypsForClass(classCode: String) {
        viewModelScope.launch {
            try {
                Log.d("ClassDetailsViewModel", "Loading klyps for class: '$classCode' (length: ${classCode.length})")
                if (classCode.isEmpty()) {
                    Log.e("ClassDetailsViewModel", "ERROR: classCode is empty!")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = "Class code is empty"
                    )
                    return@launch
                }
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val klypDataList = klypRepository.getKlypsByClassCode(classCode)
                val klyps = klypDataList.mapNotNull { klypData ->
                    try {
                        // Convert questions from Map<String, Any> to Question objects
                        val questionsData = klypData["questions"] as? List<*> ?: emptyList<Any>()
                        val questions = questionsData.mapNotNull { questionMap ->
                            if (questionMap is Map<*, *>) {
                                try {
                                    Question(
                                        questionText = questionMap["questionText"] as? String ?: "",
                                        options = (questionMap["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                                        correctAnswer = (questionMap["correctAnswer"] as? String)?.firstOrNull() ?: 'A'
                                    )
                                } catch (e: Exception) {
                                    null
                                }
                            } else null
                        }
                        
                        Klyp(
                            _id = klypData["_id"] as? String ?: "",
                            type = klypData["type"] as? String ?: "klyp",
                            classCode = klypData["classCode"] as? String ?: classCode,
                            title = klypData["title"] as? String ?: "",
                            mainBody = klypData["mainBody"] as? String ?: "",
                            questions = questions,
                            createdAt = klypData["createdAt"] as? String ?: ""
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    klyps = klyps
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load klyps: ${e.message}"
                )
            }
        }
    }

    fun addKlyp(title: String, content: String, classCode: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val klypId = "klyp_${UUID.randomUUID()}"
                
                val klypData = mapOf(
                    "_id" to klypId,
                    "type" to "klyp",
                    "classCode" to classCode,
                    "title" to title,
                    "mainBody" to content,
                    "questions" to emptyList<Map<String, Any>>(),
                    "createdAt" to currentTime
                )
                
                val success = klypRepository.save(klypData)
                
                if (success) {
                    // Refresh the klyps list
                    loadKlypsForClass(classCode)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to add klyp"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to add klyp: ${e.message}"
                )
            }
        }
    }

    fun deleteKlyp(klyp: Klyp) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(errorMessage = null)
                
                // Delete from database
                val documentId = "klyp::${klyp._id}"
                val success = klypRepository.delete(documentId)
                
                if (success) {
                    // Refresh the klyps list
                    loadKlypsForClass(klyp.classCode)
                } else {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Failed to delete klyp"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete klyp: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        _uiState.value.classDocument?.let { classDoc ->
            loadKlypsForClass(classDoc.classCode)
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
