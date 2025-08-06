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

package com.klypt.ui.classes

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Klyp
import com.klypt.data.services.UserContextProvider

/** Navigation destination data */
object ClassDetailsDestination {
    val route = "ClassDetailsRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsScreen(
    classDocument: ClassDocument? = null,
    classId: String? = null,
    onNavigateBack: () -> Unit,
    onNavigateToAddKlyp: (String) -> Unit = {}, // Pass class code for klyp creation
    onNavigateToKlypDetails: (Klyp) -> Unit = {}, // Navigate to klyp details
    modifier: Modifier = Modifier,
    viewModel: ClassDetailsViewModel = hiltViewModel(),
    userContextProvider: UserContextProvider
) {
    val uiState by viewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<Klyp?>(null) }
    var showAddKlypDialog by remember { mutableStateOf(false) }

    // Initialize the view model with the class data or ID
    LaunchedEffect(classDocument, classId) {
        when {
            classDocument != null -> viewModel.initializeWithClass(classDocument)
            classId != null -> viewModel.initializeWithClassId(classId)
        }
    }

    // Use the class document from UI state if available, otherwise use the passed one
    val currentClassDocument = uiState.classDocument ?: classDocument

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { 
                    currentClassDocument?.let { classDoc ->
                        Column {
                            Text(
                                text = classDoc.classTitle,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = classDoc.classCode,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } ?: run {
                        Text("Loading...")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentClassDocument != null) {
                FloatingActionButton(
                    onClick = { showAddKlypDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Klyp")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Class info section
            currentClassDocument?.let { classDoc ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Class Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Students: ${classDoc.studentIds.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Klyps: ${uiState.klyps.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Klyps section
            Text(
                text = "Educational Content (Klyps)",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val userId = userContextProvider.getCurrentUserId()
                    val ownsKlyp:Boolean = currentClassDocument?.educatorId == userId
                    Log.d("ClassDetailsScreen##", userId)
                    if (uiState.klyps.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No educational content available for this class",
                                actionText = if(ownsKlyp) "Add Klyp" else "Add Klyp (Not Owned, so won't be synced)",
                                onActionClick = { showAddKlypDialog = true }
                            )
                        }
                    } else {
                        Log.d("ClassDetailsScreen##", "Klyps: ${uiState.klyps.size}")
                        items(uiState.klyps) { klyp ->
                            KlypCard(
                                klyp = klyp,
                                onDelete = { showDeleteDialog = klyp },
                                onClick = { onNavigateToKlypDetails(klyp) }
                            )
                        }

                        item {
                            EmptyStateCard(
                                message = "Add your own Klyps to this Class",
                                actionText = if(ownsKlyp) "Add Klyp" else "Add Klyp (Not Owned, so won't be synced)",
                                onActionClick = { showAddKlypDialog = true }
                            )
                        }
                    }
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }

    // Delete klyp confirmation dialog
    showDeleteDialog?.let { klyp ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Klyp") },
            text = { Text("Are you sure you want to delete \"${klyp.title}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteKlyp(klyp)
                        showDeleteDialog = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Klyp dialog - navigate to LLM Chat for klyp creation
    if (showAddKlypDialog && currentClassDocument != null) {
        AlertDialog(
            onDismissRequest = { showAddKlypDialog = false },
            title = { Text("Add Klyp") },
            text = { 
                Column {
                    Text("Create new educational content for this class using AI assistance.")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Class: ${currentClassDocument.classTitle} (${currentClassDocument.classCode})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showAddKlypDialog = false
                        // Navigate to LLM Chat for klyp creation with class context
                        onNavigateToAddKlyp(currentClassDocument.classCode)
                    }
                ) {
                    Text("Create with AI")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddKlypDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KlypCard(
    klyp: Klyp,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = klyp.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = klyp.mainBody,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                //TODO(): Add delete button
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Klyp",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            if (klyp.questions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${klyp.questions.size} question(s) included",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(
    message: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (actionText != null && onActionClick != null) {
                Button(onClick = onActionClick) {
                    Text(actionText)
                }
            }
        }
    }
}
