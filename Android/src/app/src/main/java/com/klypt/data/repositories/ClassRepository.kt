package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository
import com.klypt.data.models.ClassDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClassRepository(
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
                val documentId = getClassDocumentId(currentUser)
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
            val documentData = data.toMutableMap().apply { 
                remove("_id") 
            }
            
            val mutableDocument = MutableDocument(documentId, documentData)
            try {
                val database = databaseManager.inventoryDatabase
                database?.save(mutableDocument)
            } catch (e: CouchbaseLiteException) {
                android.util.Log.e(e.message, e.stackTraceToString())
                return@withContext false
            }
            return@withContext true
        }
    }

    override suspend fun count(): Int {
        return withContext(Dispatchers.IO) {
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT COUNT(*) as count FROM _ WHERE type='$classType'"
                val result = db.createQuery(query).execute().allResults()
                if (result.isNotEmpty()) {
                    return@withContext result[0].getInt("count")
                }
            }
            return@withContext 0
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
                val query = "SELECT * FROM _ WHERE type='$classType' AND ANY student IN studentIds SATISFIES student = '$studentId' END"
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

    suspend fun getAllClasses(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$classType'"
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

    private fun getClassDocumentId(classId: String): String {
        return "class::${classId}"
    }
}
