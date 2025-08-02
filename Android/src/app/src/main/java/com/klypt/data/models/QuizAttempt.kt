package com.klypt.data.models

data class QuizAttempt(
    val _id: String, // unique attempt ID
    val type: String = "quiz_attempt",
    val studentId: String,
    val klypId: String,
    val classCode: String,
    val answers: List<StudentAnswer>,
    val percentageComplete: Double,
    val score: Double?,
    val startedAt: String,
    val completedAt: String?,
    val isSubmitted: Boolean = false
)

data class StudentAnswer(
    val questionIndex: Int,
    val selectedAnswer: Char?, // A, B, C, D or null if not answered
    val isCorrect: Boolean? = null // Calculated after submission
)
