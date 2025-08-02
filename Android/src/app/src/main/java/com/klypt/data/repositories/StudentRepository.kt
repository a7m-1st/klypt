package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument

import com.couchbase.learningpath.data.DatabaseManager
import com.couchbase.learningpath.data.KeyValueRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StudentRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val studentType = "student"

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
                val documentId = getStudentDocumentId(currentUser) //student::id
                val doc = db.getDocument(documentId)
                if (doc != null) {
                    if (doc.contains("type")) {
                        results["type"] = doc.getString("type") as Any
                    }
                    if (doc.contains("firstName")) {
                        results["firstName"] = doc.getString("firstName") as Any
                    }
                    if (doc.contains("lastName")) {
                        results["lastName"] = doc.getString("lastName") as Any
                    }
                    if (doc.contains("recoveryCode")) {
                        results["recoveryCode"] = doc.getString("recoveryCode") as Any
                    }
                    if (doc.contains("enrolledClassIds")) {
                        results["enrolledClassIds"] = doc.getArray("enrolledClassIds") as Any
                    }
                    if (doc.contains("createdAt")) {
                        results["createdAt"] = doc.getString("createdAt") as Any
                    }
                    if (doc.contains("updatedAt")) {
                        results["updatedAt"] = doc.getString("updatedAt") as Any
                    }
                }
            }
            return@withContext results
        }
    }

    override suspend fun save(data: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val studentId = data["_id"] as String
            val documentId = getStudentDocumentId(studentId)
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
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$studentType'"
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

    private fun getStudentDocumentId(studentId: String): String {
        return "student::${studentId}"
    }
}
