/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.klypt.ui.debug

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.klypt.data.repository.EducationalContentRepository
import com.klypt.data.utils.DatabaseExplorer
import com.klypt.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Debug activity for exploring database content
 * This should only be used in debug builds
 */
@AndroidEntryPoint
class DatabaseExplorerActivity : ComponentActivity() {

    @Inject
    lateinit var educationalContentRepository: EducationalContentRepository
    
    @Inject
    lateinit var databaseExplorer: DatabaseExplorer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            GalleryTheme {
                DatabaseExplorerScreen(
                    educationalContentRepository = educationalContentRepository,
                    databaseExplorer = databaseExplorer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatabaseExplorerScreen(
    educationalContentRepository: EducationalContentRepository,
    databaseExplorer: DatabaseExplorer
) {
    var selectedTab by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Ready") }
    var databaseOverview by remember { mutableStateOf<Map<String, Any>?>(null) }
    var searchResults by remember { mutableStateOf<Map<String, List<Map<String, Any>>>?>(null) }
    var analytics by remember { mutableStateOf<Map<String, Any>?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top App Bar
        TopAppBar(
            title = { Text("Database Explorer") }
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = selectedTab
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Overview") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Search") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("Analytics") }
            )
            Tab(
                selected = selectedTab == 3,
                onClick = { selectedTab = 3 },
                text = { Text("Actions") }
            )
        }
        
        // Status Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isLoading) MaterialTheme.colorScheme.secondary
                else MaterialTheme.colorScheme.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Content based on selected tab
        when (selectedTab) {
            0 -> OverviewTab(
                databaseOverview = databaseOverview,
                onLoadOverview = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "Loading database overview..."
                        try {
                            databaseOverview = educationalContentRepository.getDatabaseOverview()
                            statusMessage = "Overview loaded successfully"
                        } catch (e: Exception) {
                            statusMessage = "Error loading overview: ${e.message}"
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
            
            1 -> SearchTab(
                searchQuery = searchQuery,
                searchResults = searchResults,
                onSearchQueryChange = { searchQuery = it },
                onSearch = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "Searching database..."
                        try {
                            searchResults = educationalContentRepository.searchAllCollections(searchQuery)
                            val totalResults = searchResults?.values?.sumOf { it.size } ?: 0
                            statusMessage = "Found $totalResults results"
                        } catch (e: Exception) {
                            statusMessage = "Search error: ${e.message}"
                            Toast.makeText(context, "Search error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
            
            2 -> AnalyticsTab(
                analytics = analytics,
                onLoadAnalytics = {
                    scope.launch {
                        isLoading = true
                        statusMessage = "Generating analytics..."
                        try {
                            analytics = educationalContentRepository.getDatabaseAnalytics()
                            statusMessage = "Analytics generated successfully"
                        } catch (e: Exception) {
                            statusMessage = "Error generating analytics: ${e.message}"
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                }
            )
            
            3 -> ActionsTab(
                databaseExplorer = databaseExplorer,
                onStatusUpdate = { message ->
                    statusMessage = message
                },
                onLoadingUpdate = { loading ->
                    isLoading = loading
                }
            )
        }
    }
}

@Composable
fun OverviewTab(
    databaseOverview: Map<String, Any>?,
    onLoadOverview: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onLoadOverview,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Load Database Overview")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        databaseOverview?.let { overview ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(overview.entries.toList()) { (key, value) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = key.replaceFirstChar { it.uppercase() }.replace("_", " "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (value) {
                                is Map<*, *> -> {
                                    value.entries.forEach { (subKey, subValue) ->
                                        Text(
                                            text = "• $subKey: $subValue",
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                                is List<*> -> {
                                    Text(
                                        text = "List with ${value.size} items",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                else -> {
                                    Text(
                                        text = value.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchTab(
    searchQuery: String,
    searchResults: Map<String, List<Map<String, Any>>>?,
    onSearchQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search query") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = onSearch,
            modifier = Modifier.fillMaxWidth(),
            enabled = searchQuery.isNotBlank()
        ) {
            Text("Search All Collections")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        searchResults?.let { results ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results.entries.toList()) { (collectionName, documents) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "$collectionName (${documents.size} results)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            documents.take(3).forEach { document ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp)
                                    ) {
                                        document.entries.take(5).forEach { (field, value) ->
                                            Text(
                                                text = "$field: $value",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        if (document.size > 5) {
                                            Text(
                                                text = "... and ${document.size - 5} more fields",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            
                            if (documents.size > 3) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "... and ${documents.size - 3} more results",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsTab(
    analytics: Map<String, Any>?,
    onLoadAnalytics: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Button(
            onClick = onLoadAnalytics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Generate Analytics")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        analytics?.let { data ->
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(data.entries.toList()) { (category, categoryData) ->
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = category.replaceFirstChar { it.uppercase() }.replace("_", " "),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            when (categoryData) {
                                is Map<*, *> -> {
                                    categoryData.entries.forEach { (key, value) ->
                                        when (value) {
                                            is Number -> {
                                                Text(
                                                    text = "• $key: ${String.format("%.2f", value.toDouble())}",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            is List<*> -> {
                                                Text(
                                                    text = "• $key: [${value.size} items]",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            else -> {
                                                Text(
                                                    text = "• $key: $value",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                                else -> {
                                    Text(
                                        text = categoryData.toString(),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionsTab(
    databaseExplorer: DatabaseExplorer,
    onStatusUpdate: (String) -> Unit,
    onLoadingUpdate: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Database Actions",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = "These actions will output detailed information to the logs. Check Logcat with tag 'DatabaseExplorer'.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Running health check...")
                    try {
                        databaseExplorer.runHealthCheck()
                        onStatusUpdate("Health check completed - check logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Health check failed: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Run Health Check")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing database overview...")
                    try {
                        databaseExplorer.printDatabaseOverview()
                        onStatusUpdate("Overview printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print overview: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print Database Overview")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing all students...")
                    try {
                        databaseExplorer.printAllDocuments(
                            EducationalContentRepository.DatabaseCollectionType.STUDENTS
                        )
                        onStatusUpdate("Students printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print students: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print All Students")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing all educators...")
                    try {
                        databaseExplorer.printAllDocuments(
                            EducationalContentRepository.DatabaseCollectionType.EDUCATORS
                        )
                        onStatusUpdate("Educators printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print educators: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print All Educators")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing all classes...")
                    try {
                        databaseExplorer.printAllDocuments(
                            EducationalContentRepository.DatabaseCollectionType.CLASSES
                        )
                        onStatusUpdate("Classes printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print classes: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print All Classes")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing all klyps...")
                    try {
                        databaseExplorer.printAllDocuments(
                            EducationalContentRepository.DatabaseCollectionType.KLYPS
                        )
                        onStatusUpdate("Klyps printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print klyps: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print All Klyps")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Printing analytics...")
                    try {
                        databaseExplorer.printDatabaseAnalytics()
                        onStatusUpdate("Analytics printed to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to print analytics: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Print Database Analytics")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Exporting database...")
                    try {
                        databaseExplorer.exportDatabaseToLogs()
                        onStatusUpdate("Database exported to logs")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to export database: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export Database to Logs")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Finding incomplete students...")
                    try {
                        databaseExplorer.findIncompleteStudents()
                        onStatusUpdate("Incomplete students check completed")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to check incomplete students: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find Incomplete Students")
        }
        
        Button(
            onClick = {
                scope.launch {
                    onLoadingUpdate(true)
                    onStatusUpdate("Finding orphaned references...")
                    try {
                        databaseExplorer.findOrphanedReferences()
                        onStatusUpdate("Orphaned references check completed")
                    } catch (e: Exception) {
                        onStatusUpdate("Failed to check orphaned references: ${e.message}")
                    } finally {
                        onLoadingUpdate(false)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Find Orphaned References")
        }
    }
}
