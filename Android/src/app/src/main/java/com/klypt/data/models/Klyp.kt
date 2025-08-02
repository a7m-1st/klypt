package com.klypt.data.models

data class Klyp(
    val _id: String,
    val type: String = "klyp",
    val classCode: String, // For easy filtering
    val title: String,
    val mainBody: String,
    val questions: List<Question>,
    val createdAt: String
)

data class Question(
    val questionText: String,
    val options: List<String>, // A, B, C, D options
    val correctAnswer: Char // A, B, C, or D
)
