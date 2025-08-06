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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klypt.data.models.*
import kotlin.math.min

/**
 * Gamified dashboard showing user's learning progress, achievements, and stats
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameifiedDashboard(
    gameStats: GameStats,
    quizStats: QuizStats,
    learningProgress: List<LearningProgress>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Level and XP Header
        PlayerLevelCard(gameStats = gameStats)
        
        // Quick Stats Row
        QuickGameStats(gameStats = gameStats, quizStats = quizStats)
        
        // Weekly Goal Progress
        WeeklyGoalCard(gameStats = gameStats)
        
        // Recent Achievements
        if (gameStats.achievements.isNotEmpty()) {
            AchievementsSection(achievements = gameStats.achievements.take(3))
        }
        
        // Quiz Performance Chart
        QuizPerformanceCard(quizStats = quizStats)
        
        // Subject Mastery
        if (quizStats.subjectBreakdown.isNotEmpty()) {
            SubjectMasterySection(subjectStats = quizStats.subjectBreakdown)
        }
        
        // Learning Streaks
        StreakCard(gameStats = gameStats)
        
        // Recent Badges
        if (gameStats.badges.isNotEmpty()) {
            BadgesSection(badges = gameStats.badges.take(5))
        }
        
        // Motivational Quote
        MotivationalQuoteCard(gameStats = gameStats)
        
        // Daily Challenge
        DailyChallengeCard(
            gameStats = gameStats,
            onChallengeClick = { /* Handle challenge click */ }
        )
        
        // Study Streak Visualization
        StudyStreakVisualization(gameStats = gameStats)
    }
}

/**
 * Player level and experience points card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerLevelCard(gameStats: GameStats) {
    val xpForNextLevel = (gameStats.level * 100) // Simple XP calculation
    val currentLevelXP = gameStats.experiencePoints % 100
    val progress = currentLevelXP.toFloat() / 100f
    
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutQuart),
        label = "xp_progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Level Badge
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primaryContainer
                            )
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gameStats.level.toString(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
            
            // XP Progress
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Level ${gameStats.level}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${gameStats.experiencePoints} XP",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${currentLevelXP}/100 XP to level ${gameStats.level + 1}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Quick game statistics row
 */
@Composable
internal fun QuickGameStats(gameStats: GameStats, quizStats: QuizStats) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 4.dp)
    ) {
        item {
            StatCard(
                title = "Quiz Streak",
                value = gameStats.currentStreak.toString(),
                icon = Icons.Default.LocalFireDepartment,
                color = Color(0xFFFF6B35)
            )
        }
        item {
            StatCard(
                title = "Avg Score",
                value = "${(quizStats.averageScore * 100).toInt()}%",
                icon = Icons.Default.TrendingUp,
                color = Color(0xFF4CAF50)
            )
        }
        item {
            StatCard(
                title = "Perfect Scores",
                value = quizStats.perfectScores.toString(),
                icon = Icons.Default.Stars,
                color = Color(0xFFFFD700)
            )
        }
        item {
            StatCard(
                title = "Study Time",
                value = "${gameStats.studyTimeMinutes}min",
                icon = Icons.Default.AccessTime,
                color = Color(0xFF2196F3)
            )
        }
    }
}

/**
 * Individual stat card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Weekly goal progress card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WeeklyGoalCard(gameStats: GameStats) {
    val progress = min(gameStats.weeklyProgress.toFloat() / gameStats.weeklyGoal.toFloat(), 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = EaseOutQuart),
        label = "weekly_progress"
    )
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Flag,
                        contentDescription = "Weekly Goal",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Weekly Goal",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${gameStats.weeklyProgress}/${gameStats.weeklyGoal}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.tertiary
            )
            
            val remainingQuizzes = gameStats.weeklyGoal - gameStats.weeklyProgress
            if (remainingQuizzes > 0) {
                Text(
                    text = "$remainingQuizzes more quizzes to reach your weekly goal! 💪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            } else {
                Text(
                    text = "🎉 Weekly goal achieved! Keep up the great work!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Achievements section
 */
@Composable
internal fun AchievementsSection(achievements: List<Achievement>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Achievements",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Recent Achievements",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(achievements) { achievement ->
                AchievementCard(achievement = achievement)
            }
        }
    }
}

/**
 * Individual achievement card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementCard(achievement: Achievement) {
    val backgroundColor = if (achievement.isCompleted) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    
    Card(
        modifier = Modifier.width(160.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Achievement icon placeholder - you can replace with actual icons
            Icon(
                imageVector = when (achievement.category) {
                    AchievementCategory.QUIZ -> Icons.Default.Quiz
                    AchievementCategory.STREAK -> Icons.Default.LocalFireDepartment
                    AchievementCategory.STUDY_TIME -> Icons.Default.AccessTime
                    AchievementCategory.EXPLORATION -> Icons.Default.Explore
                    AchievementCategory.GENERAL -> Icons.Default.Star
                },
                contentDescription = achievement.title,
                modifier = Modifier.size(32.dp),
                tint = if (achievement.isCompleted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Text(
                text = achievement.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            
            if (!achievement.isCompleted && achievement.target > 1) {
                LinearProgressIndicator(
                    progress = { achievement.progress.toFloat() / achievement.target.toFloat() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
                Text(
                    text = "${achievement.progress}/${achievement.target}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Quiz performance chart card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuizPerformanceCard(quizStats: QuizStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Analytics,
                    contentDescription = "Performance",
                    tint = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Quiz Performance",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                PerformanceMetric(
                    label = "Total",
                    value = quizStats.totalAttempts.toString(),
                    color = MaterialTheme.colorScheme.secondary
                )
                PerformanceMetric(
                    label = "Average",
                    value = "${(quizStats.averageScore * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.secondary
                )
                PerformanceMetric(
                    label = "Best",
                    value = "${(quizStats.bestScore * 100).toInt()}%",
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            // Simple performance trend indicator
            if (quizStats.recentScores.isNotEmpty()) {
                val trend = if (quizStats.improvementRate > 0) "↗️ Improving" 
                           else if (quizStats.improvementRate < 0) "↘️ Needs focus"
                           else "→ Stable"
                
                Text(
                    text = "Trend: $trend",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}

@Composable
private fun PerformanceMetric(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/**
 * Subject mastery section
 */
@Composable
internal fun SubjectMasterySection(subjectStats: Map<String, SubjectStats>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = "Subject Mastery",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Subject Mastery",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(subjectStats.toList()) { (_, stats) ->
                SubjectMasteryCard(subjectStats = stats)
            }
        }
    }
}

/**
 * Individual subject mastery card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SubjectMasteryCard(subjectStats: SubjectStats) {
    val masteryColor = when (subjectStats.mastery) {
        MasteryLevel.BEGINNER -> Color(0xFF9E9E9E)
        MasteryLevel.INTERMEDIATE -> Color(0xFF4CAF50)
        MasteryLevel.ADVANCED -> Color(0xFF2196F3)
        MasteryLevel.EXPERT -> Color(0xFF9C27B0)
        MasteryLevel.MASTER -> Color(0xFFFFD700)
    }
    
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = masteryColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = subjectStats.subject,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            
            Text(
                text = subjectStats.mastery.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = masteryColor,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "${(subjectStats.averageScore * 100).toInt()}% avg",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Text(
                text = "${subjectStats.totalQuizzes} quizzes",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Streak card
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StreakCard(gameStats: GameStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Current Streak",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Best: ${gameStats.longestStreak} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Text(
                text = "${gameStats.currentStreak} days",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6B35)
            )
        }
    }
}

/**
 * Badges section
 */
@Composable
internal fun BadgesSection(badges: List<Badge>) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.MilitaryTech,
                contentDescription = "Badges",
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Recent Badges",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(badges) { badge ->
                BadgeItem(badge = badge)
            }
        }
    }
}

/**
 * Individual badge item
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BadgeItem(badge: Badge) {
    val badgeColor = when (badge.rarity) {
        BadgeRarity.COMMON -> Color(0xFF9E9E9E)
        BadgeRarity.RARE -> Color(0xFF4CAF50)
        BadgeRarity.EPIC -> Color(0xFF9C27B0)
        BadgeRarity.LEGENDARY -> Color(0xFFFFD700)
    }
    
    Card(
        modifier = Modifier.size(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = badgeColor.copy(alpha = 0.1f)
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Badge icon placeholder
                Icon(
                    imageVector = Icons.Default.MilitaryTech,
                    contentDescription = badge.name,
                    modifier = Modifier.size(24.dp),
                    tint = badgeColor
                )
                Text(
                    text = badge.name.take(8),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    fontSize = 8.sp
                )
            }
        }
    }
}
