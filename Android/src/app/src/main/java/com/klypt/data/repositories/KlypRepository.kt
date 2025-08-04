package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument
import com.klypt.data.DatabaseManager
import com.klypt.data.KeyValueRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KlypRepository(
    private val databaseManager: DatabaseManager
) : KeyValueRepository {
    private val klypType = "klyp"

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
                val query = "SELECT COUNT(*) AS count FROM _ WHERE type='$klypType'"
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

    suspend fun getKlypsByClassCode(classCode: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$klypType' AND classCode='$classCode'"
                val queryResults = db.createQuery(query).execute().allResults()
                
                for (result in queryResults) {
                    val klypData = mutableMapOf<String, Any>()
                    klypData["_id"] = result.getString("_id") ?: ""
                    klypData["type"] = result.getString("type") ?: ""
                    klypData["classCode"] = result.getString("classCode") ?: ""
                    klypData["title"] = result.getString("title") ?: ""
                    klypData["mainBody"] = result.getString("mainBody") ?: ""
                    klypData["questions"] = result.getArray("questions") ?: emptyList<Map<String, Any>>()
                    klypData["createdAt"] = result.getString("createdAt") ?: ""
                    results.add(klypData)
                }
            }
            return@withContext results
        }
    }

    suspend fun getKlypsByClassCodes(classCodes: List<String>): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                for (classCode in classCodes) {
                    val query = "SELECT * FROM _ WHERE type='$klypType' AND classCode='$classCode'"
                    val queryResults = db.createQuery(query).execute().allResults()
                    
                    for (result in queryResults) {
                        val klypData = mutableMapOf<String, Any>()
                        klypData["_id"] = result.getString("_id") ?: ""
                        klypData["type"] = result.getString("type") ?: ""
                        klypData["classCode"] = result.getString("classCode") ?: ""
                        klypData["title"] = result.getString("title") ?: ""
                        klypData["mainBody"] = result.getString("mainBody") ?: ""
                        klypData["questions"] = result.getArray("questions") ?: emptyList<Map<String, Any>>()
                        klypData["createdAt"] = result.getString("createdAt") ?: ""
                        results.add(klypData)
                    }
                }
            }
            return@withContext results
        }
    }

    suspend fun getAllKlyps(): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val query = "SELECT * FROM _ WHERE type='$klypType'"
                val queryResults = db.createQuery(query).execute().allResults()
                
                for (result in queryResults) {
                    val klypData = mutableMapOf<String, Any>()
                    klypData["_id"] = result.getString("_id") ?: ""
                    klypData["type"] = result.getString("type") ?: ""
                    klypData["classCode"] = result.getString("classCode") ?: ""
                    klypData["title"] = result.getString("title") ?: ""
                    klypData["mainBody"] = result.getString("mainBody") ?: ""
                    klypData["questions"] = result.getArray("questions") ?: emptyList<Map<String, Any>>()
                    klypData["createdAt"] = result.getString("createdAt") ?: ""
                    results.add(klypData)
                }
            }
            return@withContext results
        }
    }

    suspend fun searchKlyps(query: String): List<Map<String, Any>> {
        return withContext(Dispatchers.IO) {
            val results = mutableListOf<Map<String, Any>>()
            val database = databaseManager.inventoryDatabase
            database?.let { db ->
                val searchQuery = "SELECT * FROM _ WHERE type='$klypType' AND (title LIKE '%$query%' OR mainBody LIKE '%$query%')"
                val queryResults = db.createQuery(searchQuery).execute().allResults()
                
                for (result in queryResults) {
                    val klypData = mutableMapOf<String, Any>()
                    klypData["_id"] = result.getString("_id") ?: ""
                    klypData["type"] = result.getString("type") ?: ""
                    klypData["classCode"] = result.getString("classCode") ?: ""
                    klypData["title"] = result.getString("title") ?: ""
                    klypData["mainBody"] = result.getString("mainBody") ?: ""
                    klypData["questions"] = result.getArray("questions") ?: emptyList<Map<String, Any>>()
                    klypData["createdAt"] = result.getString("createdAt") ?: ""
                    results.add(klypData)
                }
            }
            return@withContext results
        }
    }

    private fun getKlypDocumentId(klypId: String): String {
        return "klyp::${klypId}"
    }
}
