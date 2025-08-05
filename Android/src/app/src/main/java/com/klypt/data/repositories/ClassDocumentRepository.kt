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
        return 0
//        return withContext(Dispatchers.IO) {
//            val database = databaseManager.inventoryDatabase
//            database?.let { db ->
//                try {
//                    // More standard CouchbaseLite query syntax for count
//                    val query = QueryBuilder
//                        .select(SelectResult.expression(com.couchbase.lite.Function.count(Expression.string("*"))).as("count"))
//                        .from(DataSource.database(db))
//                        .where(Expression.property("type").equalTo(Expression.string(classType)))
//
//                    android.util.Log.d("ClassDocumentRepository", "Executing count query for classes")
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
//                    android.util.Log.d("ClassDocumentRepository", "Found $count classes")
//                    return@withContext count
//
//                } catch (e: Exception) {
//                    android.util.Log.e("ClassDocumentRepository", "Count query execution failed: ${e.message}", e)
//                    return@withContext 0
//                }
//            }
//            return@withContext 0
//        }
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
                try {
                    // More standard CouchbaseLite query syntax
                    val query = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.database(db))
                        .where(Expression.property("type").equalTo(Expression.string(classType)))
                    
                    android.util.Log.d("ClassDocumentRepository", "Executing query for all classes")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("ClassDocumentRepository", "Query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            
                            val classData = mutableMapOf<String, Any>()
                            
                            // Safely extract each field
                            classData["_id"] = extractString(doc, "_id")
                            classData["type"] = extractString(doc, "type")
                            classData["classCode"] = extractString(doc, "classCode")
                            classData["classTitle"] = extractString(doc, "classTitle")
                            classData["updatedAt"] = extractString(doc, "updatedAt")
                            classData["lastSyncedAt"] = extractString(doc, "lastSyncedAt")
                            classData["educatorId"] = extractString(doc, "educatorId")
                            
                            // Handle studentIds array properly
                            classData["studentIds"] = extractStringArray(doc, "studentIds")
                            
                            android.util.Log.d("ClassDocumentRepository", "Found class: ${classData["classCode"]} - ${classData["classTitle"]}")
                            results.add(classData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("ClassDocumentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("ClassDocumentRepository", "Query execution failed: ${e.message}", e)
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
                try {
                    // More standard CouchbaseLite query syntax
                    val query = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(classType))
                                .and(Expression.property("educatorId").equalTo(Expression.string(educatorId)))
                        )
                    
                    android.util.Log.d("ClassDocumentRepository", "Executing query for educator classes: $educatorId")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("ClassDocumentRepository", "Query returned ${resultsList.size} results for educator $educatorId")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            
                            val classData = mutableMapOf<String, Any>()
                            
                            // Safely extract each field
                            classData["_id"] = extractString(doc, "_id")
                            classData["type"] = extractString(doc, "type")
                            classData["classCode"] = extractString(doc, "classCode")
                            classData["classTitle"] = extractString(doc, "classTitle")
                            classData["updatedAt"] = extractString(doc, "updatedAt")
                            classData["lastSyncedAt"] = extractString(doc, "lastSyncedAt")
                            classData["educatorId"] = extractString(doc, "educatorId")
                            
                            // Handle studentIds array properly
                            classData["studentIds"] = extractStringArray(doc, "studentIds")
                            
                            android.util.Log.d("ClassDocumentRepository", "Found class for educator $educatorId: ${classData["classCode"]} - ${classData["classTitle"]}")
                            results.add(classData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("ClassDocumentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("ClassDocumentRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null!")
            }
            
            return@withContext results
        }
    }

    suspend fun getClassesByStudentId(studentId: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // Use QueryBuilder for array containment query
                    val query = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(classType))
                                .and(ArrayFunction.contains(Expression.property("studentIds"), Expression.string(studentId)))
                        )
                    
                    android.util.Log.d("ClassDocumentRepository", "Executing query for student classes: $studentId")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("ClassDocumentRepository", "Query returned ${resultsList.size} classes for student $studentId")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            
                            val classData = mutableMapOf<String, Any>()
                            
                            // Safely extract each field
                            classData["_id"] = extractString(doc, "_id")
                            classData["type"] = extractString(doc, "type")
                            classData["classCode"] = extractString(doc, "classCode")
                            classData["classTitle"] = extractString(doc, "classTitle")
                            classData["updatedAt"] = extractString(doc, "updatedAt")
                            classData["lastSyncedAt"] = extractString(doc, "lastSyncedAt")
                            classData["educatorId"] = extractString(doc, "educatorId")
                            
                            // Handle studentIds array properly
                            classData["studentIds"] = extractStringArray(doc, "studentIds")
                            
                            android.util.Log.d("ClassDocumentRepository", "Found class for student $studentId: ${classData["classCode"]} - ${classData["classTitle"]}")
                            results.add(classData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("ClassDocumentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("ClassDocumentRepository", "Query execution failed: ${e.message}", e)
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
                try {
                    // More standard CouchbaseLite query syntax
                    val query = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(classType))
                                .and(Expression.property("classCode").equalTo(Expression.string(classCode)))
                        )
                        .limit(Expression.intValue(1)) // Limit to 1 result since we expect unique class codes
                    
                    android.util.Log.d("ClassDocumentRepository", "Getting class by code: $classCode")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    if (resultsList.isNotEmpty()) {
                        try {
                            val result = resultsList[0]
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            
                            val classData = mutableMapOf<String, Any>()
                            
                            // Safely extract each field
                            classData["_id"] = extractString(doc, "_id")
                            classData["type"] = extractString(doc, "type")
                            classData["classCode"] = extractString(doc, "classCode")
                            classData["classTitle"] = extractString(doc, "classTitle")
                            classData["updatedAt"] = extractString(doc, "updatedAt")
                            classData["lastSyncedAt"] = extractString(doc, "lastSyncedAt")
                            classData["educatorId"] = extractString(doc, "educatorId")
                            
                            // Handle studentIds array properly
                            classData["studentIds"] = extractStringArray(doc, "studentIds")
                            
                            android.util.Log.d("ClassDocumentRepository", "Found class by code: ${classData["classCode"]} - ${classData["classTitle"]}")
                            
                            queryResults.close() // Important: close the result set
                            return@withContext classData
                            
                        } catch (e: Exception) {
                            android.util.Log.e("ClassDocumentRepository", "Error processing result: ${e.message}", e)
                        }
                    } else {
                        android.util.Log.w("ClassDocumentRepository", "No class found with code: $classCode")
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("ClassDocumentRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null when getting class by code!")
            }
            
            return@withContext null
        }
    }

    suspend fun getClassesByType(classType: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax
                    val query = QueryBuilder
                        .select(SelectResult.all())
                        .from(DataSource.database(db))
                        .where(Expression.property("type").equalTo(Expression.string(classType)))
                    
                    android.util.Log.d("ClassDocumentRepository", "Executing query for type: $classType")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("ClassDocumentRepository", "Query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name) ?: result.toMap()
                            
                            val classData = mutableMapOf<String, Any>()
                            
                            // Safely extract each field
                            classData["_id"] = extractString(doc, "_id")
                            classData["type"] = extractString(doc, "type")
                            classData["classCode"] = extractString(doc, "classCode")
                            classData["classTitle"] = extractString(doc, "classTitle")
                            classData["updatedAt"] = extractString(doc, "updatedAt")
                            classData["lastSyncedAt"] = extractString(doc, "lastSyncedAt")
                            classData["educatorId"] = extractString(doc, "educatorId")
                            
                            // Handle studentIds array properly
                            classData["studentIds"] = extractStringArray(doc, "studentIds")
                            
                            android.util.Log.d("ClassDocumentRepository", "Found class: ${classData["classCode"]} - ${classData["classTitle"]}")
                            results.add(classData)
                            
                        } catch (e: Exception) {
                            android.util.Log.e("ClassDocumentRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("ClassDocumentRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("ClassDocumentRepository", "Inventory database is null!")
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

    private fun getClassDocumentId(classId: String): String {
        return "class::${classId}"
    }
}
