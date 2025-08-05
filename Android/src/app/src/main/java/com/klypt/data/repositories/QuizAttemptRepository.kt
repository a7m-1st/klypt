package com.klypt.data.repositories

import com.couchbase.lite.*
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizAttemptRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val quizAttemptType = "quiz_attempt"

    override fun inventoryDatabaseName(): String {
        return "klypts" // Use the klyp database for educational content
    }

    override fun inventoryDatabaseLocation(): String? {
        return databaseManager.klyptDatabase?.path
    }

    override suspend fun get(currentUser: String): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val results = HashMap<String, Any>()
            results["_id"] = currentUser as Any

            val database = databaseManager.klyptDatabase
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
            
            // Remove _id from the data map before creating the document
            // CouchDB Lite doesn't allow _id in the document body
            val documentData = data.toMutableMap().apply { 
                remove("_id")
                // Ensure the type field exists for querying
                if (!containsKey("type")) {
                    put("type", quizAttemptType)
                }
            }
            
            val mutableDocument = MutableDocument(documentId, documentData)
            try {
                val database = databaseManager.klyptDatabase
                database?.save(mutableDocument)
            } catch (e: CouchbaseLiteException) {
                android.util.Log.e("QuizAttemptRepository", "Failed to save quiz attempt: ${e.message}", e)
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
//        return withContext(Dispatchers.IO) {
//            val database = databaseManager.klyptDatabase
//            database?.let { db ->
//                try {
//                    // More standard CouchbaseLite query syntax for count
//                    val query = QueryBuilder
//                        .select(SelectResult.expression(Function.count(Expression.all())).as("count"))
//                        .from(DataSource.database(db))
//                        .where(Expression.property("type").equalTo(Expression.string(quizAttemptType)))
//
//                    android.util.Log.d("QuizAttemptRepository", "Executing count query for quiz attempts")
//
//                    val queryResults = query.execute()
//                    val resultsList = queryResults.allResults()
//
//                    val count = if (resultsList.isNotEmpty()) {
//                        resultsList[0].getInt("count")
//                    } else {
//                        0
//                    }
//
//                    queryResults.close() // Important: close the result set
//
//                    android.util.Log.d("QuizAttemptRepository", "Found $count quiz attempts")
//                    return@withContext count
//
//                } catch (e: Exception) {
//                    android.util.Log.e("QuizAttemptRepository", "Count query execution failed: ${e.message}", e)
//                    return@withContext 0
//                }
//            }
//            return@withContext 0
//        }
    }

    suspend fun delete(documentId: String): Boolean {
        return withContext(Dispatchers.IO) {
            var result = false
            val database = databaseManager.klyptDatabase
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

    suspend fun getAttemptsByStudentId(studentId: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.klyptDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax - include document ID in selection
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(quizAttemptType))
                                .and(Expression.property("studentId").equalTo(Expression.string(studentId)))
                        )
                    
                    android.util.Log.d("QuizAttemptRepository", "Executing query for student attempts: $studentId")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("QuizAttemptRepository", "Query returned ${resultsList.size} attempts for student $studentId")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val attemptData = mutableMapOf<String, Any>()
                            
                            // Extract the actual attempt ID from the document ID (remove "quizAttempt::" prefix)
                            val actualAttemptId = if (docId.startsWith("quizAttempt::")) {
                                docId.removePrefix("quizAttempt::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            attemptData["_id"] = actualAttemptId
                            attemptData["type"] = extractString(doc, "type")
                            attemptData["studentId"] = extractString(doc, "studentId")
                            attemptData["klypId"] = extractString(doc, "klypId")
                            attemptData["classCode"] = extractString(doc, "classCode")
                            attemptData["percentageComplete"] = extractDouble(doc, "percentageComplete")
                            attemptData["score"] = extractDouble(doc, "score")
                            attemptData["startedAt"] = extractString(doc, "startedAt")
                            attemptData["completedAt"] = extractString(doc, "completedAt")
                            attemptData["isSubmitted"] = extractBoolean(doc, "isSubmitted")
                            
                            // Handle answers array properly
                            attemptData["answers"] = extractArray(doc, "answers")
                            
                            android.util.Log.d("QuizAttemptRepository", "Found attempt for student $studentId: ${attemptData["klypId"]}")
                            results.add(attemptData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("QuizAttemptRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("QuizAttemptRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("QuizAttemptRepository", "Klyp database is null!")
            }
            
            return@withContext results
        }
    }

    suspend fun getAttemptsByKlypId(klypId: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.klyptDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax - include document ID in selection
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(quizAttemptType))
                                .and(Expression.property("klypId").equalTo(Expression.string(klypId)))
                        )
                    
                    android.util.Log.d("QuizAttemptRepository", "Executing query for klyp attempts: $klypId")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("QuizAttemptRepository", "Query returned ${resultsList.size} attempts for klyp $klypId")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val attemptData = mutableMapOf<String, Any>()
                            
                            // Extract the actual attempt ID from the document ID (remove "quizAttempt::" prefix)
                            val actualAttemptId = if (docId.startsWith("quizAttempt::")) {
                                docId.removePrefix("quizAttempt::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            attemptData["_id"] = actualAttemptId
                            attemptData["type"] = extractString(doc, "type")
                            attemptData["studentId"] = extractString(doc, "studentId")
                            attemptData["klypId"] = extractString(doc, "klypId")
                            attemptData["classCode"] = extractString(doc, "classCode")
                            attemptData["percentageComplete"] = extractDouble(doc, "percentageComplete")
                            attemptData["score"] = extractDouble(doc, "score")
                            attemptData["startedAt"] = extractString(doc, "startedAt")
                            attemptData["completedAt"] = extractString(doc, "completedAt")
                            attemptData["isSubmitted"] = extractBoolean(doc, "isSubmitted")
                            
                            // Handle answers array properly
                            attemptData["answers"] = extractArray(doc, "answers")
                            
                            android.util.Log.d("QuizAttemptRepository", "Found attempt for klyp $klypId: ${attemptData["studentId"]}")
                            results.add(attemptData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("QuizAttemptRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("QuizAttemptRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("QuizAttemptRepository", "Klyp database is null!")
            }
            
            return@withContext results
        }
    }

    // Helper function to safely extract strings
    private fun extractString(doc: Any, key: String): String {
        return when (doc) {
            is Map<*, *> -> doc[key]?.toString() ?: ""
            is Dictionary -> doc.getString(key) ?: ""
            else -> ""
        }
    }

    // Helper function to safely extract doubles
    private fun extractDouble(doc: Any, key: String): Double {
        return when (doc) {
            is Map<*, *> -> {
                val value = doc[key]
                when (value) {
                    is Number -> value.toDouble()
                    is String -> value.toDoubleOrNull() ?: 0.0
                    else -> 0.0
                }
            }
            is Dictionary -> doc.getDouble(key)
            else -> 0.0
        }
    }

    // Helper function to safely extract booleans
    private fun extractBoolean(doc: Any, key: String): Boolean {
        return when (doc) {
            is Map<*, *> -> {
                val value = doc[key]
                when (value) {
                    is Boolean -> value
                    is String -> value.toBoolean()
                    else -> false
                }
            }
            is Dictionary -> doc.getBoolean(key)
            else -> false
        }
    }

    // Helper function to safely extract arrays
    private fun extractArray(doc: Any, key: String): List<Any> {
        return when (doc) {
            is Map<*, *> -> {
                val array = doc[key]
                when (array) {
                    is List<*> -> array.mapNotNull { it }
                    else -> emptyList()
                }
            }
            is Dictionary -> {
                val array = doc.getArray(key)
                array?.let { arr ->
                    (0 until arr.count()).mapNotNull { arr.getValue(it) }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getQuizAttemptDocumentId(attemptId: String): String {
        return "quiz_attempt::${attemptId}"
    }
}
