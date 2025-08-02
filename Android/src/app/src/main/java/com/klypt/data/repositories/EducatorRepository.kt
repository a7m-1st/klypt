package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument

import com.couchbase.learningpath.data.DatabaseManager
import com.couchbase.learningpath.data.KeyValueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EducatorRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val educatorType = "educator"

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
                val documentId = getEducatorDocumentId(currentUser) //educator::id
                val doc = db.getDocument(documentId)
                if (doc != null) {
                    if (doc.contains("type")) {
                        results["type"] = doc.getString("type") as Any
                    }
                    if (doc.contains("fullName")) {
                        results["fullName"] = doc.getString("fullName") as Any
                    }
                    if (doc.contains("age")) {
                        results["age"] = doc.getInt("age") as Any
                    }
                    if (doc.contains("currentJob")) {
                        results["currentJob"] = doc.getString("currentJob") as Any
                    }
                    if (doc.contains("instituteName")) {
                        results["instituteName"] = doc.getString("instituteName") as Any
                    }
                    if (doc.contains("phoneNumber")) {
                        results["phoneNumber"] = doc.getString("phoneNumber") as Any
                    }
                    if (doc.contains("verified")) {
                        results["verified"] = doc.getBoolean("verified") as Any
                    }
                    if (doc.contains("recoveryCode")) {
                        results["recoveryCode"] = doc.getString("recoveryCode") as Any
                    }
                    if (doc.contains("classIds")) {
                        results["classIds"] = doc.getArray("classIds") as Any
                    }
                }
            }
            return@withContext results
        }
    }

    override suspend fun save(data: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val educatorId = data["_id"] as String
            val documentId = getEducatorDocumentId(educatorId)
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
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$educatorType'"
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

    private fun getEducatorDocumentId(educatorId: String): String {
        return "educator::${educatorId}"
    }
}
