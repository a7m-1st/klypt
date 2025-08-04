package com.klypt.data.utils

import com.klypt.data.models.*

/**
 * Utility functions to convert database results to data classes
 */
object DatabaseUtils {

    fun mapToStudent(data: Map<String, Any>): Student? {
        return try {
            Student(
                _id = data["_id"] as? String ?: return null,
                type = data["type"] as? String ?: "student",
                firstName = data["firstName"] as? String ?: "",
                lastName = data["lastName"] as? String ?: "",
                recoveryCode = data["recoveryCode"] as? String ?: "",
                enrolledClassIds = (data["enrolledClassIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                createdAt = data["createdAt"] as? String ?: "",
                updatedAt = data["updatedAt"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    fun mapToEducator(data: Map<String, Any>): Educator? {
        return try {
            Educator(
                _id = data["_id"] as? String ?: return null,
                type = data["type"] as? String ?: "educator",
                fullName = data["fullName"] as? String ?: "",
                age = data["age"] as? Int ?: 0,
                currentJob = data["currentJob"] as? String ?: "",
                instituteName = data["instituteName"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                verified = data["verified"] as? Boolean ?: false,
                recoveryCode = data["recoveryCode"] as? String ?: "",
                classIds = (data["classIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun mapToClassDocument(data: Map<String, Any>): ClassDocument? {
        return try {
            ClassDocument(
                _id = data["_id"] as? String ?: return null,
                type = data["type"] as? String ?: "class",
                classCode = data["classCode"] as? String ?: "",
                classTitle = data["classTitle"] as? String ?: "",
                updatedAt = data["updatedAt"] as? String ?: "",
                lastSyncedAt = data["lastSyncedAt"] as? String ?: "",
                educatorId = data["educatorId"] as? String ?: "",
                studentIds = (data["studentIds"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            )
        } catch (e: Exception) {
            null
        }
    }

    fun mapToKlyp(data: Map<String, Any>): Klyp? {
        return try {
            val questionsData = data["questions"] as? List<*> ?: emptyList<Any>()
            val questions = questionsData.mapNotNull { questionMap ->
                if (questionMap is Map<*, *>) {
                    try {
                        Question(
                            questionText = questionMap["questionText"] as? String ?: "",
                            options = (questionMap["options"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                            correctAnswer = (questionMap["correctAnswer"] as? String)?.firstOrNull() ?: 'A'
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }

            Klyp(
                _id = data["_id"] as? String ?: return null,
                type = data["type"] as? String ?: "klyp",
                classCode = data["classCode"] as? String ?: "",
                title = data["title"] as? String ?: "",
                mainBody = data["mainBody"] as? String ?: "",
                questions = questions,
                createdAt = data["createdAt"] as? String ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }

    fun studentToMap(student: Student): Map<String, Any> {
        return mapOf(
            "_id" to student._id,
            "type" to student.type,
            "firstName" to student.firstName,
            "lastName" to student.lastName,
            "recoveryCode" to student.recoveryCode,
            "enrolledClassIds" to student.enrolledClassIds,
            "createdAt" to student.createdAt,
            "updatedAt" to student.updatedAt
        )
    }

    fun educatorToMap(educator: Educator): Map<String, Any> {
        return mapOf(
            "_id" to educator._id,
            "type" to educator.type,
            "fullName" to educator.fullName,
            "age" to educator.age,
            "currentJob" to educator.currentJob,
            "instituteName" to educator.instituteName,
            "phoneNumber" to educator.phoneNumber,
            "verified" to educator.verified,
            "recoveryCode" to educator.recoveryCode,
            "classIds" to educator.classIds
        )
    }

    fun classDocumentToMap(classDoc: ClassDocument): Map<String, Any> {
        return mapOf(
            "_id" to classDoc._id,
            "type" to classDoc.type,
            "classCode" to classDoc.classCode,
            "classTitle" to classDoc.classTitle,
            "updatedAt" to classDoc.updatedAt,
            "lastSyncedAt" to classDoc.lastSyncedAt,
            "educatorId" to classDoc.educatorId,
            "studentIds" to classDoc.studentIds
        )
    }

    fun klypToMap(klyp: Klyp): Map<String, Any> {
        val questionsMap = klyp.questions.map { question ->
            mapOf(
                "questionText" to question.questionText,
                "options" to question.options,
                "correctAnswer" to question.correctAnswer.toString()
            )
        }

        return mapOf(
            "_id" to klyp._id,
            "type" to klyp.type,
            "classCode" to klyp.classCode,
            "title" to klyp.title,
            "mainBody" to klyp.mainBody,
            "questions" to questionsMap,
            "createdAt" to klyp.createdAt
        )
    }
}
