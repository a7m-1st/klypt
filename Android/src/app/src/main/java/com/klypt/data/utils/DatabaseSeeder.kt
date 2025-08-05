package com.klypt.data.utils

import com.klypt.data.DummyDataGenerator
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.KlypRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class to seed the database with initial data if it's empty
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val studentRepository: StudentRepository,
    private val educatorRepository: EducatorRepository,
    private val classRepository: ClassDocumentRepository,
    private val klypRepository: KlypRepository
) {

    suspend fun seedDatabaseIfEmpty(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("DatabaseSeeder", "Checking if database needs seeding...")
                
                // Check if database has any data
                val studentCount: Int = studentRepository.count()
                val educatorCount: Int = educatorRepository.count()
                val classCount: Int = classRepository.count()
                val klypCount: Int= klypRepository.count()
                
                android.util.Log.d("DatabaseSeeder", "Current counts - Students: $studentCount, Educators: $educatorCount, Classes: $classCount, Klyps: $klypCount")
                
                if (studentCount == 0 && educatorCount == 0 && classCount == 0 && klypCount == 0) {
                    android.util.Log.d("DatabaseSeeder", "Database is empty, seeding with dummy data...")
                    
                    // Generate and save dummy data
                    val students = DummyDataGenerator.generateSampleStudents()
                    val educators = DummyDataGenerator.generateSampleEducators()
                    val classes = DummyDataGenerator.generateSampleClasses()
                    val klyps = DummyDataGenerator.generateSampleKlyps()
                    
                    // Save students
                    students.forEach { student ->
                        val studentData = DatabaseUtils.studentToMap(student)
                        studentRepository.save(studentData)
                        android.util.Log.d("DatabaseSeeder", "Saved student: ${student._id}")
                    }
                    
                    // Save educators
                    educators.forEach { educator ->
                        val educatorData = DatabaseUtils.educatorToMap(educator)
                        educatorRepository.save(educatorData)
                        android.util.Log.d("DatabaseSeeder", "Saved educator: ${educator._id}")
                    }
                    
                    // Save classes
                    classes.forEach { classDoc ->
                        val classData = DatabaseUtils.classDocumentToMap(classDoc)
                        classRepository.save(classData)
                        android.util.Log.d("DatabaseSeeder", "Saved class: ${classDoc._id}")
                    }
                    
                    // Save klyps
                    klyps.forEach { klyp ->
                        val klypData = DatabaseUtils.klypToMap(klyp)
                        klypRepository.save(klypData)
                        android.util.Log.d("DatabaseSeeder", "Saved klyp: ${klyp._id}")
                    }
                    
                    android.util.Log.d("DatabaseSeeder", "Database seeding completed successfully!")
                    return@withContext true
                } else {
                    android.util.Log.d("DatabaseSeeder", "Database already contains data, skipping seeding")
                    return@withContext false
                }
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to seed database: ${e.message}", e)
                return@withContext false
            }
        }
    }

    suspend fun forceSeedDatabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("DatabaseSeeder", "Force seeding database...")
                
                // Generate and save dummy data
                val students = DummyDataGenerator.generateSampleStudents()
                val educators = DummyDataGenerator.generateSampleEducators()
                val classes = DummyDataGenerator.generateSampleClasses()
                val klyps = DummyDataGenerator.generateSampleKlyps()
                
                // Save students
                students.forEach { student ->
                    val studentData = DatabaseUtils.studentToMap(student)
                    studentRepository.save(studentData)
                }
                
                // Save educators
                educators.forEach { educator ->
                    val educatorData = DatabaseUtils.educatorToMap(educator)
                    educatorRepository.save(educatorData)
                }
                
                // Save classes
                classes.forEach { classDoc ->
                    val classData = DatabaseUtils.classDocumentToMap(classDoc)
                    classRepository.save(classData)
                }
                
                // Save klyps
                klyps.forEach { klyp ->
                    val klypData = DatabaseUtils.klypToMap(klyp)
                    klypRepository.save(klypData)
                }
                
                android.util.Log.d("DatabaseSeeder", "Force database seeding completed successfully!")
                return@withContext true
            } catch (e: Exception) {
                android.util.Log.e("DatabaseSeeder", "Failed to force seed database: ${e.message}", e)
                return@withContext false
            }
        }
    }
}
