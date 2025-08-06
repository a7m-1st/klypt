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

package com.klypt.ui.classcodedisplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.klypt.data.models.ClassDocument
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.services.UserContextProvider
import com.klypt.data.UserRole
import com.klypt.data.utils.ClassCodeGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ClassCodeDisplayViewModel @Inject constructor(
    private val classRepository: ClassDocumentRepository,
    private val educatorRepository: EducatorRepository,
    private val studentRepository: StudentRepository,
    private val userContextProvider: UserContextProvider
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ClassCodeDisplayUiState())
    val uiState = _uiState.asStateFlow()
    
    fun initializeWithClassData(classCode: String, className: String) {
        _uiState.value = _uiState.value.copy(
            classCode = classCode,
            className = className,
            isSuccess = true
        )
    }
    
    fun updateClassName(newClassName: String) {
        _uiState.value = _uiState.value.copy(className = newClassName)
    }
    
    fun saveClass(
        educatorId: String,
        onSuccess: (ClassDocument) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
                
                val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
                val currentUserId = userContextProvider.getCurrentUserId()
                val currentUserRole = userContextProvider.getCurrentUserRole()
                
                // Generate a unique class ID
                val classId = "class_${java.util.UUID.randomUUID().toString().replace("-", "").take(8)}"
                
                val classDocument = ClassDocument(
                    _id = classId,
                    classCode = _uiState.value.classCode,
                    classTitle = _uiState.value.className.ifBlank { "Untitled Class" }.toString(),
                    updatedAt = currentTime,
                    lastSyncedAt = currentTime,
                    educatorId = educatorId,
                    studentIds = if (currentUserId != null) listOf(currentUserId) else emptyList()
                )
                
                // Save the class to database
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
                
                val classSaved = classRepository.save(classData)
                
                if (classSaved) {
                    // Update educator's class list
                    val educatorData = educatorRepository.get(educatorId)
                    val currentClassIds = (educatorData["classIds"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    
                    if (!currentClassIds.contains(classDocument._id)) {
                        currentClassIds.add(classDocument._id)
                        
                        val updatedEducatorData = educatorData.toMutableMap()
                        updatedEducatorData["classIds"] = currentClassIds
                        educatorRepository.save(updatedEducatorData)
                    }
                    
                    // Enroll the class creator in the class (both as student and educator depending on role)
                    if (currentUserId != null) {
                        // Always enroll creator as student
                        val studentData = studentRepository.get(currentUserId)
                        val enrolledClassIds = (studentData["enrolledClassIds"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                        
                        if (!enrolledClassIds.contains(classDocument._id)) {
                            enrolledClassIds.add(classDocument._id)
                            
                            val updatedStudentData = studentData.toMutableMap()
                            updatedStudentData["enrolledClassIds"] = enrolledClassIds
                            updatedStudentData["updatedAt"] = currentTime
                            studentRepository.save(updatedStudentData)
                            
                            android.util.Log.d("ClassCodeDisplayVM", "Creator $currentUserId enrolled as student in class ${classDocument._id}")
                        }
                        
                        // If creator is an educator, also ensure they're added to the educator's class list
                        if (currentUserRole == UserRole.EDUCATOR) {
                            try {
                                val educatorData = educatorRepository.get(currentUserId)
                                val currentEducatorClassIds = (educatorData["classIds"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                                
                                if (!currentEducatorClassIds.contains(classDocument._id)) {
                                    currentEducatorClassIds.add(classDocument._id)
                                    
                                    val updatedEducatorData = educatorData.toMutableMap()
                                    updatedEducatorData["classIds"] = currentEducatorClassIds
                                    updatedEducatorData["updatedAt"] = currentTime
                                    educatorRepository.save(updatedEducatorData)
                                    
                                    android.util.Log.d("ClassCodeDisplayVM", "Creator $currentUserId enrolled as educator in class ${classDocument._id}")
                                }
                            } catch (e: Exception) {
                                android.util.Log.w("ClassCodeDisplayVM", "Could not enroll creator as educator: ${e.message}")
                            }
                        }
                        
                        android.util.Log.d("ClassCodeDisplayVM", "Creator $currentUserId enrolled as student in class ${classDocument._id}")
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSuccess = true
                    )
                    
                    onSuccess(classDocument)
                } else {
                    throw Exception("Failed to save class to database")
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to save class"
                )
                onError(_uiState.value.errorMessage ?: "Unknown error")
            }
        }
    }
    
    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
