package com.klypt.data.models

data class ClassDocument(
    val _id: String, // Could be the class code
    val type: String = "class",
    val classCode: String,
    val classTitle: String,
    val updatedAt: String,
    val lastSyncedAt: String,
    val educatorId: String,
    val studentIds: List<String>
)
