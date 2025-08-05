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

package com.klypt.data.utils

import android.util.Log
import com.klypt.data.repository.EducationalContentRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for exploring and debugging database content
 * Provides convenient methods to investigate database state and content
 */
@Singleton
class DatabaseExplorer @Inject constructor(
    private val educationalContentRepository: EducationalContentRepository
) {

    companion object {
        private const val TAG = "DatabaseExplorer"
    }

    /**
     * Print comprehensive database overview to logs
     */
    suspend fun printDatabaseOverview() {
        Log.d(TAG, "=== DATABASE OVERVIEW ===")
        
        val overview = educationalContentRepository.getDatabaseOverview()
        
        overview.forEach { (key, value) ->
            Log.d(TAG, "$key: $value")
        }
        
        Log.d(TAG, "=== END DATABASE OVERVIEW ===")
    }

    /**
     * Print all documents from a specific collection
     */
    suspend fun printAllDocuments(collectionType: EducationalContentRepository.DatabaseCollectionType) {
        Log.d(TAG, "=== ALL ${collectionType.name} DOCUMENTS ===")
        
        val documents = educationalContentRepository.getAllRawDocuments(collectionType)
        
        documents.forEachIndexed { index, document ->
            Log.d(TAG, "Document ${index + 1}:")
            document.forEach { (field, value) ->
                Log.d(TAG, "  $field: $value")
            }
            Log.d(TAG, "---")
        }
        
        Log.d(TAG, "Total ${collectionType.name} documents: ${documents.size}")
        Log.d(TAG, "=== END ${collectionType.name} DOCUMENTS ===")
    }

    /**
     * Print detailed information about a specific document
     */
    suspend fun printDocumentDetails(
        collectionType: EducationalContentRepository.DatabaseCollectionType,
        documentId: String
    ) {
        Log.d(TAG, "=== DOCUMENT DETAILS: ${collectionType.name} - $documentId ===")
        
        val details = educationalContentRepository.getDocumentDetails(collectionType, documentId)
        
        details.forEach { (key, value) ->
            when (key) {
                "raw_data", "mapped_data" -> {
                    Log.d(TAG, "$key:")
                    if (value is Map<*, *>) {
                        value.forEach { (field, fieldValue) ->
                            Log.d(TAG, "  $field: $fieldValue")
                        }
                    } else {
                        Log.d(TAG, "  $value")
                    }
                }
                else -> Log.d(TAG, "$key: $value")
            }
        }
        
        Log.d(TAG, "=== END DOCUMENT DETAILS ===")
    }

    /**
     * Search and print results from all collections
     */
    suspend fun searchAndPrint(searchTerm: String) {
        Log.d(TAG, "=== SEARCH RESULTS FOR: '$searchTerm' ===")
        
        val results = educationalContentRepository.searchAllCollections(searchTerm)
        
        results.forEach { (collectionName, documents) ->
            Log.d(TAG, "$collectionName (${documents.size} matches):")
            documents.forEachIndexed { index, document ->
                Log.d(TAG, "  Match ${index + 1}:")
                document.forEach { (field, value) ->
                    Log.d(TAG, "    $field: $value")
                }
            }
        }
        
        val totalMatches = results.values.sumOf { it.size }
        Log.d(TAG, "Total matches found: $totalMatches")
        Log.d(TAG, "=== END SEARCH RESULTS ===")
    }

    /**
     * Print database analytics
     */
    suspend fun printDatabaseAnalytics() {
        Log.d(TAG, "=== DATABASE ANALYTICS ===")
        
        val analytics = educationalContentRepository.getDatabaseAnalytics()
        
        fun printNestedMap(map: Map<String, Any>, indent: String = "") {
            map.forEach { (key, value) ->
                when (value) {
                    is Map<*, *> -> {
                        Log.d(TAG, "$indent$key:")
                        @Suppress("UNCHECKED_CAST")
                        printNestedMap(value as Map<String, Any>, "$indent  ")
                    }
                    is List<*> -> {
                        Log.d(TAG, "$indent$key: [${value.size} items]")
                        value.take(5).forEach { item ->
                            Log.d(TAG, "$indent  - $item")
                        }
                        if (value.size > 5) {
                            Log.d(TAG, "$indent  ... and ${value.size - 5} more")
                        }
                    }
                    else -> Log.d(TAG, "$indent$key: $value")
                }
            }
        }
        
        printNestedMap(analytics)
        
        Log.d(TAG, "=== END DATABASE ANALYTICS ===")
    }

    /**
     * Export database content to logs in JSON-like format
     */
    suspend fun exportDatabaseToLogs() {
        Log.d(TAG, "=== DATABASE EXPORT ===")
        
        val exportData = educationalContentRepository.exportAllDatabaseContent()
        
        // Log export metadata
        Log.d(TAG, "Export timestamp: ${exportData["export_timestamp"]}")
        
        // Log each collection
        listOf("students", "educators", "classes", "klyps").forEach { collectionName ->
            val collectionData = exportData[collectionName] as? Map<String, Any>
            if (collectionData != null) {
                Log.d(TAG, "=== $collectionName ===")
                
                val rawData = collectionData["raw_data"] as? List<Map<String, Any>>
                val mappedData = collectionData["mapped_data"] as? List<Any>
                
                Log.d(TAG, "Raw documents: ${rawData?.size ?: 0}")
                Log.d(TAG, "Mapped objects: ${mappedData?.size ?: 0}")
                
                // Sample raw data
                rawData?.take(2)?.forEachIndexed { index, document ->
                    Log.d(TAG, "Raw sample ${index + 1}:")
                    document.forEach { (field, value) ->
                        Log.d(TAG, "  $field: $value")
                    }
                }
            }
        }
        
        Log.d(TAG, "=== END DATABASE EXPORT ===")
    }

    /**
     * Run a comprehensive database health check
     */
    suspend fun runHealthCheck() {
        Log.d(TAG, "=== DATABASE HEALTH CHECK ===")
        
        try {
            // 1. Overview check
            val overview = educationalContentRepository.getDatabaseOverview()
            Log.d(TAG, "✓ Database overview retrieved successfully")
            
            // 2. Collection checks
            val collections = listOf(
                EducationalContentRepository.DatabaseCollectionType.STUDENTS,
                EducationalContentRepository.DatabaseCollectionType.EDUCATORS,
                EducationalContentRepository.DatabaseCollectionType.CLASSES,
                EducationalContentRepository.DatabaseCollectionType.KLYPS
            )
            
            collections.forEach { collection ->
                try {
                    val documents = educationalContentRepository.getAllRawDocuments(collection)
                    Log.d(TAG, "✓ ${collection.name}: ${documents.size} documents")
                } catch (e: Exception) {
                    Log.e(TAG, "✗ ${collection.name}: Error - ${e.message}")
                }
            }
            
            // 3. Analytics check
            val analytics = educationalContentRepository.getDatabaseAnalytics()
            Log.d(TAG, "✓ Database analytics generated successfully")
            
            // 4. Search functionality check
            val searchResults = educationalContentRepository.searchAllCollections("test")
            Log.d(TAG, "✓ Search functionality working")
            
            Log.d(TAG, "=== HEALTH CHECK COMPLETED ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "✗ Health check failed: ${e.message}", e)
        }
    }

    /**
     * Helper method to find students with incomplete data
     */
    suspend fun findIncompleteStudents() {
        Log.d(TAG, "=== INCOMPLETE STUDENTS CHECK ===")
        
        val allStudents = educationalContentRepository.getAllRawDocuments(
            EducationalContentRepository.DatabaseCollectionType.STUDENTS
        )
        
        val incompleteStudents = allStudents.filter { student ->
            val firstName = student["firstName"]?.toString()
            val lastName = student["lastName"]?.toString()
            val recoveryCode = student["recoveryCode"]?.toString()
            
            firstName.isNullOrBlank() || lastName.isNullOrBlank() || recoveryCode.isNullOrBlank()
        }
        
        Log.d(TAG, "Found ${incompleteStudents.size} incomplete students out of ${allStudents.size} total")
        
        incompleteStudents.forEach { student ->
            Log.d(TAG, "Incomplete student: ${student["_id"]} - $student")
        }
        
        Log.d(TAG, "=== END INCOMPLETE STUDENTS CHECK ===")
    }

    /**
     * Helper method to find orphaned documents (references that don't exist)
     */
    suspend fun findOrphanedReferences() {
        Log.d(TAG, "=== ORPHANED REFERENCES CHECK ===")
        
        try {
            val allStudents = educationalContentRepository.getAllRawDocuments(
                EducationalContentRepository.DatabaseCollectionType.STUDENTS
            )
            val allEducators = educationalContentRepository.getAllRawDocuments(
                EducationalContentRepository.DatabaseCollectionType.EDUCATORS
            )
            val allClasses = educationalContentRepository.getAllRawDocuments(
                EducationalContentRepository.DatabaseCollectionType.CLASSES
            )
            
            val studentIds = allStudents.mapNotNull { it["_id"]?.toString() }.toSet()
            val educatorIds = allEducators.mapNotNull { it["_id"]?.toString() }.toSet()
            val classIds = allClasses.mapNotNull { it["_id"]?.toString() }.toSet()
            
            // Check for orphaned educator references in classes
            allClasses.forEach { classDoc ->
                val educatorId = classDoc["educatorId"]?.toString()
                if (educatorId != null && educatorId !in educatorIds) {
                    Log.w(TAG, "Class ${classDoc["_id"]} references non-existent educator: $educatorId")
                }
            }
            
            // Check for orphaned student references in classes
            allClasses.forEach { classDoc ->
                val studentIdsList = classDoc["studentIds"] as? List<*>
                studentIdsList?.forEach { studentId ->
                    val studentIdStr = studentId?.toString()
                    if (studentIdStr != null && studentIdStr !in studentIds) {
                        Log.w(TAG, "Class ${classDoc["_id"]} references non-existent student: $studentIdStr")
                    }
                }
            }
            
            // Check for orphaned class references in student enrollments
            allStudents.forEach { student ->
                val enrolledClassIds = student["enrolledClassIds"] as? List<*>
                enrolledClassIds?.forEach { classId ->
                    val classIdStr = classId?.toString()
                    if (classIdStr != null && classIdStr !in classIds) {
                        Log.w(TAG, "Student ${student["_id"]} enrolled in non-existent class: $classIdStr")
                    }
                }
            }
            
            Log.d(TAG, "=== END ORPHANED REFERENCES CHECK ===")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking orphaned references: ${e.message}", e)
        }
    }
}
