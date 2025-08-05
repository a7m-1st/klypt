package com.klypt.data.repositories

import com.couchbase.lite.*
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

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
    
    suspend fun searchStudents(query: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax with LIKE expression for both firstName and lastName - include document ID
                    val searchQuery = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(studentType))
                                .and(
                                    Expression.property("firstName").like(Expression.string("%$query%"))
                                        .or(Expression.property("lastName").like(Expression.string("%$query%")))
                                )
                        )
                    
                    android.util.Log.d("StudentRepository", "Executing search query for: $query")
                    
                    val queryResults = searchQuery.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("StudentRepository", "Search query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val studentData = mutableMapOf<String, Any>()
                            
                            // Extract the actual student ID from the document ID (remove "student::" prefix)
                            val actualStudentId = if (docId.startsWith("student::")) {
                                docId.removePrefix("student::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            studentData["_id"] = actualStudentId
                            studentData["type"] = extractString(doc, "type")
                            studentData["firstName"] = extractString(doc, "firstName")
                            studentData["lastName"] = extractString(doc, "lastName")
                            studentData["recoveryCode"] = extractString(doc, "recoveryCode")
                            studentData["createdAt"] = extractString(doc, "createdAt")
                            studentData["updatedAt"] = extractString(doc, "updatedAt")
                            
                            // Handle enrolledClassIds array properly
                            studentData["enrolledClassIds"] = extractStringArray(doc, "enrolledClassIds")
                            
                            android.util.Log.d("StudentRepository", "Found student: ${studentData["firstName"]} ${studentData["lastName"]}")
                            results.add(studentData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("StudentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("StudentRepository", "Search query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("StudentRepository", "Inventory database is null!")
            }
            
            return@withContext results
        }
    }
    
    suspend fun getAllStudents(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax - include document ID in selection
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(Expression.property("type").equalTo(Expression.string(studentType)))
                    
                    android.util.Log.d("StudentRepository", "Executing query for all students")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("StudentRepository", "Query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val studentData = mutableMapOf<String, Any>()
                            
                            // Extract the actual student ID from the document ID (remove "student::" prefix)
                            val actualStudentId = if (docId.startsWith("student::")) {
                                docId.removePrefix("student::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            studentData["_id"] = actualStudentId
                            studentData["type"] = extractString(doc, "type")
                            studentData["firstName"] = extractString(doc, "firstName")
                            studentData["lastName"] = extractString(doc, "lastName")
                            studentData["recoveryCode"] = extractString(doc, "recoveryCode")
                            studentData["createdAt"] = extractString(doc, "createdAt")
                            studentData["updatedAt"] = extractString(doc, "updatedAt")
                            
                            // Handle enrolledClassIds array properly
                            studentData["enrolledClassIds"] = extractStringArray(doc, "enrolledClassIds")
                            
                            android.util.Log.d("StudentRepository", "Found student: ${studentData["firstName"]} ${studentData["lastName"]}")
                            results.add(studentData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("StudentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("StudentRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("StudentRepository", "Inventory database is null!")
            }
            
            android.util.Log.d("StudentRepository", "Returning ${results.size} students")
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

    // Helper function to safely extract string arrays
    private fun extractStringArray(doc: Any, key: String): List<String> {
        return when (doc) {
            is Map<*, *> -> {
                val array = doc[key]
                when (array) {
                    is List<*> -> array.mapNotNull { it?.toString() }
                    else -> emptyList()
                }
            }
            is Dictionary -> {
                val array = doc.getArray(key)
                array?.let { arr ->
                    (0 until arr.count()).mapNotNull { arr.getString(it) }
                } ?: emptyList()
            }
            else -> emptyList()
        }
    }

    private fun getStudentDocumentId(studentId: String): String {
        return "student::${studentId}"
    }
}
