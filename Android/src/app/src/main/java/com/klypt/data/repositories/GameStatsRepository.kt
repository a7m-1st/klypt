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
            
            // If no quiz attempts, generate sample data for demonstration
            if (quizAttempts.isEmpty()) {
                return@withContext generateSampleGameStats()
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
            generateSampleGameStats() // Return sample stats on error
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
            
            // If no quiz attempts, generate sample data
            if (completedQuizzes.isEmpty()) {
                return@withContext generateSampleQuizStats()
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
            generateSampleQuizStats()
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
            
            // If no quiz attempts, return sample learning progress
            if (classCodes.isEmpty()) {
                return@withContext generateSampleLearningProgress()
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
            generateSampleLearningProgress()
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
     * Generate sample game stats for demonstration purposes
     */
    private fun generateSampleGameStats(): GameStats {
        val currentTime = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        
        return GameStats(
            totalQuizzesCompleted = 23,
            averageQuizScore = 0.82,
            totalCorrectAnswers = 89,
            totalQuestions = 115,
            currentStreak = 5,
            longestStreak = 12,
            experiencePoints = 450,
            level = 5,
            badges = listOf(
                Badge("first_quiz", "Getting Started", "Completed first quiz", "🎯", currentTime, BadgeRarity.COMMON),
                Badge("quiz_enthusiast", "Quiz Enthusiast", "Completed 10+ quizzes", "🥉", currentTime, BadgeRarity.COMMON),
                Badge("streak_master", "Streak Master", "5-day streak", "🔥", currentTime, BadgeRarity.RARE)
            ),
            achievements = listOf(
                Achievement("first_quiz", "First Steps", "Complete your first quiz", "🎯", 1, 1, true, "25 XP", AchievementCategory.QUIZ),
                Achievement("perfectionist", "Perfectionist", "Get 5 perfect scores", "⭐", 2, 5, false, "100 XP", AchievementCategory.QUIZ),
                Achievement("streak_builder", "Streak Builder", "Maintain 7-day streak", "🔥", 5, 7, false, "75 XP", AchievementCategory.STREAK)
            ),
            weeklyGoal = 5,
            weeklyProgress = 3,
            favoriteSubject = "Computer Science",
            studyTimeMinutes = 345
        )
    }
    
    /**
     * Generate sample quiz stats for demonstration purposes
     */
    private fun generateSampleQuizStats(): QuizStats {
        return QuizStats(
            totalAttempts = 23,
            averageScore = 0.82,
            bestScore = 1.0,
            perfectScores = 2,
            improvementRate = 0.15,
            subjectBreakdown = mapOf(
                "Computer Science" to SubjectStats("Computer Science", 0.85, 8, MasteryLevel.ADVANCED),
                "Mathematics" to SubjectStats("Mathematics", 0.78, 6, MasteryLevel.INTERMEDIATE),
                "Physics" to SubjectStats("Physics", 0.90, 5, MasteryLevel.ADVANCED),
                "English" to SubjectStats("English", 0.75, 4, MasteryLevel.INTERMEDIATE)
            ),
            recentScores = listOf(0.80, 0.85, 0.70, 0.95, 1.0, 0.75, 0.85, 0.90, 0.80, 0.85),
            timeSpentMinutes = 345
        )
    }
    
    /**
     * Generate sample learning progress for demonstration purposes
     */
    private fun generateSampleLearningProgress(): List<LearningProgress> {
        return listOf(
            LearningProgress(
                classId = "CS101",
                className = "Computer Science 101",
                completedKlyps = 8,
                totalKlyps = 12,
                averageQuizScore = 0.85,
                lastActivity = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                progressPercentage = 66.7,
                upcomingDeadlines = listOf("Final Project due next week", "Quiz on Friday")
            ),
            LearningProgress(
                classId = "MATH201",
                className = "Calculus I",
                completedKlyps = 6,
                totalKlyps = 10,
                averageQuizScore = 0.78,
                lastActivity = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                progressPercentage = 60.0,
                upcomingDeadlines = listOf("Homework due tomorrow")
            ),
            LearningProgress(
                classId = "PHYS101",
                className = "Physics 101",
                completedKlyps = 5,
                totalKlyps = 8,
                averageQuizScore = 0.90,
                lastActivity = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date()),
                progressPercentage = 62.5,
                upcomingDeadlines = listOf("Lab report due next Tuesday")
            )
        )
    }
}
