package com.klypt.data.models

data class Educator(
    val _id: String,
    val type: String = "educator",
    val fullName: String,
    val age: Int,
    val currentJob: String,
    val instituteName: String,
    val phoneNumber: String,
    val verified: Boolean,
    val recoveryCode: String,
    val classIds: List<String> // Reference to class documents
)
