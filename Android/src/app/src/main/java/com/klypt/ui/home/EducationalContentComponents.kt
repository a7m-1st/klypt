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

package com.klypt.ui.home

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klypt.data.UserRole
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Student

/**
 * Quick stats cards for the dashboard
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickStatsSection(
    stats: Map<String, Any>,
    userRole: UserRole,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        when (userRole) {
            UserRole.STUDENT -> {
                item {
                    StatsCard(
                        title = "My Classes",
                        value = (stats["totalClasses"] as? Int) ?: 0,
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    StatsCard(
                        title = "Recent Klyps",
                        value = (stats["totalKlyps"] as? Int) ?: 0,
                        icon = Icons.Default.Assignment,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                item {
                    StatsCard(
                        title = "Assignments",
                        value = (stats["averageStudentsPerClass"] as? Int) ?: 0,
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            UserRole.EDUCATOR -> {
                item {
                    StatsCard(
                        title = "My Classes",
                        value = (stats["totalClasses"] as? Int) ?: 0,
                        icon = Icons.Default.School,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                item {
                    StatsCard(
                        title = "Total Students",
                        value = (stats["totalStudents"] as? Int) ?: 0,
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                item {
                    StatsCard(
                        title = "Klyps Created",
                        value = (stats["totalKlyps"] as? Int) ?: 0,
                        icon = Icons.Default.Create,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatsCard(
    title: String,
    value: Int,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(140.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Recent Klyps section
 */
@Composable
fun RecentKlypsSection(
    klyps: List<Klyp>,
    onKlypClick: (Klyp) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Learning Content",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            TextButton(onClick = { /* Navigate to all Klyps */ }) {
                Text("View All")
            }
        }
        
        if (klyps.isEmpty()) {
            EmptyStateCard(
                message = "No learning content available",
                icon = Icons.Default.Assignment
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(klyps) { klyp ->
                    KlypCard(
                        klyp = klyp,
                        onClick = { onKlypClick(klyp) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun KlypCard(
    klyp: Klyp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = klyp.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = klyp.classCode,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "${klyp.questions.size}Q",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = klyp.mainBody,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * My Classes section
 */
@Composable
fun MyClassesSection(
    classes: List<ClassDocument>,
    onClassClick: (ClassDocument) -> Unit,
    onAddNewClassClick: () -> Unit,
    onViewAllClick: (() -> Unit)? = null,
    onExportClass: ((ClassDocument) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Log.d("MyClassesSection", "classes: $classes")
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Classes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            if (onViewAllClick != null) {
                TextButton(onClick = onViewAllClick) {
                    Text("View All")
                }
            }
        }
        
        if (classes.isEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                item {
                    AddNewClassCard(
                        onClick = onAddNewClassClick
                    )
                }
                item {
                    EmptyClassStateCard()
                }
            }
        } else {
            val classColors = listOf(
                Color(0xFFB3E5FC), // Light Blue
                Color(0xFFFFF9C4), // Light Yellow
                Color(0xFFC8E6C9), // Light Green
                Color(0xFFFFCCBC), // Light Orange
                Color(0xFFD1C4E9), // Light Purple
                Color(0xFFFFF176), // Yellow
                Color(0xFFFFAB91)  // Orange
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                itemsIndexed(classes) { idx, classDoc ->
                    ClassCard(
                        classDocument = classDoc,
                        onClick = { onClassClick(classDoc) },
                        cardColor = classColors[idx % classColors.size],
                        showMenu = true,
                        onExport = if (onExportClass != null) {
                            { onExportClass(classDoc) }
                        } else null
                    )
                }
                // Add Class box at the end
                item {
                    AddClassBox(onClick = onAddNewClassClick)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassCard(
    classDocument: ClassDocument,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = MaterialTheme.colorScheme.surface,
    showMenu: Boolean = false,
    onDelete: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Box(Modifier.fillMaxSize()) {
            val classTextColor = Color(0xCC000000) // Slightly lighter for readability
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = classDocument.classCode,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp
                        ),
                        color = classTextColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = classDocument.classTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.1.sp
                        ),
                        color = classTextColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${classDocument.studentIds.size} students",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.1.sp
                        ),
                        color = classTextColor
                    )
                }
            }
            if (showMenu) {
                Box(Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { menuExpanded = true },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .padding(bottom = 12.dp) // Move up from the bottom
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = Color.Black
                        )
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        modifier = Modifier.width(160.dp)
                    ) {
                        if (onExport != null) {
                            DropdownMenuItem(
                                text = { Text("Export to JSON") },
                                onClick = {
                                    menuExpanded = false
                                    onExport.invoke()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = { Text("Remove Class") },
                            onClick = {
                                menuExpanded = false
                                onDelete?.invoke()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddClassBox(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Class",
                modifier = Modifier.size(48.dp),
                tint = Color(0xFFB0B0B0)
            )
        }
    }
}

/**
 * Upcoming assignments section (for students)
 */
@Composable
fun UpcomingAssignmentsSection(
    assignments: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Upcoming Assignments",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        if (assignments.isEmpty()) {
            EmptyStateCard(
                message = "No upcoming assignments",
                icon = Icons.Default.AssignmentTurnedIn
            )
        } else {
            LazyColumn(
                modifier = Modifier.height(200.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(assignments) { (className, assignment) ->
                    AssignmentCard(
                        className = className,
                        assignment = assignment
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignmentCard(
    className: String,
    assignment: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Assignment,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = className,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = assignment,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * User welcome section
 */
@Composable
fun UserWelcomeSection(
    currentUser: Any?,
    userRole: UserRole,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        when (userRole) {
            UserRole.STUDENT -> {
                val student = currentUser as? Student
                if (student != null) {
                    Text(
                        text = "Welcome back, ${student.firstName}!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Ready to continue learning?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            UserRole.EDUCATOR -> {
                val educator = currentUser as? Educator
                if (educator != null) {
                    Text(
                        text = "Hello, ${educator.fullName.split(" ").firstOrNull() ?: "Educator"}!",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Manage your classes and create engaging content",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Empty state card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyStateCard(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Add new class card with + button
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddNewClassCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = BorderStroke(
            2.dp, 
            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add new class",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "New Class",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Empty class state card for carousel
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyClassStateCard(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(200.dp)
            .height(120.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No classes yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
