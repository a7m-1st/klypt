package com.klypt.data.repositories

import com.couchbase.lite.*
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KlypRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val klypType = "klyp"

    override fun inventoryDatabaseName(): String {
        return "klypts" // Use the klyp database name directly
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
                val documentId = getKlypDocumentId(currentUser) //klyp::id
                val doc = db.getDocument(documentId)
                if (doc != null) {
                    if (doc.contains("type")) {
                        results["type"] = doc.getString("type") as Any
                    }
                    if (doc.contains("classCode")) {
                        results["classCode"] = doc.getString("classCode") as Any
                    }
                    if (doc.contains("title")) {
                        results["title"] = doc.getString("title") as Any
                    }
                    if (doc.contains("mainBody")) {
                        results["mainBody"] = doc.getString("mainBody") as Any
                    }
                    if (doc.contains("questions")) {
                        results["questions"] = doc.getArray("questions") as Any
                    }
                    if (doc.contains("createdAt")) {
                        results["createdAt"] = doc.getString("createdAt") as Any
                    }
                }
            }
            return@withContext results
        }
    }

    override suspend fun save(data: Map<String, Any>): Boolean {
        return withContext(Dispatchers.IO) {
            val klypId = data["_id"] as String
            val documentId = getKlypDocumentId(klypId)
            
            // Remove _id from the data map before creating the document
            // CouchDB Lite doesn't allow _id in the document body
            val documentData = data.toMutableMap().apply { 
                remove("_id")
                // Ensure the type field exists for querying
                if (!containsKey("type")) {
                    put("type", klypType)
                }
            }
            
            val mutableDocument = MutableDocument(documentId, documentData)
            try {
                val database = databaseManager.klyptDatabase
                database?.save(mutableDocument)
                android.util.Log.d("KlypRepository", "Successfully saved klyp document with ID: $documentId")
            } catch (e: CouchbaseLiteException) {
                android.util.Log.e("KlypRepository", "Failed to save klyp document: ${e.message}", e)
                return@withContext false
            }
            return@withContext true
        }
    }

    override suspend fun count(): Int {
        return withContext(Dispatchers.IO) {
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$klypType'"
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
//                        .where(Expression.property("type").equalTo(Expression.string(klypType)))
//
//                    android.util.Log.d("KlypRepository", "Executing count query for klyps")
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
//                    android.util.Log.d("KlypRepository", "Found $count klyps")
//                    return@withContext count
//
//                } catch (e: Exception) {
//                    android.util.Log.e("KlypRepository", "Count query execution failed: ${e.message}", e)
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
                val docId = getKlypDocumentId(documentId)
                val document = db.getDocument(docId)
                document?.let {
                    db.delete(it)
                    result = true
                }
            }
            return@withContext result
        }
    }

    suspend fun getKlypsByClassCode(classCode: String): List<Map<String, Any>> {
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
                            Expression.property("type").equalTo(Expression.string(klypType))
                                .and(Expression.property("classCode").equalTo(Expression.string(classCode)))
                        )
                    
                    android.util.Log.d("KlypRepository", "Executing query for klyps in class: $classCode")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("KlypRepository", "Query returned ${resultsList.size} klyps for class $classCode")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name)
                            val docId = result.getString("docId") ?: ""
                            
                            if (doc != null) {
                                val klypData = mutableMapOf<String, Any>()
                                
                                // Extract the actual klyp ID from the document ID (remove "klyp::" prefix)
                                val actualKlypId = if (docId.startsWith("klyp::")) {
                                    docId.removePrefix("klyp::")
                                } else {
                                    docId
                                }
                                
                                // Eagerly copy all data to avoid Fleece object issues
                                klypData["_id"] = actualKlypId
                                klypData["type"] = doc.getString("type") ?: ""
                                klypData["classCode"] = doc.getString("classCode") ?: ""
                                klypData["title"] = doc.getString("title") ?: ""
                                klypData["mainBody"] = doc.getString("mainBody") ?: ""
                                klypData["createdAt"] = doc.getString("createdAt") ?: ""
                                
                                // Handle questions array properly by eagerly copying the data
                                val questionsArray = doc.getArray("questions")
                                val questionsList = mutableListOf<Map<String, Any>>()
                                
                                if (questionsArray != null) {
                                    for (i in 0 until questionsArray.count()) {
                                        val questionDict = questionsArray.getDictionary(i)
                                        if (questionDict != null) {
                                            val questionMap = mutableMapOf<String, Any>()
                                            questionMap["questionText"] = questionDict.getString("questionText") ?: ""
                                            questionMap["correctAnswer"] = questionDict.getString("correctAnswer") ?: ""
                                            
                                            // Handle options array
                                            val optionsArray = questionDict.getArray("options")
                                            val optionsList = mutableListOf<String>()
                                            if (optionsArray != null) {
                                                for (j in 0 until optionsArray.count()) {
                                                    optionsArray.getString(j)?.let { optionsList.add(it) }
                                                }
                                            }
                                            questionMap["options"] = optionsList
                                            
                                            questionsList.add(questionMap)
                                        }
                                    }
                                }
                                klypData["questions"] = questionsList
                                
                                android.util.Log.d("KlypRepository", "Found klyp: ${klypData["title"]} in class $classCode")
                                results.add(klypData)
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("KlypRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("KlypRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("KlypRepository", "Klyp database is null!")
            }
            
            return@withContext results
        }
    }

    suspend fun getKlypsByClassCodes(classCodes: List<String>): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.klyptDatabase
            
            database?.let { db ->
                try {
                    // Use QueryBuilder with IN expression for multiple class codes - include document ID in selection
                    val classCodeExpressions = classCodes.map { Expression.string(it) }
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(klypType))
                                .and(Expression.property("classCode").`in`(*classCodeExpressions.toTypedArray()))
                        )
                    
                    android.util.Log.d("KlypRepository", "Executing query for klyps in classes: $classCodes")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("KlypRepository", "Query returned ${resultsList.size} klyps for classes $classCodes")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name)
                            val docId = result.getString("docId") ?: ""
                            
                            if (doc != null) {
                                val klypData = mutableMapOf<String, Any>()
                                
                                // Extract the actual klyp ID from the document ID (remove "klyp::" prefix)
                                val actualKlypId = if (docId.startsWith("klyp::")) {
                                    docId.removePrefix("klyp::")
                                } else {
                                    docId
                                }
                                
                                // Eagerly copy all data to avoid Fleece object issues
                                klypData["_id"] = actualKlypId
                                klypData["type"] = doc.getString("type") ?: ""
                                klypData["classCode"] = doc.getString("classCode") ?: ""
                                klypData["title"] = doc.getString("title") ?: ""
                                klypData["mainBody"] = doc.getString("mainBody") ?: ""
                                klypData["createdAt"] = doc.getString("createdAt") ?: ""
                                
                                // Handle questions array properly by eagerly copying the data
                                val questionsArray = doc.getArray("questions")
                                val questionsList = mutableListOf<Map<String, Any>>()
                                
                                if (questionsArray != null) {
                                    for (i in 0 until questionsArray.count()) {
                                        val questionDict = questionsArray.getDictionary(i)
                                        if (questionDict != null) {
                                            val questionMap = mutableMapOf<String, Any>()
                                            questionMap["questionText"] = questionDict.getString("questionText") ?: ""
                                            questionMap["correctAnswer"] = questionDict.getString("correctAnswer") ?: ""
                                            
                                            // Handle options array
                                            val optionsArray = questionDict.getArray("options")
                                            val optionsList = mutableListOf<String>()
                                            if (optionsArray != null) {
                                                for (j in 0 until optionsArray.count()) {
                                                    optionsArray.getString(j)?.let { optionsList.add(it) }
                                                }
                                            }
                                            questionMap["options"] = optionsList
                                            
                                            questionsList.add(questionMap)
                                        }
                                    }
                                }
                                klypData["questions"] = questionsList
                                
                                android.util.Log.d("KlypRepository", "Found klyp: ${klypData["title"]} in class ${klypData["classCode"]}")
                                results.add(klypData)
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("KlypRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("KlypRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("KlypRepository", "Klyp database is null!")
            }
            
            return@withContext results
        }
    }

    suspend fun getAllKlyps(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.klyptDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax - include document ID in selection
                    val query = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(Expression.property("type").equalTo(Expression.string(klypType)))
                    
                    android.util.Log.d("KlypRepository", "Executing query for all klyps")
                    
                    val queryResults = query.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("KlypRepository", "Query returned ${resultsList.size} total klyps")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name)
                            val docId = result.getString("docId") ?: ""
                            
                            if (doc != null) {
                                val klypData = mutableMapOf<String, Any>()
                                
                                // Extract the actual klyp ID from the document ID (remove "klyp::" prefix)
                                val actualKlypId = if (docId.startsWith("klyp::")) {
                                    docId.removePrefix("klyp::")
                                } else {
                                    docId
                                }
                                
                                // Eagerly copy all data to avoid Fleece object issues
                                klypData["_id"] = actualKlypId
                                klypData["type"] = doc.getString("type") ?: ""
                                klypData["classCode"] = doc.getString("classCode") ?: ""
                                klypData["title"] = doc.getString("title") ?: ""
                                klypData["mainBody"] = doc.getString("mainBody") ?: ""
                                klypData["createdAt"] = doc.getString("createdAt") ?: ""
                                
                                // Handle questions array properly by eagerly copying the data
                                val questionsArray = doc.getArray("questions")
                                val questionsList = mutableListOf<Map<String, Any>>()
                                
                                if (questionsArray != null) {
                                    for (i in 0 until questionsArray.count()) {
                                        val questionDict = questionsArray.getDictionary(i)
                                        if (questionDict != null) {
                                            val questionMap = mutableMapOf<String, Any>()
                                            questionMap["questionText"] = questionDict.getString("questionText") ?: ""
                                            questionMap["correctAnswer"] = questionDict.getString("correctAnswer") ?: ""
                                            
                                            // Handle options array
                                            val optionsArray = questionDict.getArray("options")
                                            val optionsList = mutableListOf<String>()
                                            if (optionsArray != null) {
                                                for (j in 0 until optionsArray.count()) {
                                                    optionsArray.getString(j)?.let { optionsList.add(it) }
                                                }
                                            }
                                            questionMap["options"] = optionsList
                                            
                                            questionsList.add(questionMap)
                                        }
                                    }
                                }
                                klypData["questions"] = questionsList
                                
                                android.util.Log.d("KlypRepository", "Found klyp: ${klypData["title"]}")
                                results.add(klypData)
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("KlypRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("KlypRepository", "Query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("KlypRepository", "Klyp database is null when getting all klyps!")
            }
            
            android.util.Log.d("KlypRepository", "Returning ${results.size} klyps")
            return@withContext results
        }
    }

    suspend fun searchKlyps(query: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.klyptDatabase
            
            database?.let { db ->
                try {
                    // More standard CouchbaseLite query syntax with LIKE expressions for search - include document ID in selection
                    val searchQuery = QueryBuilder
                        .select(SelectResult.all(), SelectResult.expression(Meta.id).`as`("docId"))
                        .from(DataSource.database(db))
                        .where(
                            Expression.property("type").equalTo(Expression.string(klypType))
                                .and(
                                    Expression.property("title").like(Expression.string("%$query%"))
                                        .or(Expression.property("mainBody").like(Expression.string("%$query%")))
                                )
                        )
                    
                    android.util.Log.d("KlypRepository", "Executing search query for: $query")
                    
                    val queryResults = searchQuery.execute()
                    val resultsList = queryResults.allResults()
                    
                    android.util.Log.d("KlypRepository", "Search query returned ${resultsList.size} results")
                    
                    for (result in resultsList) {
                        try {
                            // Handle the nested structure - CouchbaseLite wraps results in database name
                            val doc = result.getDictionary(db.name)
                            val docId = result.getString("docId") ?: ""
                            
                            if (doc != null) {
                                val klypData = mutableMapOf<String, Any>()
                                
                                // Extract the actual klyp ID from the document ID (remove "klyp::" prefix)
                                val actualKlypId = if (docId.startsWith("klyp::")) {
                                    docId.removePrefix("klyp::")
                                } else {
                                    docId
                                }
                                
                                // Eagerly copy all data to avoid Fleece object issues
                                klypData["_id"] = actualKlypId
                                klypData["type"] = doc.getString("type") ?: ""
                                klypData["classCode"] = doc.getString("classCode") ?: ""
                                klypData["title"] = doc.getString("title") ?: ""
                                klypData["mainBody"] = doc.getString("mainBody") ?: ""
                                klypData["createdAt"] = doc.getString("createdAt") ?: ""
                                
                                // Handle questions array properly by eagerly copying the data
                                val questionsArray = doc.getArray("questions")
                                val questionsList = mutableListOf<Map<String, Any>>()
                                
                                if (questionsArray != null) {
                                    for (i in 0 until questionsArray.count()) {
                                        val questionDict = questionsArray.getDictionary(i)
                                        if (questionDict != null) {
                                            val questionMap = mutableMapOf<String, Any>()
                                            questionMap["questionText"] = questionDict.getString("questionText") ?: ""
                                            questionMap["correctAnswer"] = questionDict.getString("correctAnswer") ?: ""
                                            
                                            // Handle options array
                                            val optionsArray = questionDict.getArray("options")
                                            val optionsList = mutableListOf<String>()
                                            if (optionsArray != null) {
                                                for (j in 0 until optionsArray.count()) {
                                                    optionsArray.getString(j)?.let { optionsList.add(it) }
                                                }
                                            }
                                            questionMap["options"] = optionsList
                                            
                                            questionsList.add(questionMap)
                                        }
                                    }
                                }
                                klypData["questions"] = questionsList
                                
                                android.util.Log.d("KlypRepository", "Found klyp: ${klypData["title"]}")
                                results.add(klypData)
                            }
                            
                        } catch (e: Exception) {
                            android.util.Log.e("KlypRepository", "Error processing result: ${e.message}", e)
                        }
                    }
                    
                    queryResults.close() // Important: close the result set
                    
                } catch (e: Exception) {
                    android.util.Log.e("KlypRepository", "Search query execution failed: ${e.message}", e)
                }
            } ?: run {
                android.util.Log.w("KlypRepository", "Klyp database is null!")
            }
            
            return@withContext results
        }
    }

    private fun getKlypDocumentId(klypId: String): String {
        return "klyp::${klypId}"
    }
}
