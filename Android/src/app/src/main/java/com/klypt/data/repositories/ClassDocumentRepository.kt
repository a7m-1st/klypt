package com.klypt.data.repositories

import com.couchbase.lite.*
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClassDocumentRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val classType = "class"

    override fun inventoryDatabaseName(): String {
        return databaseManager.currentInventoryDatabaseName
    }

    override fun inventoryDatabaseLocation(): String? {
        return databaseManager.inventoryDatabase?.path
    }

    override suspend fun get(currentUser: String): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val results = HashMap<String, Any>()
            results["_id"] = currentUser as Any

            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val documentId = getClassDocumentId(currentUser) //class::id
                val doc = db.getDocument(documentId)
                if (doc != null) {
                    if (doc.contains("type")) {
                        results["type"] = doc.getString("type") as Any
                    }
                    if (doc.contains("classCode")) {
                        results["classCode"] = doc.getString("classCode") as Any
                    }
                    if (doc.contains("classTitle")) {
                        results["classTitle"] = doc.getString("classTitle") as Any
                    }
                    if (doc.contains("updatedAt")) {
                        results["updatedAt"] = doc.getString("updatedAt") as Any
                    }
                    if (doc.contains("lastSyncedAt")) {
                        results["lastSyncedAt"] = doc.getString("lastSyncedAt") as Any
                    }
                    if (doc.contains("educatorId")) {
                        results["educatorId"] = doc.getString("educatorId") as Any
                    }
                    if (doc.contains("studentIds")) {
                        results["studentIds"] = doc.getArray("studentIds") as Any
                    }
                }
            }
            return@withContext results
        }
    }

    override suspend fun save(data: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val classId = data["_id"] as String
            val documentId = getClassDocumentId(classId)
            
            // Remove _id from the data map before creating the document
            // CouchDB Lite doesn't allow _id in the document body
            // BUT ensure the type field is preserved for querying
            val documentData = data.toMutableMap().apply { 
                remove("_id")
                // Ensure the type field exists for querying
                if (!containsKey("type")) {
                    put("type", classType)
                }
            }
            
            val mutableDocument = MutableDocument(documentId, documentData)
            try {
                val database = databaseManager.inventoryDatabase
                database?.save(mutableDocument)
                android.util.Log.d("ClassDocumentRepository", "Successfully saved class document with ID: $documentId, data: $documentData")
            } catch (e: CouchbaseLiteException) {
                android.util.Log.e("ClassDocumentRepository", "Failed to save class document: ${e.message}", e)
                return@withContext false
            }
            return@withContext true
        }
    }

    override suspend fun count(): Int {
        return withContext(Dispatchers.IO) {
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$classType'"
                val results = db.createQuery(query).execute().allResults()
                return@withContext results[0].getInt("count")
            }
            return@withContext 0
        }
    }

    suspend fun delete(documentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            var result = false
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val document = db.getDocument(documentId)
                document?.let {
                    db.delete(it)
                    result = true
                }
            }
            return@withContext result
        }
    }

    suspend fun getAllClasses(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$classType'"
                android.util.Log.d("ClassDocumentRepository", "Executing query: $query")
                val queryResults = db.createQuery(query).execute().allResults()
                android.util.Log.d("ClassDocumentRepository", "Query returned ${queryResults.size} results")
                android.util.Log.d("ClassDocumentRepository", "Query returned $queryResults")
                
                for (result in queryResults) {
                    val classData = mutableMapOf<String, Any>()
                    classData["_id"] = result.getString("_id") ?: ""
                    classData["type"] = result.getString("type") ?: ""
                    classData["classCode"] = result.getString("classCode") ?: ""
                    classData["classTitle"] = result.getString("classTitle") ?: ""
                    classData["updatedAt"] = result.getString("updatedAt") ?: ""
                    classData["lastSyncedAt"] = result.getString("lastSyncedAt") ?: ""
                    classData["educatorId"] = result.getString("educatorId") ?: ""
                    classData["studentIds"] = result.getArray("studentIds") ?: emptyList<String>()
                    
                    android.util.Log.d("ClassDocumentRepository", "Found class: $classData")
                    results.add(classData)
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null!")
            }
            android.util.Log.d("ClassDocumentRepository", "Returning ${results.size} classes")
            return@withContext results
        }
    }

    suspend fun getClassesByEducatorId(educatorId: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$classType' AND educatorId='$educatorId'"
                val queryResults = db.createQuery(query).execute().allResults()
                
                for (result in queryResults) {
                    val classData = mutableMapOf<String, Any>()
                    classData["_id"] = result.getString("_id") ?: ""
                    classData["type"] = result.getString("type") ?: ""
                    classData["classCode"] = result.getString("classCode") ?: ""
                    classData["classTitle"] = result.getString("classTitle") ?: ""
                    classData["updatedAt"] = result.getString("updatedAt") ?: ""
                    classData["lastSyncedAt"] = result.getString("lastSyncedAt") ?: ""
                    classData["educatorId"] = result.getString("educatorId") ?: ""
                    classData["studentIds"] = result.getArray("studentIds") ?: emptyList<String>()
                    results.add(classData)
                }
            }
            return@withContext results
        }
    }

    suspend fun getClassesByStudentId(studentId: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                // Fix N1QL syntax for array containment - use ANY...IN...SATISFIES instead of CONTAINS
                val query = "SELECT * FROM _ WHERE type='$classType' AND ANY studentId IN studentIds SATISFIES educatorId = '$studentId' END"
                android.util.Log.d("ClassDocumentRepository", "Executing query for student classes: $query")
                val queryResults = db.createQuery(query).execute().allResults()
                android.util.Log.d("ClassDocumentRepository", "Query returned ${queryResults.size} classes for student $studentId")
                
                for (result in queryResults) {
                    android.util.Log.d("ClassDocumentRepository", "Query returned ${result.getString("classCode")}")
                    val classData = mutableMapOf<String, Any>()
                    classData["_id"] = result.getString("_id") ?: ""
                    classData["type"] = result.getString("type") ?: ""
                    classData["classCode"] = result.getString("classCode") ?: ""
                    classData["classTitle"] = result.getString("classTitle") ?: ""
                    classData["updatedAt"] = result.getString("updatedAt") ?: ""
                    classData["lastSyncedAt"] = result.getString("lastSyncedAt") ?: ""
                    classData["educatorId"] = result.getString("educatorId") ?: ""
                    classData["studentIds"] = result.getArray("studentIds") ?: emptyList<String>()
                    
                    android.util.Log.d("ClassDocumentRepository", "Found class for student $studentId: $classData")
                    results.add(classData)
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null when getting classes for student!")
            }
            android.util.Log.d("ClassDocumentRepository", "Returning ${results.size} classes for student $studentId")
            return@withContext results
        }
    }

    suspend fun getClassByCode(classCode: String): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$classType' AND classCode='$classCode'"
                android.util.Log.d("ClassDocumentRepository", "Getting class by code: $query")
                val queryResults = db.createQuery(query).execute().allResults()
                
                if (queryResults.isNotEmpty()) {
                    val result = queryResults[0]
                    val classData = mutableMapOf<String, Any>()
                    classData["_id"] = result.getString("_id") ?: ""
                    classData["type"] = result.getString("type") ?: ""
                    classData["classCode"] = result.getString("classCode") ?: ""
                    classData["classTitle"] = result.getString("classTitle") ?: ""
                    classData["updatedAt"] = result.getString("updatedAt") ?: ""
                    classData["lastSyncedAt"] = result.getString("lastSyncedAt") ?: ""
                    classData["educatorId"] = result.getString("educatorId") ?: ""
                    classData["studentIds"] = result.getArray("studentIds") ?: emptyList<String>()
                    android.util.Log.d("ClassDocumentRepository", "Found class by code: $classData")
                    return@withContext classData
                } else {
                    android.util.Log.w("ClassDocumentRepository", "No class found with code: $classCode")
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null when getting class by code!")
            }
            return@withContext null
        }
    }

    private fun getClassDocumentId(classId: String): String {
        return "class::${classId}"
    }
}
