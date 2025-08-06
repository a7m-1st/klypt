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

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.klypt.R
import com.klypt.data.models.ClassDocument
import com.klypt.ui.home.ClassCard

/** Navigation destination data */
object ViewAllClassesDestination {
    val route = "ViewAllClassesRoute"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewAllClassesScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddClass: () -> Unit,
    onClassClick: (ClassDocument) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ViewAllClassesViewModel = hiltViewModel(),
    exportViewModel: ClassExportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportUiState by exportViewModel.uiState.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<ClassDocument?>(null) }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Show export messages
    LaunchedEffect(exportUiState.successMessage) {
        exportUiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            exportViewModel.clearMessages()
        }
    }
    
    LaunchedEffect(exportUiState.errorMessage) {
        exportUiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar("Error: $message")
            exportViewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("All Classes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddClass,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Class")
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.classes.isEmpty()) {
                        item {
                            EmptyStateCard(
                                message = "No classes available",
                                actionText = "Add Class",
                                onActionClick = onNavigateToAddClass
                            )
                        }
                    } else {
                        items(uiState.classes) { classDoc ->
                            ClassCard(
                                classDocument = classDoc,
                                onClick = { onClassClick(classDoc) },
                                cardColor = MaterialTheme.colorScheme.surfaceVariant,
                                showMenu = true,
                                onDelete = { showDeleteDialog = classDoc },
                                onExport = { 
                                    exportViewModel.exportClassToJson(
                                        context = context,
                                        classCode = classDoc.classCode,
                                        className = classDoc.classTitle
                                    )
                                }
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

    // Delete confirmation dialog
    showDeleteDialog?.let { classDoc ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("Delete Class") },
            text = { Text("Are you sure you want to delete \"${classDoc.classTitle}\"? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteClass(classDoc)
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

    // Share dialog after successful export
    if (exportUiState.showShareDialog) {
        AlertDialog(
            onDismissRequest = { exportViewModel.dismissShareDialog() },
            title = { Text("Export Successful") },
            text = { Text("Your class has been exported to JSON format. Would you like to share the file?") },
            confirmButton = {
                TextButton(
                    onClick = { 
                        exportViewModel.shareExportedFile(context)
                    }
                ) {
                    Text("Share")
                }
            },
            dismissButton = {
                TextButton(onClick = { exportViewModel.dismissShareDialog() }) {
                    Text("Done")
                }
            }
        )
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
