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

package com.klypt.data.models

data class GameStats(
    val totalQuizzesCompleted: Int = 0,
    val averageQuizScore: Double = 0.0,
    val totalCorrectAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val experiencePoints: Int = 0,
    val level: Int = 1,
    val badges: List<Badge> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val weeklyGoal: Int = 5,
    val weeklyProgress: Int = 0,
    val favoriteSubject: String? = null,
    val studyTimeMinutes: Int = 0
)

data class Badge(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,
    val earnedAt: String,
    val rarity: BadgeRarity = BadgeRarity.COMMON
)

enum class BadgeRarity {
    COMMON, RARE, EPIC, LEGENDARY
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val progress: Int = 0,
    val target: Int = 1,
    val isCompleted: Boolean = false,
    val reward: String? = null,
    val category: AchievementCategory = AchievementCategory.GENERAL
)

enum class AchievementCategory {
    QUIZ, STUDY_TIME, STREAK, EXPLORATION, GENERAL
}

data class QuizStats(
    val totalAttempts: Int = 0,
    val averageScore: Double = 0.0,
    val bestScore: Double = 0.0,
    val perfectScores: Int = 0,
    val improvementRate: Double = 0.0,
    val subjectBreakdown: Map<String, SubjectStats> = emptyMap(),
    val recentScores: List<Double> = emptyList(),
    val timeSpentMinutes: Int = 0
)

data class SubjectStats(
    val subject: String,
    val averageScore: Double = 0.0,
    val totalQuizzes: Int = 0,
    val mastery: MasteryLevel = MasteryLevel.BEGINNER
)

enum class MasteryLevel {
    BEGINNER, INTERMEDIATE, ADVANCED, EXPERT, MASTER
}

data class LearningProgress(
    val classId: String,
    val className: String,
    val completedKlyps: Int = 0,
    val totalKlyps: Int = 0,
    val averageQuizScore: Double = 0.0,
    val lastActivity: String? = null,
    val progressPercentage: Double = 0.0,
    val upcomingDeadlines: List<String> = emptyList()
)
