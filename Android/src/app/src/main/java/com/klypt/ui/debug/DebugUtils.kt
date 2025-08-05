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

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.klypt.BuildConfig

/**
 * Debug utilities for development builds
 * Only shows in debug builds
 */
object DebugUtils {
    
    /**
     * Launch the Database Explorer Activity
     */
    fun launchDatabaseExplorer(context: Context) {
        if (BuildConfig.DEBUG) {
            val intent = Intent(context, DatabaseExplorerActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

/**
 * Floating debug button that can be added to any screen
 * Only visible in debug builds
 */
@Composable
fun DebugFloatingButton(
    modifier: Modifier = Modifier
) {
    if (BuildConfig.DEBUG) {
        val context = LocalContext.current
        var showMenu by remember { mutableStateOf(false) }
        
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomEnd
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                // Debug menu items
                if (showMenu) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Button(
                                onClick = {
                                    DebugUtils.launchDatabaseExplorer(context)
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Database Explorer")
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Button(
                                onClick = {
                                    // Add more debug actions here
                                    showMenu = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Cache")
                            }
                        }
                    }
                }
                
                // Main debug button
                FloatingActionButton(
                    onClick = { showMenu = !showMenu },
                    modifier = Modifier.padding(16.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.secondary
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "Debug Menu"
                    )
                }
            }
        }
    }
}

/**
 * Simple debug button for toolbars/app bars
 */
@Composable
fun DebugMenuButton() {
    if (BuildConfig.DEBUG) {
        val context = LocalContext.current
        var showDropdown by remember { mutableStateOf(false) }
        
        Box {
            IconButton(onClick = { showDropdown = true }) {
                Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Debug Menu"
                )
            }
            
            DropdownMenu(
                expanded = showDropdown,
                onDismissRequest = { showDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Database Explorer") },
                    onClick = {
                        DebugUtils.launchDatabaseExplorer(context)
                        showDropdown = false
                    }
                )
                
                DropdownMenuItem(
                    text = { Text("Clear Cache") },
                    onClick = {
                        // Add cache clearing logic here
                        showDropdown = false
                    }
                )
            }
        }
    }
}
