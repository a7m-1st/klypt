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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.klypt.data.models.GameStats
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Motivational quote card that changes based on user's performance and progress
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun MotivationalQuoteCard(
    gameStats: GameStats,
    modifier: Modifier = Modifier
) {
    var currentQuoteIndex by remember { mutableStateOf(0) }
    val quotes = getMotivationalQuotes(gameStats)
    
    // Auto-rotate quotes every 10 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(10000)
            currentQuoteIndex = (currentQuoteIndex + 1) % quotes.size
        }
    }
    
    AnimatedContent(
        targetState = currentQuoteIndex,
        transitionSpec = {
            slideInHorizontally { width -> width } + fadeIn() with
            slideOutHorizontally { width -> -width } + fadeOut()
        },
        label = "quote_transition"
    ) { index ->
        if (quotes.isNotEmpty()) {
            val quote = quotes[index]
            Card(
                modifier = modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = quote.color.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = quote.icon,
                        contentDescription = null,
                        tint = quote.color,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = quote.text,
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        quote.author?.let { author ->
                            Text(
                                text = "- $author",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Daily challenge card to encourage engagement
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyChallengeCard(
    gameStats: GameStats,
    onChallengeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val challenge = getDailyChallenge(gameStats)
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        onClick = onChallengeClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "Daily Challenge",
                    tint = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "Can you do it?",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Text(
                text = challenge.title,
                style = MaterialTheme.typography.bodyLarge
            )
            
            Text(
                text = challenge.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Progress if applicable
            if (challenge.progress > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Progress: ${challenge.progress}/${challenge.target}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    
                    LinearProgressIndicator(
                        progress = { challenge.progress.toFloat() / challenge.target.toFloat() },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            
            // Reward
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Reward",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Reward: ${challenge.reward} XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFFFD700)
                )
            }
        }
    }
}

/**
 * Study streak visualization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyStreakVisualization(
    gameStats: GameStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFF6B35).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalFireDepartment,
                    contentDescription = "Streak",
                    tint = Color(0xFFFF6B35),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = "Study Streak",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Flame visualization based on streak length
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(minOf(gameStats.currentStreak, 7)) { index ->
                    var flameVisible by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(gameStats.currentStreak) {
                        delay(index * 100L)
                        flameVisible = true
                    }
                    
                    AnimatedVisibility(
                        visible = flameVisible,
                        enter = scaleIn(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        ) + fadeIn()
                    ) {
                        Text(
                            text = "🔥",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
            }
            
            Text(
                text = "${gameStats.currentStreak} day${if (gameStats.currentStreak != 1) "s" else ""}",
                style = MaterialTheme.typography.titleLarge,
                color = Color(0xFFFF6B35)
            )
            
            val encouragement = when {
                gameStats.currentStreak >= 30 -> "Legendary dedication! 🏆"
                gameStats.currentStreak >= 14 -> "Two weeks strong! Amazing! 💪"
                gameStats.currentStreak >= 7 -> "One week streak! Keep it up! 🎯"
                gameStats.currentStreak >= 3 -> "Building momentum! 🚀"
                gameStats.currentStreak >= 1 -> "Great start! 🌟"
                else -> "Start your streak today! ⚡"
            }
            
            Text(
                text = encouragement,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// Data classes for quotes and challenges
private data class MotivationalQuote(
    val text: String,
    val author: String? = null,
    val icon: ImageVector = Icons.Default.FormatQuote,
    val color: Color = Color(0xFF4CAF50)
)

private data class DailyChallenge(
    val title: String,
    val description: String,
    val progress: Int = 0,
    val target: Int = 1,
    val reward: Int = 50
)

private fun getMotivationalQuotes(gameStats: GameStats): List<MotivationalQuote> {
    val allQuotes = listOf(
        // High performance quotes
        MotivationalQuote(
            "Excellence is not a skill, it's an attitude.",
            "Ralph Marston",
            Icons.Default.Star,
            Color(0xFFFFD700)
        ),
        MotivationalQuote(
            "Success is the sum of small efforts repeated day in and day out.",
            "Robert Collier",
            Icons.Default.TrendingUp,
            Color(0xFF4CAF50)
        ),
        
        // Learning quotes
        MotivationalQuote(
            "The beautiful thing about learning is that no one can take it away from you.",
            "B.B. King",
            Icons.Default.School,
            Color(0xFF2196F3)
        ),
        MotivationalQuote(
            "Education is the most powerful weapon which you can use to change the world.",
            "Nelson Mandela",
            Icons.Default.Public,
            Color(0xFF9C27B0)
        ),
        
        // Encouragement quotes
        MotivationalQuote(
            "Believe you can and you're halfway there.",
            "Theodore Roosevelt",
            Icons.Default.Favorite,
            Color(0xFFE91E63)
        ),
        MotivationalQuote(
            "The only way to do great work is to love what you do.",
            "Steve Jobs",
            Icons.Default.Work,
            Color(0xFFFF9800)
        ),
        
        // Progress quotes
        MotivationalQuote(
            "Progress, not perfection, is the goal.",
            null,
            Icons.Default.Timeline,
            Color(0xFF00BCD4)
        ),
        MotivationalQuote(
            "Every expert was once a beginner.",
            null,
            Icons.Default.EmojiEvents,
            Color(0xFF795548)
        )
    )
    
    // Return personalized quotes based on performance
    return when {
        gameStats.averageQuizScore >= 0.9 -> allQuotes.filter { 
            it.author in listOf("Ralph Marston", "Steve Jobs") 
        }
        gameStats.currentStreak >= 7 -> allQuotes.filter {
            it.author in listOf("Robert Collier", "Theodore Roosevelt")
        }
        gameStats.totalQuizzesCompleted >= 20 -> allQuotes.filter {
            it.author in listOf("B.B. King", "Nelson Mandela")
        }
        else -> allQuotes.shuffled().take(4)
    }
}

private fun getDailyChallenge(gameStats: GameStats): DailyChallenge {
    val challenges = listOf(
        DailyChallenge(
            title = "Quiz Master",
            description = "Complete 3 quizzes today",
            progress = gameStats.weeklyProgress % 3,
            target = 3,
            reward = 75
        ),
        DailyChallenge(
            title = "Perfect Score",
            description = "Get 100% on any quiz",
            progress = 0,
            target = 1,
            reward = 100
        ),
        DailyChallenge(
            title = "Study Session",
            description = "Study for 30 minutes",
            progress = minOf(gameStats.studyTimeMinutes % 30, 30),
            target = 30,
            reward = 50
        ),
        DailyChallenge(
            title = "Streak Builder",
            description = "Maintain your study streak",
            progress = if (gameStats.currentStreak > 0) 1 else 0,
            target = 1,
            reward = 25
        )
    )
    
    // Return a challenge based on current stats
    return challenges.random()
}
