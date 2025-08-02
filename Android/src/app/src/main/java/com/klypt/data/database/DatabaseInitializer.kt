package com.klypt.data.database

import com.klypt.DatabaseManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    
    fun initializeOnStartup() {
        try {
            // Initialize the databases with null user initially
            databaseManager.initializeDatabases()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to initialize databases", e)
        }
    }
    
    fun cleanup() {
        try {
            databaseManager.dispose()
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "Failed to cleanup databases", e)
        }
    }
    
    fun getDatabaseStatus(): String {
        return try {
            val inventoryDb = databaseManager.inventoryDatabase
            val warehouseDb = databaseManager.warehouseDatabase
            val klyptDb = databaseManager.klyptDatabase
            
            "Inventory DB: ${if (inventoryDb != null) "Open" else "Closed"}, " +
            "Warehouse DB: ${if (warehouseDb != null) "Open" else "Closed"}, " +
            "Klyp DB: ${if (klyptDb != null) "Open" else "Closed"}"
        } catch (e: Exception) {
            "Error getting database status: ${e.message}"
        }
    }
}
