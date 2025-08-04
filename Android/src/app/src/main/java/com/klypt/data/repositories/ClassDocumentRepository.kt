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
            val mutableDocument = MutableDocument(documentId, data)
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

    private fun getClassDocumentId(classId: String): String {
        return "class::${classId}"
    }
}
