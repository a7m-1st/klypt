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

package com.klypt.data.repositories

import android.util.Log
import com.klypt.data.DatabaseManager
import com.klypt.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.min

@Singleton
class GameStatsRepository @Inject constructor(
    private val databaseManager: DatabaseManager,
    private val quizAttemptRepository: QuizAttemptRepository
) {
    
    companion object {
        private const val TAG = "GameStatsRepository"
        private const val GAME_STATS_TYPE = "game_stats"
        private const val XP_PER_QUIZ_COMPLETION = 10
        private const val XP_PER_PERFECT_SCORE = 25
        private const val XP_PER_IMPROVEMENT = 5
    }
    
    /**
     * Get comprehensive game statistics for a student
     */
    suspend fun getGameStats(studentId: String): GameStats = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching game stats for student: $studentId")
            
            val quizAttempts = quizAttemptRepository.getAttemptsByStudentId(studentId)
            Log.d(TAG, "Found ${quizAttempts.size} quiz attempts")
            
            // If no quiz attempts, return initial empty stats for new user
            if (quizAttempts.isEmpty()) {
                return@withContext generateInitialGameStats()
            }
            
            // Calculate basic quiz stats
            val completedQuizzes = quizAttempts.filter { 
                it.containsKey("isSubmitted") && it["isSubmitted"] as Boolean 
            }
            
            val totalQuizzesCompleted = completedQuizzes.size
            val averageScore = if (completedQuizzes.isNotEmpty()) {
                completedQuizzes.mapNotNull { it["score"] as? Double }.average()
            } else 0.0
            
            // Calculate streaks
            val (currentStreak, longestStreak) = calculateStreaks(completedQuizzes)
            
            // Calculate experience points
            val experiencePoints = calculateExperiencePoints(completedQuizzes)
            val level = (experiencePoints / 100) + 1
            
            // Get achievements and badges
            val achievements = generateAchievements(studentId, completedQuizzes)
            val badges = generateBadges(completedQuizzes, achievements)
            
            // Weekly progress
            val weeklyProgress = calculateWeeklyProgress(completedQuizzes)
            
            // Study time calculation (mock data for now)
            val studyTimeMinutes = totalQuizzesCompleted * 15 // Assume 15 min per quiz
            
            GameStats(
                totalQuizzesCompleted = totalQuizzesCompleted,
                averageQuizScore = averageScore,
                totalCorrectAnswers = calculateTotalCorrectAnswers(completedQuizzes),
                totalQuestions = calculateTotalQuestions(completedQuizzes),
                currentStreak = currentStreak,
                longestStreak = longestStreak,
                experiencePoints = experiencePoints,
                level = level,
                badges = badges,
                achievements = achievements,
                weeklyGoal = 5, // Default weekly goal
                weeklyProgress = weeklyProgress,
                favoriteSubject = findFavoriteSubject(completedQuizzes),
                studyTimeMinutes = studyTimeMinutes
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get game stats for student $studentId", e)
            generateInitialGameStats() // Return initial stats on error
        }
    }
    
    /**
     * Get detailed quiz statistics for a student
     */
    suspend fun getQuizStats(studentId: String): QuizStats = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching quiz stats for student: $studentId")
            
            val quizAttempts = quizAttemptRepository.getAttemptsByStudentId(studentId)
            val completedQuizzes = quizAttempts.filter { 
                it.containsKey("isSubmitted") && it["isSubmitted"] as Boolean 
            }
            
            // If no quiz attempts, return initial empty stats for new user
            if (completedQuizzes.isEmpty()) {
                return@withContext generateInitialQuizStats()
            }
            
            val scores = completedQuizzes.mapNotNull { it["score"] as? Double }
            val totalAttempts = completedQuizzes.size
            val averageScore = if (scores.isNotEmpty()) scores.average() else 0.0
            val bestScore = if (scores.isNotEmpty()) scores.maxOrNull() ?: 0.0 else 0.0
            val perfectScores = scores.count { it >= 1.0 }
            
            // Calculate improvement rate
            val improvementRate = calculateImprovementRate(scores)
            
            // Subject breakdown
            val subjectBreakdown = calculateSubjectBreakdown(completedQuizzes)
            
            // Recent scores (last 10)
            val recentScores = scores.takeLast(10)
            
            // Study time
            val timeSpentMinutes = totalAttempts * 15 // Assume 15 minutes per quiz
            
            QuizStats(
                totalAttempts = totalAttempts,
                averageScore = averageScore,
                bestScore = bestScore,
                perfectScores = perfectScores,
                improvementRate = improvementRate,
                subjectBreakdown = subjectBreakdown,
                recentScores = recentScores,
                timeSpentMinutes = timeSpentMinutes
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get quiz stats for student $studentId", e)
            generateInitialQuizStats()
        }
    }
    
    /**
     * Get learning progress for all classes a student is enrolled in
     */
    suspend fun getLearningProgress(studentId: String): List<LearningProgress> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching learning progress for student: $studentId")
            
            // This would typically query the student's enrolled classes
            // For now, return mock data based on quiz attempts
            val quizAttempts = quizAttemptRepository.getAttemptsByStudentId(studentId)
            val classCodes = quizAttempts.mapNotNull { it["classCode"] as? String }.distinct()
            
            // If no quiz attempts, return empty learning progress for new user
            if (classCodes.isEmpty()) {
                return@withContext emptyList()
            }
            
            classCodes.map { classCode ->
                val classAttempts = quizAttempts.filter { it["classCode"] == classCode }
                val completedQuizzes = classAttempts.filter { 
                    it.containsKey("isSubmitted") && it["isSubmitted"] as Boolean 
                }
                
                val averageScore = if (completedQuizzes.isNotEmpty()) {
                    completedQuizzes.mapNotNull { it["score"] as? Double }.average()
                } else 0.0
                
                val lastActivity = completedQuizzes.maxByOrNull { 
                    it["completedAt"] as? String ?: "" 
                }?.get("completedAt") as? String
                
                LearningProgress(
                    classId = classCode,
                    className = getClassName(classCode),
                    completedKlyps = completedQuizzes.size,
                    totalKlyps = classAttempts.size + 5, // Add some incomplete ones
                    averageQuizScore = averageScore,
                    lastActivity = lastActivity,
                    progressPercentage = (completedQuizzes.size.toDouble() / (classAttempts.size + 5)) * 100,
                    upcomingDeadlines = listOf("Assignment due tomorrow", "Quiz next week")
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get learning progress for student $studentId", e)
            emptyList()
        }
    }
    
    private fun calculateStreaks(completedQuizzes: List<Map<String, Any>>): Pair<Int, Int> {
        if (completedQuizzes.isEmpty()) return Pair(0, 0)
        
        try {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            val sortedDates = completedQuizzes
                .mapNotNull { it["completedAt"] as? String }
                .mapNotNull { 
                    try { 
                        dateFormat.parse(it) 
                    } catch (e: Exception) { 
                        null 
                    } 
                }
                .sortedDescending()
            
            if (sortedDates.isEmpty()) return Pair(0, 0)
            
            var currentStreak = 1
            var longestStreak = 1
            var tempStreak = 1
            
            for (i in 0 until sortedDates.size - 1) {
                val current = sortedDates[i]
                val next = sortedDates[i + 1]
                
                val daysDifference = ((current.time - next.time) / (1000 * 60 * 60 * 24)).toInt()
                
                if (daysDifference <= 1) {
                    if (i == 0) currentStreak++
                    tempStreak++
                    longestStreak = max(longestStreak, tempStreak)
                } else {
                    tempStreak = 1
                    if (i == 0) currentStreak = 1
                }
            }
            
            return Pair(currentStreak, longestStreak)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating streaks", e)
            return Pair(0, 0)
        }
    }
    
    private fun calculateExperiencePoints(completedQuizzes: List<Map<String, Any>>): Int {
        var totalXP = 0
        
        completedQuizzes.forEach { quiz ->
            totalXP += XP_PER_QUIZ_COMPLETION
            
            val score = quiz["score"] as? Double ?: 0.0
            if (score >= 1.0) {
                totalXP += XP_PER_PERFECT_SCORE
            }
        }
        
        return totalXP
    }
    
    private fun calculateTotalCorrectAnswers(completedQuizzes: List<Map<String, Any>>): Int {
        return completedQuizzes.sumOf { quiz ->
            val answers = quiz["answers"] as? List<*> ?: emptyList<Any>()
            answers.count { answer ->
                if (answer is Map<*, *>) {
                    answer["isCorrect"] as? Boolean ?: false
                } else false
            }
        }
    }
    
    private fun calculateTotalQuestions(completedQuizzes: List<Map<String, Any>>): Int {
        return completedQuizzes.sumOf { quiz ->
            val answers = quiz["answers"] as? List<*> ?: emptyList<Any>()
            answers.size
        }
    }
    
    private fun calculateWeeklyProgress(completedQuizzes: List<Map<String, Any>>): Int {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val weekStart = calendar.time
        
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        
        return completedQuizzes.count { quiz ->
            val completedAt = quiz["completedAt"] as? String
            if (completedAt != null) {
                try {
                    val date = dateFormat.parse(completedAt)
                    date?.after(weekStart) ?: false
                } catch (e: Exception) {
                    false
                }
            } else false
        }
    }
    
    private fun calculateImprovementRate(scores: List<Double>): Double {
        if (scores.size < 3) return 0.0
        
        val firstHalf = scores.take(scores.size / 2)
        val secondHalf = scores.drop(scores.size / 2)
        
        val firstAverage = firstHalf.average()
        val secondAverage = secondHalf.average()
        
        return secondAverage - firstAverage
    }
    
    private fun calculateSubjectBreakdown(completedQuizzes: List<Map<String, Any>>): Map<String, SubjectStats> {
        val subjectMap = mutableMapOf<String, MutableList<Double>>()
        
        completedQuizzes.forEach { quiz ->
            val classCode = quiz["classCode"] as? String ?: "Unknown"
            val score = quiz["score"] as? Double ?: 0.0
            
            subjectMap.getOrPut(classCode) { mutableListOf() }.add(score)
        }
        
        return subjectMap.mapValues { (subject, scores) ->
            val averageScore = scores.average()
            val mastery = when {
                averageScore >= 0.95 -> MasteryLevel.MASTER
                averageScore >= 0.85 -> MasteryLevel.EXPERT
                averageScore >= 0.75 -> MasteryLevel.ADVANCED
                averageScore >= 0.65 -> MasteryLevel.INTERMEDIATE
                else -> MasteryLevel.BEGINNER
            }
            
            SubjectStats(
                subject = getSubjectName(subject),
                averageScore = averageScore,
                totalQuizzes = scores.size,
                mastery = mastery
            )
        }
    }
    
    private fun generateAchievements(studentId: String, completedQuizzes: List<Map<String, Any>>): List<Achievement> {
        val achievements = mutableListOf<Achievement>()

        // Perfect Score Achievement
        val perfectScores = completedQuizzes.count { (it["score"] as? Double ?: 0.0) >= 1.0 }
        achievements.add(
            Achievement(
                id = "perfectionist",
                title = "Perfectionist",
                description = "Get 5 perfect quiz scores",
                icon = "⭐",
                progress = perfectScores,
                target = 5,
                isCompleted = perfectScores >= 5,
                category = AchievementCategory.QUIZ
            )
        )
        
        // Quiz Marathon Achievement
        achievements.add(
            Achievement(
                id = "quiz_marathon",
                title = "Quiz Marathon",
                description = "Complete 50 quizzes",
                icon = "🏃",
                progress = completedQuizzes.size,
                target = 50,
                isCompleted = completedQuizzes.size >= 50,
                category = AchievementCategory.QUIZ
            )
        )

        // First Quiz Achievement
        if (completedQuizzes.isNotEmpty()) {
            achievements.add(
                Achievement(
                    id = "first_quiz",
                    title = "Getting Started",
                    description = "Complete your first quiz",
                    icon = "🎯",
                    isCompleted = true,
                    category = AchievementCategory.QUIZ
                )
            )
        }
        
        return achievements
    }
    
    private fun generateBadges(completedQuizzes: List<Map<String, Any>>, achievements: List<Achievement>): List<Badge> {
        val badges = mutableListOf<Badge>()
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        // Quiz Count Badges
        when {
            completedQuizzes.size >= 100 -> badges.add(
                Badge("quiz_legend", "Quiz Legend", "Completed 100+ quizzes", "🏆", currentTime, BadgeRarity.LEGENDARY)
            )
            completedQuizzes.size >= 50 -> badges.add(
                Badge("quiz_master", "Quiz Master", "Completed 50+ quizzes", "🥇", currentTime, BadgeRarity.EPIC)
            )
            completedQuizzes.size >= 25 -> badges.add(
                Badge("quiz_expert", "Quiz Expert", "Completed 25+ quizzes", "🥈", currentTime, BadgeRarity.RARE)
            )
            completedQuizzes.size >= 10 -> badges.add(
                Badge("quiz_enthusiast", "Quiz Enthusiast", "Completed 10+ quizzes", "🥉", currentTime, BadgeRarity.COMMON)
            )
        }
        
        // Perfect Score Badges
        val perfectScores = completedQuizzes.count { (it["score"] as? Double ?: 0.0) >= 1.0 }
        if (perfectScores >= 5) {
            badges.add(
                Badge("perfectionist", "Perfectionist", "5 perfect quiz scores", "⭐", currentTime, BadgeRarity.RARE)
            )
        }
        
        return badges
    }
    
    private fun findFavoriteSubject(completedQuizzes: List<Map<String, Any>>): String? {
        val subjectCounts = completedQuizzes.groupingBy { it["classCode"] as? String ?: "Unknown" }
            .eachCount()
        
        return subjectCounts.maxByOrNull { it.value }?.let { getSubjectName(it.key) }
    }
    
    private fun getClassName(classCode: String): String {
        return when (classCode) {
            "CS101" -> "Computer Science 101"
            "MATH201" -> "Calculus I"
            "PHYS101" -> "Physics 101"
            "ENG101" -> "English Composition"
            "CHEM101" -> "General Chemistry"
            else -> classCode.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
    
    private fun getSubjectName(classCode: String): String {
        return when {
            classCode.startsWith("CS") -> "Computer Science"
            classCode.startsWith("MATH") -> "Mathematics"
            classCode.startsWith("PHYS") -> "Physics"
            classCode.startsWith("ENG") -> "English"
            classCode.startsWith("CHEM") -> "Chemistry"
            else -> classCode.replace("_", " ").replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Generate initial game stats for new users
     */
    private fun generateInitialGameStats(): GameStats {
        return GameStats(
            totalQuizzesCompleted = 0,
            averageQuizScore = 0.0,
            totalCorrectAnswers = 0,
            totalQuestions = 0,
            currentStreak = 0,
            longestStreak = 0,
            experiencePoints = 0,
            level = 1, // Start at level 1
            badges = emptyList(),
            achievements = emptyList(),
            weeklyGoal = 5, // Default weekly goal
            weeklyProgress = 0,
            favoriteSubject = null,
            studyTimeMinutes = 0
        )
    }

    /**
     * Generate initial quiz stats for new users
     */
    private fun generateInitialQuizStats(): QuizStats {
        return QuizStats(
            totalAttempts = 0,
            averageScore = 0.0,
            bestScore = 0.0,
            perfectScores = 0,
            improvementRate = 0.0,
            subjectBreakdown = emptyMap(),
            recentScores = emptyList(),
            timeSpentMinutes = 0
        )
    }}
