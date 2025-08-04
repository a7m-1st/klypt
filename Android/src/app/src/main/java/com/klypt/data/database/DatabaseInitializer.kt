package com.klypt.data.database

import com.klypt.data.DatabaseManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseInitializer @Inject constructor(
    private val databaseManager: DatabaseManager
) {
    
    fun initializeOnStartup() {
        android.util.Log.d("DatabaseInitializer", "================================")
        android.util.Log.d("DatabaseInitializer", "DATABASE INITIALIZER CALLED")
        android.util.Log.d("DatabaseInitializer", "================================")
        try {
            android.util.Log.d("DatabaseInitializer", "Starting database initialization...")
            android.util.Log.d("DatabaseInitializer", "DatabaseManager instance: $databaseManager")
            
            // Initialize the databases with null user initially
            databaseManager.initializeDatabases()
            
            android.util.Log.d("DatabaseInitializer", "Checking database states after initialization:")
            android.util.Log.d("DatabaseInitializer", "Inventory DB: ${databaseManager.inventoryDatabase != null}")
            android.util.Log.d("DatabaseInitializer", "Warehouse DB: ${databaseManager.warehouseDatabase != null}")
            android.util.Log.d("DatabaseInitializer", "Klyp DB: ${databaseManager.klyptDatabase != null}")
            
            android.util.Log.d("DatabaseInitializer", "Database initialization completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("DatabaseInitializer", "CRITICAL: Failed to initialize databases", e)
            android.util.Log.e("DatabaseInitializer", "Exception message: ${e.message}")
            android.util.Log.e("DatabaseInitializer", "Exception details:", e)
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
