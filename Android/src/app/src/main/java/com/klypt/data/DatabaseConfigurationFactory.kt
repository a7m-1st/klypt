package com.klypt.data

import com.couchbase.lite.DatabaseConfiguration
import java.io.File

/**
 * Factory class for creating DatabaseConfiguration instances
 * This provides a centralized way to configure CouchDB Lite databases
 */
object DatabaseConfigurationFactory {
    
    /**
     * Creates a DatabaseConfiguration with the specified directory path
     * @param directoryPath The directory path where the database files will be stored
     * @return A configured DatabaseConfiguration instance
     */
    fun create(directoryPath: String): DatabaseConfiguration {
        android.util.Log.d("DatabaseConfigurationFactory", "Creating database configuration for path: $directoryPath")
        
        val config = DatabaseConfiguration()
        
        // Ensure the directory exists
        val directory = File(directoryPath)
        if (!directory.exists()) {
            android.util.Log.d("DatabaseConfigurationFactory", "Creating directory: $directoryPath")
            directory.mkdirs()
        }
        
        // Set the directory for the database
        config.directory = directoryPath
        
        android.util.Log.d("DatabaseConfigurationFactory", "Database configuration created successfully")
        return config
    }
}
