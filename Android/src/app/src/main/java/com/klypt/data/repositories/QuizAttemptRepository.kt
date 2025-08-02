package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument
import com.klypt.DatabaseManager
import com.klypt.data.KeyValueRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizAttemptRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val quizAttemptType = "quiz_attempt"

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
                val documentId = getQuizAttemptDocumentId(currentUser) //quiz_attempt::id
                val doc = db.getDocument(documentId)
                if (doc != null) {
                    if (doc.contains("type")) {
                        results["type"] = doc.getString("type") as Any
                    }
                    if (doc.contains("studentId")) {
                        results["studentId"] = doc.getString("studentId") as Any
                    }
                    if (doc.contains("klypId")) {
                        results["klypId"] = doc.getString("klypId") as Any
                    }
                    if (doc.contains("classCode")) {
                        results["classCode"] = doc.getString("classCode") as Any
                    }
                    if (doc.contains("answers")) {
                        results["answers"] = doc.getArray("answers") as Any
                    }
                    if (doc.contains("percentageComplete")) {
                        results["percentageComplete"] = doc.getDouble("percentageComplete") as Any
                    }
                    if (doc.contains("score")) {
                        results["score"] = doc.getDouble("score") as Any
                    }
                    if (doc.contains("startedAt")) {
                        results["startedAt"] = doc.getString("startedAt") as Any
                    }
                    if (doc.contains("completedAt")) {
                        results["completedAt"] = doc.getString("completedAt") as Any
                    }
                    if (doc.contains("isSubmitted")) {
                        results["isSubmitted"] = doc.getBoolean("isSubmitted") as Any
                    }
                }
            }
            return@withContext results
        }
    }

    override suspend fun save(data: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val attemptId = data["_id"] as String
            val documentId = getQuizAttemptDocumentId(attemptId)
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
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$quizAttemptType'"
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

    private fun getQuizAttemptDocumentId(attemptId: String): String {
        return "quiz_attempt::${attemptId}"
    }
}
