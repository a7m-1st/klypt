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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.klypt.data.models.Achievement
import com.klypt.data.models.Badge
import com.klypt.data.models.AchievementCategory
import com.klypt.data.models.BadgeRarity
import kotlinx.coroutines.delay

/**
 * Achievement unlocked notification that appears when user earns a new achievement
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementNotification(
    achievement: Achievement?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    achievement?.let { ach ->
        Dialog(onDismissRequest = onDismiss) {
            var isVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(ach) {
                isVisible = true
                delay(3000) // Auto dismiss after 3 seconds
                onDismiss()
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = slideOutVertically(
                    targetOffsetY = { -it }
                ) + fadeOut()
            ) {
                Card(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Sparkle animation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            repeat(3) {
                                var sparkleVisible by remember { mutableStateOf(false) }
                                LaunchedEffect(ach) {
                                    delay(it * 200L)
                                    sparkleVisible = true
                                }
                                AnimatedVisibility(
                                    visible = sparkleVisible,
                                    enter = scaleIn() + fadeIn()
                                ) {
                                    Text(
                                        text = "✨",
                                        style = MaterialTheme.typography.headlineMedium
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = "Achievement Unlocked!",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // Achievement icon
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500)
                                        )
                                    ),
                                    shape = RoundedCornerShape(30.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (ach.category) {
                                    AchievementCategory.QUIZ -> Icons.Default.Quiz
                                    AchievementCategory.STREAK -> Icons.Default.LocalFireDepartment
                                    AchievementCategory.STUDY_TIME -> Icons.Default.AccessTime
                                    AchievementCategory.EXPLORATION -> Icons.Default.Explore
                                    AchievementCategory.GENERAL -> Icons.Default.Star
                                },
                                contentDescription = ach.title,
                                modifier = Modifier.size(32.dp),
                                tint = Color.White
                            )
                        }
                        
                        Text(
                            text = ach.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = ach.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        // Reward if available
                        ach.reward?.let { reward ->
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                )
                            ) {
                                Text(
                                    text = "Reward: $reward",
                                    modifier = Modifier.padding(12.dp),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Badge earned notification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeNotification(
    badge: Badge?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    badge?.let { b ->
        Dialog(onDismissRequest = onDismiss) {
            var isVisible by remember { mutableStateOf(false) }
            
            LaunchedEffect(b) {
                isVisible = true
                delay(2500)
                onDismiss()
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                ) + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = modifier
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getBadgeColor(b.rarity).copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Badge Earned!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getBadgeColor(b.rarity)
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(
                                    color = getBadgeColor(b.rarity).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(25.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = b.icon,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        
                        Text(
                            text = b.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = b.rarity.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = getBadgeColor(b.rarity),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Level up notification
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LevelUpNotification(
    newLevel: Int?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    newLevel?.let { level ->
        Dialog(onDismissRequest = onDismiss) {
            var isVisible by remember { mutableStateOf(false) }
            var showFireworks by remember { mutableStateOf(false) }
            
            LaunchedEffect(level) {
                isVisible = true
                delay(500)
                showFireworks = true
                delay(3000)
                onDismiss()
            }
            
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Card(
                    modifier = modifier
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Fireworks animation
                        AnimatedVisibility(
                            visible = showFireworks,
                            enter = fadeIn() + scaleIn()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("🎆", "🎉", "🎊", "🌟", "✨").forEach { emoji ->
                                    var bouncing by remember { mutableStateOf(false) }
                                    LaunchedEffect(showFireworks) {
                                        while (showFireworks) {
                                            bouncing = !bouncing
                                            delay(300)
                                        }
                                    }
                                    
                                    AnimatedVisibility(
                                        visible = bouncing,
                                        enter = slideInVertically { -it } + fadeIn(),
                                        exit = slideOutVertically { -it } + fadeOut()
                                    ) {
                                        Text(
                                            text = emoji,
                                            style = MaterialTheme.typography.headlineSmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        Text(
                            text = "LEVEL UP!",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        // New level badge
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFFD700),
                                            Color(0xFFFFA500),
                                            Color(0xFFFF6B35)
                                        )
                                    ),
                                    shape = RoundedCornerShape(40.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = level.toString(),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                        
                        Text(
                            text = "You reached level $level!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Text(
                            text = "Keep up the amazing work! 💪",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private fun getBadgeColor(rarity: BadgeRarity): Color = when (rarity) {
    BadgeRarity.COMMON -> Color(0xFF9E9E9E)
    BadgeRarity.RARE -> Color(0xFF4CAF50)
    BadgeRarity.EPIC -> Color(0xFF9C27B0)
    BadgeRarity.LEGENDARY -> Color(0xFFFFD700)
}
