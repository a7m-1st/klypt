package com.klypt.data.repositories

import com.couchbase.lite.*
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

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
    
    suspend fun searchEducators(query: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax with LIKE expression - include document ID
                    val searchQuery = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(educatorType))
                                .and(Expression.property("fullName").like(Expression.string("%$query%")))
                        )
                    
                    android.util.Log.d("EducatorRepository", "Executing search query for: $query")
                    
                    val queryResults = searchQuery.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("EducatorRepository", "Search query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val educatorData = mutableMapOf<String, Any>()
                            
                            // Extract the actual educator ID from the document ID (remove "educator::" prefix)
                            val actualEducatorId = if (docId.startsWith("educator::")) {
                                docId.removePrefix("educator::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            educatorData["_id"] = actualEducatorId
                            educatorData["type"] = extractString(doc, "type")
                            educatorData["fullName"] = extractString(doc, "fullName")
                            educatorData["age"] = extractInt(doc, "age")
                            educatorData["currentJob"] = extractString(doc, "currentJob")
                            educatorData["instituteName"] = extractString(doc, "instituteName")
                            educatorData["phoneNumber"] = extractString(doc, "phoneNumber")
                            educatorData["verified"] = extractBoolean(doc, "verified")
                            educatorData["recoveryCode"] = extractString(doc, "recoveryCode")
                            
                            // Handle classIds array properly
                            educatorData["classIds"] = extractStringArray(doc, "classIds")
                            
                            android.util.Log.d("EducatorRepository", "Found educator: ${educatorData["fullName"]}")
                            results.add(educatorData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("EducatorRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("EducatorRepository", "Search query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("EducatorRepository", "Inventory database is null!")
            }
            
            return@withContext results
        }
    }
    
    suspend fun getAllEducators(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax - include document ID in selection
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(Expression.property("type").equalTo(Expression.string(educatorType)))
                    
                    android.util.Log.d("EducatorRepository", "Executing query for all educators")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("EducatorRepository", "Query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            val docId = result.getString("docId") ?: ""
                            
                            val educatorData = mutableMapOf<String, Any>()
                            
                            // Extract the actual educator ID from the document ID (remove "educator::" prefix)
                            val actualEducatorId = if (docId.startsWith("educator::")) {
                                docId.removePrefix("educator::")
                            } else {
                                docId
                            }
                            
                            // Safely extract each field
                            educatorData["_id"] = actualEducatorId
                            educatorData["type"] = extractString(doc, "type")
                            educatorData["fullName"] = extractString(doc, "fullName")
                            educatorData["age"] = extractInt(doc, "age")
                            educatorData["currentJob"] = extractString(doc, "currentJob")
                            educatorData["instituteName"] = extractString(doc, "instituteName")
                            educatorData["phoneNumber"] = extractString(doc, "phoneNumber")
                            educatorData["verified"] = extractBoolean(doc, "verified")
                            educatorData["recoveryCode"] = extractString(doc, "recoveryCode")
                            
                            // Handle classIds array properly
                            educatorData["classIds"] = extractStringArray(doc, "classIds")
                            
                            android.util.Log.d("EducatorRepository", "Found educator: ${educatorData["fullName"]}")
                            results.add(educatorData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("EducatorRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("EducatorRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("EducatorRepository", "Inventory database is null!")
            }
            
            android.util.Log.d("EducatorRepository", "Returning ${results.size} educators")
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

    // Helper function to safely extract integers
    private fun extractInt(doc: Any, key: String): Int {
        return when (doc) {
            is Map<*, *> -> {
                val value = doc[key]
                when (value) {
                    is Number -> value.toInt()
                    is String -> value.toIntOrNull() ?: 0
                    else -> 0
                }
            }
            is Dictionary -> doc.getInt(key)
            else -> 0
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

    private fun getEducatorDocumentId(educatorId: String): String {
        return "educator::${educatorId}"
    }
}
