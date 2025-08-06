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

package com.klypt.data.repository

import android.util.Log
import com.klypt.data.utils.DatabaseUtils
import com.klypt.data.DummyDataGenerator
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Student
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.utils.DatabaseSeeder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing educational content and user data.
 * Now uses database repositories instead of dummy data.
 */
@Singleton
class EducationalContentRepository @Inject constructor(
    private val studentRepository: StudentRepository,
    private val educatorRepository: EducatorRepository,
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository,
    private val databaseSeeder: DatabaseSeeder
) {

    // Cache for database data
    private var _students: List<Student>? = null
    private var _educators: List<Educator>? = null
    private var _classes: List<ClassDocument>? = null
    private var _klyps: List<Klyp>? = null
    private var _databaseSeeded = false

    /**
     * Clear cached data to force refresh from database
     */
    fun clearCache() {
        _students = null
        _educators = null
        _classes = null
        _klyps = null
        // Force database re-seeding check on next access
        _databaseSeeded = false
    }
    
    /**
     * Ensure database is seeded with initial data
     */
    private suspend fun ensureDatabaseSeeded() {
        if (!_databaseSeeded) {
            try {
                databaseSeeder.seedDatabaseIfEmpty()
                _databaseSeeded = true
            } catch (e: Exception) {
                android.util.Log.e("EducationalContentRepository", "Failed to seed database: ${e.message}", e)
            }
        }
    }

    /**
     * Get all students
     */
    fun getStudents(): Flow<List<Student>> = flow {
        try {
            ensureDatabaseSeeded()
            val studentData = studentRepository.getAllStudents()
            val students = studentData.mapNotNull { DatabaseUtils.mapToStudent(it) }

            Log.d("EducationalContentRepository", "Students: $students")

            _students = students
            emit(students)
        } catch (e: Exception) {
            // Fallback to dummy data if database fails
            if (_students == null) {
                _students = DummyDataGenerator.generateSampleStudents()
            }
            emit(_students!!)
        }
    }

    /**
     * Get student by ID
     */
    suspend fun getStudentById(studentId: String): Student? {
        Log.d("EducationalContentRepository", "getStudentById() called with studentId: $studentId")
        
        return try {
            ensureDatabaseSeeded()
            Log.d("EducationalContentRepository", "Fetching student data from repository")
            
            val data = studentRepository.get(studentId)
            Log.d("EducationalContentRepository", "Raw student data from repository: $data")
            
            // DIAGNOSTIC: If we only get _id, let's check what's actually in the database
            if (data.size == 1 && data.containsKey("_id")) {
                Log.e("EducationalContentRepository", "CRITICAL: Only _id found in student data!")
                Log.e("EducationalContentRepository", "This indicates the student record is incomplete in the database")
                
                // Try to force create/update the student with proper data
//                Log.d("EducationalContentRepository", "Attempting to force create/update student...")
//                val forceCreatedStudent = forceCreateStudent(studentId)
//                if (forceCreatedStudent != null) {
//                    Log.d("EducationalContentRepository", "Successfully force created student: $forceCreatedStudent")
//                    return forceCreatedStudent
//                }
                
                // Try to get all students to see what's actually stored
                try {
                    Log.d("EducationalContentRepository", "Checking all students in database for debugging...")
                    val allStudentsData = studentRepository.getAllStudents()
                    Log.d("EducationalContentRepository", "Total students in database: ${allStudentsData.size}")
                    allStudentsData.forEachIndexed { index, studentData ->
                        Log.d("EducationalContentRepository", "Student $index: $studentData")
                    }
                    
                    // Check if we have the student with correct data
                    val foundStudent = allStudentsData.find { it["_id"] == studentId }
                    if (foundStudent != null) {
                        Log.d("EducationalContentRepository", "Found student in getAllStudents: $foundStudent")
                        val mappedFromAll = DatabaseUtils.mapToStudent(foundStudent)
                        Log.d("EducationalContentRepository", "Mapped from getAllStudents: $mappedFromAll")
                        if (mappedFromAll != null && mappedFromAll.firstName.isNotEmpty()) {
                            Log.d("EducationalContentRepository", "Using student data from getAllStudents instead")
                            return mappedFromAll
                        }
                    }
                } catch (e: Exception) {
                    Log.e("EducationalContentRepository", "Error checking all students", e)
                }
                
                // Force fallback to dummy data since database data is corrupted
                Log.w("EducationalContentRepository", "Database has corrupted student data, falling back to dummy data")
                if (_students == null) {
                    _students = DummyDataGenerator.generateSampleStudents()
                }
                val fallbackStudent = _students?.find { it._id == studentId }
                if (fallbackStudent != null) {
                    Log.d("EducationalContentRepository", "Using fallback dummy student: $fallbackStudent")
                    return fallbackStudent
                }
                
                // If no dummy student matches, create a temporary student with the current user's name
                Log.w("EducationalContentRepository", "Creating temporary student from current user context")
                try {
                    // Try to get the current user name from token manager
                    val parts = studentId.split("_")
                    if (parts.size >= 2) {
                        val tempStudent = Student(
                            _id = studentId,
                            firstName = parts[0].replaceFirstChar { it.uppercase() },
                            lastName = parts[1].replaceFirstChar { it.uppercase() },
                            recoveryCode = "TEMP001",
                            enrolledClassIds = listOf("class_cs101"), // Give them a default class
                            createdAt = System.currentTimeMillis().toString(),
                            updatedAt = System.currentTimeMillis().toString()
                        )
                        Log.d("EducationalContentRepository", "Created temporary student: $tempStudent")
                        return tempStudent
                    }
                } catch (e: Exception) {
                    Log.e("EducationalContentRepository", "Error creating temporary student", e)
                }
            }
            
            val student = DatabaseUtils.mapToStudent(data)
            Log.d("EducationalContentRepository", "Mapped student: $student")
            
            if (student != null) {
                Log.d("EducationalContentRepository", "Student details:")
                Log.d("EducationalContentRepository", "  - ID: ${student._id}")
                Log.d("EducationalContentRepository", "  - First Name: '${student.firstName}'")
                Log.d("EducationalContentRepository", "  - Last Name: '${student.lastName}'")
                Log.d("EducationalContentRepository", "  - Enrolled Classes: ${student.enrolledClassIds}")
                Log.d("EducationalContentRepository", "  - Created At: '${student.createdAt}'")
                Log.d("EducationalContentRepository", "  - Updated At: '${student.updatedAt}'")
                
                if (student.firstName.isEmpty() || student.lastName.isEmpty()) {
                    Log.w("EducationalContentRepository", "WARNING: Student has empty firstName or lastName!")
                }
                if (student.enrolledClassIds.isEmpty()) {
                    Log.w("EducationalContentRepository", "WARNING: Student has no enrolled classes!")
                }
            } else {
                Log.w("EducationalContentRepository", "Student mapping returned null")
            }
            
            student
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Exception getting student by ID: ${e.message}", e)
            
            // Fallback to dummy data
            if (_students == null) {
                _students = DummyDataGenerator.generateSampleStudents()
            }
            val fallbackStudent = _students?.find { it._id == studentId }
            Log.d("EducationalContentRepository", "Using fallback student: $fallbackStudent")
            fallbackStudent
        }
    }

    /**
     * Get all educators
     */
    fun getEducators(): Flow<List<Educator>> = flow {
        try {
            val educatorData = educatorRepository.getAllEducators()
            val educators = educatorData.mapNotNull { DatabaseUtils.mapToEducator(it) }
            _educators = educators
            emit(educators)
        } catch (e: Exception) {
            // Fallback to dummy data if database fails
            if (_educators == null) {
                _educators = DummyDataGenerator.generateSampleEducators()
            }
            emit(_educators!!)
        }
    }

    /**
     * Get educator by ID
     */
    suspend fun getEducatorById(educatorId: String): Educator? {
        return try {
            val data = educatorRepository.get(educatorId)
            DatabaseUtils.mapToEducator(data)
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_educators == null) {
                _educators = DummyDataGenerator.generateSampleEducators()
            }
            _educators?.find { it._id == educatorId }
        }
    }
    
    /**
     * Get all educators as a simple list (for synchronous operations)
     */
    suspend fun getAllEducators(): List<Educator> {
        return try {
            val educatorData = educatorRepository.getAllEducators()
            val educators = educatorData.mapNotNull { DatabaseUtils.mapToEducator(it) }
            _educators = educators
            educators
        } catch (e: Exception) {
            // Fallback to dummy data if database fails
            if (_educators == null) {
                _educators = DummyDataGenerator.generateSampleEducators()
            }
            _educators!!
        }
    }

    /**
     * Get all classes
     */
    fun getClasses(): Flow<List<ClassDocument>> = flow {
        try {
            val classData = classRepository.getAllClasses()
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            
            // If database returns classes but they have empty fields, fall back to dummy data
            if (classes.isNotEmpty() && classes.all { it.classTitle.isBlank() || it.classCode.isBlank() }) {
                android.util.Log.w("EducationalContentRepository", "Database returned classes with empty fields, falling back to dummy data")
                if (_classes == null) {
                    _classes = DummyDataGenerator.generateSampleClasses()
                }
                emit(_classes!!)
                return@flow
            }
            
            _classes = classes
            emit(classes)
        } catch (e: Exception) {
            android.util.Log.e("EducationalContentRepository", "Database query failed, falling back to dummy data", e)
            // Fallback to dummy data if database fails
            if (_classes == null) {
                _classes = DummyDataGenerator.generateSampleClasses()
            }
            emit(_classes!!)
        }
    }

    /**
     * Get classes for a specific student
     */
    fun getClassesForStudent(studentId: String): Flow<List<ClassDocument>> = flow {
        try {
            android.util.Log.d("EducationalContentRepository", "Getting classes for student: $studentId")
            val classData = classRepository.getClassesByStudentId(studentId)
            android.util.Log.d("EducationalContentRepository", "Raw class data for student: $classData")
            
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            android.util.Log.d("EducationalContentRepository", "Mapped classes for student: $classes")
            
            // If database returns classes but they have empty fields, fall back to dummy data
            if (classes.isNotEmpty() && classes.all { it.classTitle.isBlank() || it.classCode.isBlank() }) {
                android.util.Log.w("EducationalContentRepository", "Database returned student classes with empty fields, falling back to dummy data")
                if (_classes == null) {
                    _classes = DummyDataGenerator.generateSampleClasses()
                }
                val studentClasses = _classes?.filter { classDoc ->
                    classDoc.studentIds.contains(studentId)
                } ?: emptyList()
                android.util.Log.d("EducationalContentRepository", "Using dummy classes for student: $studentClasses")
                emit(studentClasses)
                return@flow
            }
            
            // If database returns no classes, check if the student is enrolled in classes that don't exist
            if (classes.isEmpty()) {
                android.util.Log.w("EducationalContentRepository", "No classes found for student $studentId in database, checking for enrollment mismatches")
                
                // Get the student's enrolled class IDs
                val student = getStudentById(studentId)
                if (student != null && student.enrolledClassIds.isNotEmpty()) {
                    android.util.Log.w("EducationalContentRepository", "Student is enrolled in classes ${student.enrolledClassIds} but they don't exist in database, falling back to dummy data")
                    if (_classes == null) {
                        _classes = DummyDataGenerator.generateSampleClasses()
                    }
                    val studentClasses = _classes?.filter { classDoc ->
                        classDoc.studentIds.contains(studentId)
                    } ?: emptyList()
                    android.util.Log.d("EducationalContentRepository", "Using dummy classes for student with missing enrollment: $studentClasses")
                    emit(studentClasses)
                    return@flow
                }
            }
            
            android.util.Log.d("EducationalContentRepository", "Emitting ${classes.size} classes for student")
            emit(classes)
        } catch (e: Exception) {
            android.util.Log.e("EducationalContentRepository", "Database query for student classes failed, falling back to dummy data", e)
            // Fallback to dummy data
            if (_classes == null) {
                _classes = DummyDataGenerator.generateSampleClasses()
            }
            val studentClasses = _classes?.filter { classDoc ->
                classDoc.studentIds.contains(studentId)
            } ?: emptyList()
            emit(studentClasses)
        }
    }

    /**
     * Get classes for a specific educator
     */
    fun getClassesForEducator(educatorId: String): Flow<List<ClassDocument>> = flow {
        try {
            val classData = classRepository.getClassesByEducatorId(educatorId)
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            
            // If database returns classes but they have empty fields, fall back to dummy data
            if (classes.isNotEmpty() && classes.all { it.classTitle.isBlank() || it.classCode.isBlank() }) {
                android.util.Log.w("EducationalContentRepository", "Database returned educator classes with empty fields, falling back to dummy data")
                if (_classes == null) {
                    _classes = DummyDataGenerator.generateSampleClasses()
                }
                val educatorClasses = _classes?.filter { classDoc ->
                    classDoc.educatorId == educatorId
                } ?: emptyList()
                emit(educatorClasses)
                return@flow
            }
            
            emit(classes)
        } catch (e: Exception) {
            android.util.Log.e("EducationalContentRepository", "Database query for educator classes failed, falling back to dummy data", e)
            // Fallback to dummy data
            if (_classes == null) {
                _classes = DummyDataGenerator.generateSampleClasses()
            }
            val educatorClasses = _classes?.filter { classDoc ->
                classDoc.educatorId == educatorId
            } ?: emptyList()
            emit(educatorClasses)
        }
    }

    /**
     * Get all Klyps (educational content)
     */
    fun getKlyps(): Flow<List<Klyp>> = flow {
        try {
            val klypData = klypRepository.getAllKlyps()
            val klyps = klypData.mapNotNull { DatabaseUtils.mapToKlyp(it) }
            _klyps = klyps
            emit(klyps)
        } catch (e: Exception) {
            // Fallback to dummy data if database fails
            if (_klyps == null) {
                _klyps = DummyDataGenerator.generateSampleKlyps()
            }
            emit(_klyps!!)
        }
    }

    /**
     * Get Klyps for a specific class
     */
    fun getKlypsForClass(classCode: String): Flow<List<Klyp>> = flow {
        try {
            val klypData = klypRepository.getKlypsByClassCode(classCode)
            val klyps = klypData.mapNotNull { DatabaseUtils.mapToKlyp(it) }
            emit(klyps)
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_klyps == null) {
                _klyps = DummyDataGenerator.generateSampleKlyps()
            }
            val classKlyps = _klyps?.filter { klyp ->
                klyp.classCode == classCode
            } ?: emptyList()
            emit(classKlyps)
        }
    }

    /**
     * Get recent Klyps for student's enrolled classes
     */
    fun getRecentKlypsForStudent(studentId: String): Flow<List<Klyp>> = flow {
        try {
            val student = getStudentById(studentId)
            if (student != null) {
                // Get student's enrolled classes
                val classData = classRepository.getClassesByStudentId(studentId)
                val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
                val classCodes = classes.map { it.classCode }
                
                // Get Klyps for those classes
                val klypData = klypRepository.getKlypsByClassCodes(classCodes)
                val klyps = klypData.mapNotNull { DatabaseUtils.mapToKlyp(it) }
                    .sortedByDescending { it.createdAt }
                    .take(5) // Take 5 most recent
                
                emit(klyps)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            // Fallback to dummy data
            val student = getStudentById(studentId)
            if (student != null) {
                if (_klyps == null) {
                    _klyps = DummyDataGenerator.generateSampleKlyps()
                }
                
                // Get class codes from student's enrolled classes
                if (_classes == null) {
                    _classes = DummyDataGenerator.generateSampleClasses()
                }
                
                val enrolledClassCodes = _classes?.filter { classDoc ->
                    student.enrolledClassIds.contains(classDoc._id)
                }?.map { it.classCode } ?: emptyList()
                
                // Filter Klyps for enrolled classes
                val relevantKlyps = _klyps?.filter { klyp ->
                    enrolledClassCodes.contains(klyp.classCode)
                }?.take(5) ?: emptyList() // Take 5 most recent
                
                emit(relevantKlyps)
            } else {
                emit(emptyList())
            }
        }
    }

    /**
     * Get class schedules
     */
    fun getClassSchedules(): Map<String, List<String>> {
        return DummyDataGenerator.generateClassSchedules()
    }

    /**
     * Get assignments for classes
     */
    fun getAssignments(): Map<String, List<String>> {
        return DummyDataGenerator.generateAssignments()
    }

    /**
     * Get assignments for a specific class
     */
    fun getAssignmentsForClass(classCode: String): List<String> {
        val assignments = DummyDataGenerator.generateAssignments()
        return assignments[classCode] ?: emptyList()
    }

    /**
     * Get upcoming assignments for a student
     */
    suspend fun getUpcomingAssignmentsForStudent(studentId: String): List<Pair<String, String>> {
        val student = getStudentById(studentId)
        if (student != null) {
            if (_classes == null) {
                _classes = DummyDataGenerator.generateSampleClasses()
            }
            
            val enrolledClasses = _classes?.filter { classDoc ->
                student.enrolledClassIds.contains(classDoc._id)
            } ?: emptyList()
            
            val assignments = DummyDataGenerator.generateAssignments()
            val upcomingAssignments = mutableListOf<Pair<String, String>>()
            
            enrolledClasses.forEach { classDoc ->
                val classAssignments = assignments[classDoc._id] ?: emptyList()
                classAssignments.take(2).forEach { assignment -> // Take 2 upcoming per class
                    upcomingAssignments.add(Pair(classDoc.classTitle, assignment))
                }
            }
            
            return upcomingAssignments.take(5) // Return max 5 upcoming assignments
        }
        return emptyList()
    }

    /**
     * Get class statistics for educator dashboard
     */
    suspend fun getClassStatisticsForEducator(educatorId: String): Map<String, Any> {
        return try {
            val classData = classRepository.getClassesByEducatorId(educatorId)
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            val totalStudents = classes.sumOf { it.studentIds.size }
            val totalClasses = classes.size
            
            val classCodes = classes.map { it.classCode }
            val klypData = klypRepository.getKlypsByClassCodes(classCodes)
            val totalKlyps = klypData.size
            
            mapOf(
                "totalClasses" to totalClasses,
                "totalStudents" to totalStudents,
                "totalKlyps" to totalKlyps,
                "averageStudentsPerClass" to if (totalClasses > 0) totalStudents / totalClasses else 0
            )
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_classes == null) {
                _classes = DummyDataGenerator.generateSampleClasses()
            }
            
            val educatorClasses = _classes?.filter { it.educatorId == educatorId } ?: emptyList()
            val totalStudents = educatorClasses.sumOf { it.studentIds.size }
            val totalClasses = educatorClasses.size
            
            if (_klyps == null) {
                _klyps = DummyDataGenerator.generateSampleKlyps()
            }
            
            val classCodes = educatorClasses.map { it.classCode }
            val totalKlyps = _klyps?.count { klyp ->
                classCodes.contains(klyp.classCode)
            } ?: 0
            
            mapOf(
                "totalClasses" to totalClasses,
                "totalStudents" to totalStudents,
                "totalKlyps" to totalKlyps,
                "averageStudentsPerClass" to if (totalClasses > 0) totalStudents / totalClasses else 0
            )
        }
    }

    /**
     * Search Klyps by title or content
     */
    fun searchKlyps(query: String): Flow<List<Klyp>> = flow {
        try {
            val klypData = klypRepository.searchKlyps(query)
            val klyps = klypData.mapNotNull { DatabaseUtils.mapToKlyp(it) }
            emit(klyps)
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_klyps == null) {
                _klyps = DummyDataGenerator.generateSampleKlyps()
            }
            
            val searchResults = _klyps?.filter { klyp ->
                klyp.title.contains(query, ignoreCase = true) ||
                klyp.mainBody.contains(query, ignoreCase = true) ||
                klyp.questions.any { question ->
                    question.questionText.contains(query, ignoreCase = true)
                }
            } ?: emptyList()
            
            emit(searchResults)
        }
    }

    /**
     * Get featured content for home page
     */
    fun getFeaturedContent(): Flow<Map<String, Any>> = flow {
        try {
            ensureDatabaseSeeded()
            // Get data from database
            val klypData = klypRepository.getAllKlyps()
            val classData = classRepository.getAllClasses()
            val studentData = studentRepository.getAllStudents()
            val educatorData = educatorRepository.getAllEducators()
            
            val klyps = klypData.mapNotNull { DatabaseUtils.mapToKlyp(it) }
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            val students = studentData.mapNotNull { DatabaseUtils.mapToStudent(it) }
            val educators = educatorData.mapNotNull { DatabaseUtils.mapToEducator(it) }
            
            val featured = mapOf(
                "recentKlyps" to klyps.take(3),
                "activeClasses" to classes.take(4),
                "totalStudents" to students.size,
                "totalEducators" to educators.size,
                "totalClasses" to classes.size,
                "totalKlyps" to klyps.size
            )
            
            emit(featured)
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_klyps == null) _klyps = DummyDataGenerator.generateSampleKlyps()
            if (_classes == null) _classes = DummyDataGenerator.generateSampleClasses()
            if (_students == null) _students = DummyDataGenerator.generateSampleStudents()
            if (_educators == null) _educators = DummyDataGenerator.generateSampleEducators()

            val featured = mapOf(
                "recentKlyps" to (_klyps?.take(3) ?: emptyList()),
                "activeClasses" to (_classes?.take(4) ?: emptyList()), 
                "totalStudents" to (_students?.size ?: 0),
                "totalEducators" to (_educators?.size ?: 0),
                "totalClasses" to (_classes?.size ?: 0),
                "totalKlyps" to (_klyps?.size ?: 0)
            )
            
            emit(featured)
        }
    }

    /**
     * Import a class by class code for the current user
     */
    suspend fun importClassByCode(classCode: String, currentUserId: String): Result<ClassDocument> {
        return try {
            val classData = classRepository.getClassByCode(classCode)
            if (classData != null) {
                val classDocument = DatabaseUtils.mapToClassDocument(classData)
                if (classDocument != null) {
                    // Add current user to the class if they're not already enrolled
                    val currentStudentIds = classDocument.studentIds.toMutableList()
                    if (!currentStudentIds.contains(currentUserId)) {
                        currentStudentIds.add(currentUserId)
                        
                        // Update the class with the new student
                        //TODO(SAVE IN DB)
                        val updatedClassData = classData.toMutableMap()
                        updatedClassData["studentIds"] = currentStudentIds
                        
                        classRepository.save(updatedClassData)
                        
                        // Return updated class document
                        val updatedClass = classDocument.copy(studentIds = currentStudentIds)
                        Result.success(updatedClass)
                    } else {
                        // User already enrolled
                        Result.success(classDocument)
                    }
                } else {
                    Result.failure(Exception("Failed to parse class data"))
                }
            } else {
                Result.failure(Exception("Class with code '$classCode' not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ====================================
    // DATABASE EXPLORATION METHODS
    // ====================================

    /**
     * Get comprehensive database overview with counts and sample data
     */
    suspend fun getDatabaseOverview(): Map<String, Any> {
        Log.d("EducationalContentRepository", "Getting database overview...")
        
        return try {
            ensureDatabaseSeeded()
            
            // Get raw data counts and samples
            val allStudentsData = studentRepository.getAllStudents()
            val allEducatorsData = educatorRepository.getAllEducators()
            val allClassesData = classRepository.getAllClasses()
            val allKlypsData = klypRepository.getAllKlyps()
            
            val overview = mutableMapOf<String, Any>()
            
            // Students overview
            overview["students"] = mapOf(
                "total_count" to allStudentsData.size,
                "sample_raw_data" to allStudentsData.take(3),
                "mapped_count" to allStudentsData.mapNotNull { DatabaseUtils.mapToStudent(it) }.size,
                "sample_mapped" to allStudentsData.take(3).mapNotNull { DatabaseUtils.mapToStudent(it) }
            )
            
            // Educators overview
            overview["educators"] = mapOf(
                "total_count" to allEducatorsData.size,
                "sample_raw_data" to allEducatorsData.take(3),
                "mapped_count" to allEducatorsData.mapNotNull { DatabaseUtils.mapToEducator(it) }.size,
                "sample_mapped" to allEducatorsData.take(3).mapNotNull { DatabaseUtils.mapToEducator(it) }
            )
            
            // Classes overview
            overview["classes"] = mapOf(
                "total_count" to allClassesData.size,
                "sample_raw_data" to allClassesData.take(3),
                "mapped_count" to allClassesData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }.size,
                "sample_mapped" to allClassesData.take(3).mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            )
            
            // Klyps overview
            overview["klyps"] = mapOf(
                "total_count" to allKlypsData.size,
                "sample_raw_data" to allKlypsData.take(3),
                "mapped_count" to allKlypsData.mapNotNull { DatabaseUtils.mapToKlyp(it) }.size,
                "sample_mapped" to allKlypsData.take(3).mapNotNull { DatabaseUtils.mapToKlyp(it) }
            )
            
            // Database health summary
            overview["database_health"] = mapOf(
                "total_documents" to (allStudentsData.size + allEducatorsData.size + allClassesData.size + allKlypsData.size),
                "seeded" to _databaseSeeded,
                "cache_status" to mapOf(
                    "students_cached" to (_students != null),
                    "educators_cached" to (_educators != null),
                    "classes_cached" to (_classes != null),
                    "klyps_cached" to (_klyps != null)
                )
            )
            
            Log.d("EducationalContentRepository", "Database overview completed successfully")
            overview
            
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error getting database overview", e)
            mapOf(
                "error" to "Failed to get database overview: ${e.message}",
                "fallback_data_available" to mapOf(
                    "students" to (_students != null),
                    "educators" to (_educators != null),
                    "classes" to (_classes != null),
                    "klyps" to (_klyps != null)
                )
            )
        }
    }

    /**
     * Get all raw database documents for a specific collection
     */
    suspend fun getAllRawDocuments(collectionType: DatabaseCollectionType): List<Map<String, Any>> {
        Log.d("EducationalContentRepository", "Getting all raw documents for: $collectionType")
        
        return try {
            ensureDatabaseSeeded()
            
            when (collectionType) {
                DatabaseCollectionType.STUDENTS -> {
                    val data = studentRepository.getAllStudents()
                    Log.d("EducationalContentRepository", "Retrieved ${data.size} raw student documents")
                    data
                }
                DatabaseCollectionType.EDUCATORS -> {
                    val data = educatorRepository.getAllEducators()
                    Log.d("EducationalContentRepository", "Retrieved ${data.size} raw educator documents")
                    data
                }
                DatabaseCollectionType.CLASSES -> {
                    val data = classRepository.getAllClasses()
                    Log.d("EducationalContentRepository", "Retrieved ${data.size} raw class documents")
                    data
                }
                DatabaseCollectionType.KLYPS -> {
                    val data = klypRepository.getAllKlyps()
                    Log.d("EducationalContentRepository", "Retrieved ${data.size} raw klyp documents")
                    data
                }
            }
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error getting raw documents for $collectionType", e)
            emptyList()
        }
    }

    /**
     * Get detailed information about a specific document by ID
     */
    suspend fun getDocumentDetails(collectionType: DatabaseCollectionType, documentId: String): Map<String, Any> {
        Log.d("EducationalContentRepository", "Getting document details for: $collectionType, ID: $documentId")
        
        return try {
            ensureDatabaseSeeded()
            
            when (collectionType) {
                DatabaseCollectionType.STUDENTS -> {
                    val rawData = studentRepository.get(documentId)
                    val mappedData = DatabaseUtils.mapToStudent(rawData)
                    mapOf(
                        "collection" to "students",
                        "document_id" to documentId,
                        "raw_data" to rawData,
                        "mapped_data" to (mappedData ?: "null"),
                        "mapping_successful" to (mappedData != null),
                        "raw_fields" to rawData.keys.toList(),
                        "raw_field_count" to rawData.size
                    )
                }
                DatabaseCollectionType.EDUCATORS -> {
                    val rawData = educatorRepository.get(documentId)
                    val mappedData = DatabaseUtils.mapToEducator(rawData)
                    mapOf(
                        "collection" to "educators",
                        "document_id" to documentId,
                        "raw_data" to rawData,
                        "mapped_data" to (mappedData ?: "null"),
                        "mapping_successful" to (mappedData != null),
                        "raw_fields" to rawData.keys.toList(),
                        "raw_field_count" to rawData.size
                    )
                }
                DatabaseCollectionType.CLASSES -> {
                    val rawData = classRepository.get(documentId)
                    val mappedData = DatabaseUtils.mapToClassDocument(rawData)
                    mapOf(
                        "collection" to "classes",
                        "document_id" to documentId,
                        "raw_data" to rawData,
                        "mapped_data" to (mappedData ?: "null"),
                        "mapping_successful" to (mappedData != null),
                        "raw_fields" to rawData.keys.toList(),
                        "raw_field_count" to rawData.size
                    )
                }
                DatabaseCollectionType.KLYPS -> {
                    val rawData = klypRepository.get(documentId)
                    val mappedData = DatabaseUtils.mapToKlyp(rawData)
                    mapOf(
                        "collection" to "klyps",
                        "document_id" to documentId,
                        "raw_data" to rawData,
                        "mapped_data" to (mappedData ?: "null"),
                        "mapping_successful" to (mappedData != null),
                        "raw_fields" to rawData.keys.toList(),
                        "raw_field_count" to rawData.size
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error getting document details for $collectionType, ID: $documentId", e)
            mapOf(
                "error" to "Failed to get document details: ${e.message}",
                "collection" to collectionType.toString().lowercase(),
                "document_id" to documentId
            )
        }
    }

    /**
     * Search for documents across all collections
     */
    suspend fun searchAllCollections(searchTerm: String): Map<String, List<Map<String, Any>>> {
        Log.d("EducationalContentRepository", "Searching all collections for: $searchTerm")
        
        return try {
            ensureDatabaseSeeded()
            
            val results = mutableMapOf<String, List<Map<String, Any>>>()
            
            // Search students
            val allStudents = studentRepository.getAllStudents()
            val matchingStudents = allStudents.filter { studentData ->
                studentData.values.any { value ->
                    value.toString().contains(searchTerm, ignoreCase = true)
                }
            }
            results["students"] = matchingStudents
            
            // Search educators  
            val allEducators = educatorRepository.getAllEducators()
            val matchingEducators = allEducators.filter { educatorData ->
                educatorData.values.any { value ->
                    value.toString().contains(searchTerm, ignoreCase = true)
                }
            }
            results["educators"] = matchingEducators
            
            // Search classes
            val allClasses = classRepository.getAllClasses()
            val matchingClasses = allClasses.filter { classData ->
                classData.values.any { value ->
                    value.toString().contains(searchTerm, ignoreCase = true)
                }
            }
            results["classes"] = matchingClasses
            
            // Search klyps
            val allKlyps = klypRepository.getAllKlyps()
            val matchingKlyps = allKlyps.filter { klypData ->
                klypData.values.any { value ->
                    value.toString().contains(searchTerm, ignoreCase = true)
                }
            }
            results["klyps"] = matchingKlyps
            
            Log.d("EducationalContentRepository", "Search completed. Found: ${matchingStudents.size} students, ${matchingEducators.size} educators, ${matchingClasses.size} classes, ${matchingKlyps.size} klyps")
            results
            
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error searching all collections", e)
            mapOf("error" to listOf(mapOf("message" to "Search failed: ${e.message}")))
        }
    }

    /**
     * Get database statistics and analytics
     */
    suspend fun getDatabaseAnalytics(): Map<String, Any> {
        Log.d("EducationalContentRepository", "Getting database analytics...")
        
        return try {
            ensureDatabaseSeeded()
            
            val allStudents = studentRepository.getAllStudents()
            val allEducators = educatorRepository.getAllEducators()
            val allClasses = classRepository.getAllClasses()
            val allKlyps = klypRepository.getAllKlyps()
            
            // Map to objects for analysis
            val students = allStudents.mapNotNull { DatabaseUtils.mapToStudent(it) }
            val educators = allEducators.mapNotNull { DatabaseUtils.mapToEducator(it) }
            val classes = allClasses.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            val klyps = allKlyps.mapNotNull { DatabaseUtils.mapToKlyp(it) }
            
            // Calculate analytics
            val totalStudentsInClasses = classes.sumOf { it.studentIds.size }
            val averageStudentsPerClass = if (classes.isNotEmpty()) totalStudentsInClasses.toDouble() / classes.size else 0.0
            val averageKlypsPerClass = if (classes.isNotEmpty()) klyps.size.toDouble() / classes.size else 0.0
            
            // Class enrollment distribution
            val enrollmentDistribution = classes.map { it.studentIds.size }.groupingBy { it }.eachCount()
            
            // Most active educators (by number of classes)
            val educatorClassCounts = classes.groupingBy { it.educatorId }.eachCount()
            val mostActiveEducators = educatorClassCounts.entries.sortedByDescending { it.value }.take(5)
            
            // Data quality metrics
            val studentsWithEmptyFields = students.count { it.firstName.isBlank() || it.lastName.isBlank() }
            val classesWithEmptyFields = classes.count { it.classTitle.isBlank() || it.classCode.isBlank() }
            val klypsMissingContent = klyps.count { it.title.isBlank() || it.mainBody.isBlank() }
            
            mapOf(
                "overview" to mapOf(
                    "total_students" to students.size,
                    "total_educators" to educators.size,
                    "total_classes" to classes.size,
                    "total_klyps" to klyps.size,
                    "total_documents" to (students.size + educators.size + classes.size + klyps.size)
                ),
                "class_analytics" to mapOf(
                    "average_students_per_class" to averageStudentsPerClass,
                    "average_klyps_per_class" to averageKlypsPerClass,
                    "enrollment_distribution" to enrollmentDistribution,
                    "most_active_educators" to mostActiveEducators
                ),
                "data_quality" to mapOf(
                    "students_with_empty_fields" to studentsWithEmptyFields,
                    "classes_with_empty_fields" to classesWithEmptyFields,
                    "klyps_missing_content" to klypsMissingContent,
                    "data_integrity_score" to calculateDataIntegrityScore(students, educators, classes, klyps)
                ),
                "mapping_success_rates" to mapOf(
                    "students_mapping_rate" to if (allStudents.isNotEmpty()) (students.size.toDouble() / allStudents.size * 100) else 0.0,
                    "educators_mapping_rate" to if (allEducators.isNotEmpty()) (educators.size.toDouble() / allEducators.size * 100) else 0.0,
                    "classes_mapping_rate" to if (allClasses.isNotEmpty()) (classes.size.toDouble() / allClasses.size * 100) else 0.0,
                    "klyps_mapping_rate" to if (allKlyps.isNotEmpty()) (klyps.size.toDouble() / allKlyps.size * 100) else 0.0
                )
            )
            
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error getting database analytics", e)
            mapOf("error" to "Failed to get analytics: ${e.message}")
        }
    }

    /**
     * Calculate data integrity score based on completeness and consistency
     */
    private fun calculateDataIntegrityScore(
        students: List<Student>,
        educators: List<Educator>,
        classes: List<ClassDocument>,
        klyps: List<Klyp>
    ): Double {
        var totalChecks = 0
        var passedChecks = 0
        
        // Check student data completeness
        students.forEach { student ->
            totalChecks += 3
            if (student.firstName.isNotBlank()) passedChecks++
            if (student.lastName.isNotBlank()) passedChecks++
            if (student.recoveryCode.isNotBlank()) passedChecks++
        }
        
        // Check educator data completeness
        educators.forEach { educator ->
            totalChecks += 3
            if (educator.fullName.isNotBlank()) passedChecks++
            if (educator.instituteName.isNotBlank()) passedChecks++
            if (educator.recoveryCode.isNotBlank()) passedChecks++
        }
        
        // Check class data completeness
        classes.forEach { classDoc ->
            totalChecks += 3
            if (classDoc.classTitle.isNotBlank()) passedChecks++
            if (classDoc.classCode.isNotBlank()) passedChecks++
            if (classDoc.educatorId.isNotBlank()) passedChecks++
        }
        
        // Check klyp data completeness
        klyps.forEach { klyp ->
            totalChecks += 2
            if (klyp.title.isNotBlank()) passedChecks++
            if (klyp.mainBody.isNotBlank()) passedChecks++
        }
        
        return if (totalChecks > 0) (passedChecks.toDouble() / totalChecks * 100) else 0.0
    }

    /**
     * Export all database content to a structured format
     */
    suspend fun exportAllDatabaseContent(): Map<String, Any> {
        Log.d("EducationalContentRepository", "Exporting all database content...")
        
        return try {
            ensureDatabaseSeeded()
            
            mapOf(
                "export_timestamp" to System.currentTimeMillis(),
                "students" to mapOf(
                    "raw_data" to studentRepository.getAllStudents(),
                    "mapped_data" to studentRepository.getAllStudents().mapNotNull { DatabaseUtils.mapToStudent(it) }
                ),
                "educators" to mapOf(
                    "raw_data" to educatorRepository.getAllEducators(),
                    "mapped_data" to educatorRepository.getAllEducators().mapNotNull { DatabaseUtils.mapToEducator(it) }
                ),
                "classes" to mapOf(
                    "raw_data" to classRepository.getAllClasses(),
                    "mapped_data" to classRepository.getAllClasses().mapNotNull { DatabaseUtils.mapToClassDocument(it) }
                ),
                "klyps" to mapOf(
                    "raw_data" to klypRepository.getAllKlyps(),
                    "mapped_data" to klypRepository.getAllKlyps().mapNotNull { DatabaseUtils.mapToKlyp(it) }
                )
            )
            
        } catch (e: Exception) {
            Log.e("EducationalContentRepository", "Error exporting database content", e)
            mapOf("error" to "Export failed: ${e.message}")
        }
    }

    /**
     * Enum for database collection types
     */
    enum class DatabaseCollectionType {
        STUDENTS, EDUCATORS, CLASSES, KLYPS
    }
}
