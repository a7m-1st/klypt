package com.klypt.data.repositories

import com.couchbase.lite.CouchbaseLiteException
import com.couchbase.lite.MutableDocument
import com.klypt.DatabaseManager
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

    private fun getKlypDocumentId(klypId: String): String {
        return "klyp::${klypId}"
    }
}
