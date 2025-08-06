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
                
                // Check if class already exists (to avoid duplicate creation)
                val existingClassData = classRepository.getClassByCode(_uiState.value.classCode)
                val classDocument = if (existingClassData != null) {
                    android.util.Log.d("ClassCodeDisplayVM", "Class with code ${_uiState.value.classCode} already exists, using existing class")
                    
                    // Use existing class but update the class title if it was changed
                    val existingId = existingClassData["_id"] as String
                    val updatedTitle = _uiState.value.className.ifBlank { existingClassData["classTitle"] as? String ?: "Untitled Class" }
                    val existingStudentIds = (existingClassData["studentIds"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    
                    // Add current user to studentIds if they're not already in the list (for students creating classes)
                    var needsUpdate = false
                    if (currentUserId != null && !existingStudentIds.contains(currentUserId)) {
                        existingStudentIds.add(currentUserId)
                        needsUpdate = true
                        android.util.Log.d("ClassCodeDisplayVM", "Added current user $currentUserId to existing class studentIds")
                    }
                    
                    // Update class title if changed or if studentIds were updated
                    if (updatedTitle != (existingClassData["classTitle"] as? String) || needsUpdate) {
                        val updatedClassData = existingClassData.toMutableMap()
                        updatedClassData["classTitle"] = updatedTitle
                        updatedClassData["studentIds"] = existingStudentIds
                        updatedClassData["updatedAt"] = currentTime
                        classRepository.save(updatedClassData)
                        android.util.Log.d("ClassCodeDisplayVM", "Updated existing class with new title and/or student enrollment")
                    }
                    
                    ClassDocument(
                        _id = existingId,
                        classCode = existingClassData["classCode"] as String,
                        classTitle = updatedTitle,
                        updatedAt = currentTime,
                        lastSyncedAt = existingClassData["lastSyncedAt"] as? String ?: currentTime,
                        educatorId = existingClassData["educatorId"] as String,
                        studentIds = existingStudentIds
                    )
                } else {
                    android.util.Log.d("ClassCodeDisplayVM", "Creating new class with code ${_uiState.value.classCode}")
                    
                    // Generate a unique class ID for new class
                    val classId = "class_${java.util.UUID.randomUUID().toString().replace("-", "").take(8)}"
                    
                    val newClassDocument = ClassDocument(
                        _id = classId,
                        classCode = _uiState.value.classCode,
                        classTitle = _uiState.value.className.ifBlank { "Untitled Class" }.toString(),
                        updatedAt = currentTime,
                        lastSyncedAt = currentTime,
                        educatorId = educatorId,
                        studentIds = if (currentUserId != null) listOf(currentUserId) else emptyList()
                    )
                    
                    // Save the new class to database
                    val classData = mapOf(
                        "_id" to newClassDocument._id,
                        "type" to newClassDocument.type,
                        "classCode" to newClassDocument.classCode,
                        "classTitle" to newClassDocument.classTitle,
                        "updatedAt" to newClassDocument.updatedAt,
                        "lastSyncedAt" to newClassDocument.lastSyncedAt,
                        "educatorId" to newClassDocument.educatorId,
                        "studentIds" to newClassDocument.studentIds
                    )
                    
                    val classSaved = classRepository.save(classData)
                    if (!classSaved) {
                        throw Exception("Failed to save new class to database")
                    }
                    
                    newClassDocument
                }

                // Now proceed with educator and student enrollment logic
                val classSaved = true // We know the class exists at this point
                
                if (classSaved) {
                    // Update educator's class list
                    val educatorData = educatorRepository.get(educatorId)
                    val currentClassIds = (educatorData["classIds"] as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    
                    if (!currentClassIds.contains(classDocument._id)) {
                        currentClassIds.add(classDocument._id)
                        
                        val updatedEducatorData = educatorData.toMutableMap()
                        updatedEducatorData["classIds"] = currentClassIds
                        educatorRepository.save(updatedEducatorData)
                        android.util.Log.d("ClassCodeDisplayVM", "Added class ${classDocument._id} to educator $educatorId's class list")
                    }
                    
                    // Enroll the class creator in the class (both as student and educator depending on role)
                    if (currentUserId != null) {
                        // Always enroll creator as student
                        var studentData = studentRepository.get(currentUserId)
                        
                        // Check if student exists properly (has firstName and lastName)
                        // If not, create a complete student record
                        if (!studentData.containsKey("firstName") || !studentData.containsKey("lastName") || 
                            studentData["firstName"] == null || studentData["lastName"] == null) {
                            
                            android.util.Log.w("ClassCodeDisplayVM", "Student $currentUserId doesn't exist or is incomplete, creating proper student record")
                            
                            // Extract firstName and lastName from the currentUserId
                            val nameParts = currentUserId.split("_")
                            if (nameParts.size >= 2) {
                                val firstName = nameParts[0]
                                val lastName = nameParts[1]
                                
                                // Create a complete student record
                                studentData = mapOf(
                                    "_id" to currentUserId,
                                    "type" to "student",
                                    "firstName" to firstName,
                                    "lastName" to lastName,
                                    "recoveryCode" to "",
                                    "enrolledClassIds" to emptyList<String>(),
                                    "createdAt" to currentTime,
                                    "updatedAt" to currentTime
                                )
                                
                                android.util.Log.d("ClassCodeDisplayVM", "Created complete student record for $firstName $lastName")
                            } else {
                                android.util.Log.e("ClassCodeDisplayVM", "Cannot extract firstName/lastName from userId: $currentUserId")
                            }
                        }
                        
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
                        if (currentUserRole == UserRole.EDUCATOR && currentUserId != educatorId) {
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
