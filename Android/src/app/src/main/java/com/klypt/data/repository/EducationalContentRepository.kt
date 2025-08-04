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
import com.klypt.data.DummyDataGenerator
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Student
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.repositories.ClassRepository
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.utils.DatabaseUtils
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
    private val classRepository: ClassRepository,
    private val klypRepository: KlypRepository,
    private val databaseSeeder: com.klypt.data.utils.DatabaseSeeder
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
        return try {
            ensureDatabaseSeeded()
            val data = studentRepository.get(studentId)
            DatabaseUtils.mapToStudent(data)
        } catch (e: Exception) {
            // Fallback to dummy data
            if (_students == null) {
                _students = DummyDataGenerator.generateSampleStudents()
            }
            _students?.find { it._id == studentId }
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
            _classes = classes
            emit(classes)
        } catch (e: Exception) {
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
            val classData = classRepository.getClassesByStudentId(studentId)
            val classes = classData.mapNotNull { DatabaseUtils.mapToClassDocument(it) }
            emit(classes)
        } catch (e: Exception) {
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
            emit(classes)
        } catch (e: Exception) {
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
}
