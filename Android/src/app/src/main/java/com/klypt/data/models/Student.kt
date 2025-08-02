package com.klypt.data.models

data class Student(
    val _id: String, // student ID
    val type: String = "student",
    val firstName: String,
    val lastName: String,
    val recoveryCode: String,
    val enrolledClassIds: List<String>, // References to class documents
    val createdAt: String,
    val updatedAt: String
)
