package com.klypt

import android.content.Context
import com.couchbase.lite.*
import com.klypt.data.models.*
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class DatabaseManager(private val context: Context) {

    var inventoryDatabase: Database? = null
    var warehouseDatabase: Database? = null
    var klyptDatabase: Database? = null

    private val defaultInventoryDatabaseName = "inventory"
    private val warehouseDatabaseName = "warehouse"
    private val klyptDatabaseName = "klypts"
    private val startingWarehouseFileName = "startingWarehouses.zip"
    private val startingWarehouseDatabaseName = "startingWarehouses"

    private val documentTypeIndexName = "idxDocumentType"
    private val documentTypeAttributeName = "documentType"

    private val teamIndexName = "idxTeam"
    private val teamAttributeName = "team"

    private val cityIndexName = "idxCityType"
    private val cityAttributeName = "city"

    private val cityStateIndexName = "idxCityStateType"
    private val stateAttributeName = "state"

    private val auditIndexName = "idxAudit"
    private val projectIdAttributeName = "projectId"

    // New indexes for Klyp data models
    private val studentIndexName = "idxStudent"
    private val educatorIndexName = "idxEducator"
    private val classCodeIndexName = "idxClassCode"
    private val klypIndexName = "idxKlyp"
    private val quizAttemptIndexName = "idxQuizAttempt"
    private val studentIdAttributeName = "studentId"
    private val educatorIdAttributeName = "educatorId"
    private val classCodeAttributeName = "classCode"
    private val klypIdAttributeName = "klypId"

    var currentInventoryDatabaseName = "inventory"

    init {
        //setup couchbase lite
        CouchbaseLite.init(context)

        //turn on uber logging - in production apps this shouldn't be turn on
        Database.log.console.domains = LogDomain.ALL_DOMAINS
        Database.log.console.level = LogLevel.VERBOSE
    }

    fun dispose() {
        inventoryDatabase?.close()
        warehouseDatabase?.close()
        klyptDatabase?.close()
    }

    fun deleteDatabases() {
        try {
            closeDatabases()
            Database.delete(currentInventoryDatabaseName, context.filesDir)
            Database.delete(warehouseDatabaseName, context.filesDir)
            Database.delete(klyptDatabaseName, context.filesDir)
        } catch (e: Exception) {
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    fun closeDatabases() {
        try {
            inventoryDatabase?.close()
            warehouseDatabase?.close()
            klyptDatabase?.close()
        } catch (e: java.lang.Exception) {
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    fun initializeDatabases() {
        try {
            val dbConfig = DatabaseConfigurationFactory.create(context.filesDir.toString())

            // create or open a database to share between team members to store
            // projects, assets, and user profiles
            // calculate database name based on current logged in users team name
//            val teamName = (currentUser.team.filterNot { it.isWhitespace() }).lowercase()
//            currentInventoryDatabaseName = teamName.plus("_").plus(defaultInventoryDatabaseName)
            inventoryDatabase = Database("klypt", dbConfig)

            //setup the warehouse Database
            setupWarehouseDatabase(dbConfig)

            // setup the Klyp database for educational content
            setupKlyptDatabase(dbConfig)

            //create indexes for database queries
            createTypeIndex(warehouseDatabase)
            createTypeIndex(inventoryDatabase)
            createTypeIndex(klyptDatabase)

            createTeamTypeIndex()
            createCityTypeIndex()
            createCityCountryTypeIndex()
            createAuditIndex()

            // Create indexes for new Klyp data models
            createKlyptIndexes()

        } catch (e: Exception) {
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    private fun setupWarehouseDatabase(dbConfig: DatabaseConfiguration) {
        // create the warehouse database if it doesn't already exist
        if (!Database.exists(warehouseDatabaseName, context.filesDir)) {
            unzip(startingWarehouseFileName, File(context.filesDir.toString()))

            // copy the warehouse database to the project database
            // never open the database directly as this will cause issues
            // with sync
            val warehouseDbFile =
                File(
                    String.format(
                        "%s/%s",
                        context.filesDir,
                        ("${startingWarehouseDatabaseName}.cblite2")
                    )
                )
            Database.copy(warehouseDbFile, warehouseDatabaseName, dbConfig)
        }
        warehouseDatabase = Database(warehouseDatabaseName, dbConfig)
    }

    private fun setupKlyptDatabase(dbConfig: DatabaseConfiguration) {
        // create or open the Klyp database for storing educational content
        klyptDatabase = Database(klyptDatabaseName, dbConfig)
    }

    private fun createKlyptIndexes() {
        try {
            klyptDatabase?.let { db ->
                // Index for students by type
                if (!db.indexes.contains(studentIndexName)) {
                    db.createIndex(
                        studentIndexName,
                        IndexBuilder.valueIndex(
                            ValueIndexItem.property(documentTypeAttributeName),
                            ValueIndexItem.property("firstName"),
                            ValueIndexItem.property("lastName")
                        )
                    )
                }

                // Index for educators by type
                if (!db.indexes.contains(educatorIndexName)) {
                    db.createIndex(
                        educatorIndexName,
                        IndexBuilder.valueIndex(
                            ValueIndexItem.property(documentTypeAttributeName),
                            ValueIndexItem.property("instituteName")
                        )
                    )
                }

                // Index for classes by class code
                if (!db.indexes.contains(classCodeIndexName)) {
                    db.createIndex(
                        classCodeIndexName,
                        IndexBuilder.valueIndex(
                            ValueIndexItem.property(documentTypeAttributeName),
                            ValueIndexItem.property(classCodeAttributeName)
                        )
                    )
                }

                // Index for Klyps by class code
                if (!db.indexes.contains(klypIndexName)) {
                    db.createIndex(
                        klypIndexName,
                        IndexBuilder.valueIndex(
                            ValueIndexItem.property(documentTypeAttributeName),
                            ValueIndexItem.property(classCodeAttributeName),
                            ValueIndexItem.property("title")
                        )
                    )
                }

                // Index for quiz attempts
                if (!db.indexes.contains(quizAttemptIndexName)) {
                    db.createIndex(
                        quizAttemptIndexName,
                        IndexBuilder.valueIndex(
                            ValueIndexItem.property(documentTypeAttributeName),
                            ValueIndexItem.property(studentIdAttributeName),
                            ValueIndexItem.property(klypIdAttributeName),
                            ValueIndexItem.property(classCodeAttributeName)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error creating Klyp indexes: ${e.message}", e)
        }
    }

    private fun createTeamTypeIndex(){
        try {
            inventoryDatabase?.let {  // 1
                if (!it.indexes.contains(teamIndexName)) {
                    // create index for ProjectListView to only return documents with
                    // the type attribute set to project and the team attribute set to the
                    // logged in users team
                    it.createIndex( // 2
                        teamIndexName, // 3
                        IndexBuilder.valueIndex(   // 4
                            ValueIndexItem.property(documentTypeAttributeName), // 5
                            ValueIndexItem.property(teamAttributeName)) // 5
                    )
                }
            }
        } catch (e: Exception){
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    private fun createCityTypeIndex(){
        try {
            inventoryDatabase?.let {  // 1
                if (!it.indexes.contains(cityIndexName)) {
                    // create index for Warehouse only return documents with
                    // the type attribute set to warehouse and the city attribute filtered
                    // by value sent in using `like` statement
                    it.createIndex( // 3
                        cityIndexName, // 4
                        IndexBuilder.valueIndex(   // 5
                            ValueIndexItem.property(documentTypeAttributeName), // 5
                            ValueIndexItem.property(cityAttributeName)) // 5
                    )
                }
            }
        } catch (e: Exception){
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    private fun createCityCountryTypeIndex(){
        try {
            inventoryDatabase?.let {  // 1
                if (!it.indexes.contains(cityIndexName)) {
                    // create index for Locations only return documents with
                    // the type attribute set to location, the city attribute filtered
                    // by value sent in using `like` statement, and the country attribute filtered
                    // by the value sent in using `like` statement

                    it.createIndex( // 3
                        cityStateIndexName, // 4
                        IndexBuilder.valueIndex(   // 5
                            ValueIndexItem.property(documentTypeAttributeName), // 5
                            ValueIndexItem.property(cityAttributeName), // 5
                            ValueIndexItem.property(stateAttributeName)) // 5
                    )
                }
            }
        } catch (e: Exception){
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    private fun createAuditIndex(){
        try {
            inventoryDatabase?.let {  // 1
                if (!it.indexes.contains(auditIndexName)) {
                    // create index for Audits to return documents with
                    // the type attribute set to audit, the projectId filtered
                    // by value sent in using equals, and the team attribute filtered
                    // by the value sent in using equals

                    it.createIndex( // 3
                        auditIndexName, // 4
                        IndexBuilder.valueIndex(   // 5
                            ValueIndexItem.property(documentTypeAttributeName), // 5
                            ValueIndexItem.property(projectIdAttributeName), // 5
                            ValueIndexItem.property(teamAttributeName)) // 5
                    )
                }
            }
        } catch (e: Exception){
            android.util.Log.e(e.message, e.stackTraceToString())
        }
    }

    private fun createTypeIndex(
        database: Database?
    ) {
        // create indexes for document type
        // create index for document type if it doesn't exist
        database?.let {
            if (!it.indexes.contains(documentTypeIndexName)) {
                it.createIndex(
                    documentTypeIndexName, IndexBuilder.valueIndex(
                        ValueIndexItem.expression(
                            Expression.property(documentTypeAttributeName)
                        )
                    )
                )
            }
        }
    }

    private fun unzip(
        file: String,
        destination: File
    ) {
        context.assets.open(file).use { stream ->
            val buffer = ByteArray(1024)
            val zis = ZipInputStream(stream)
            var ze: ZipEntry? = zis.nextEntry
            while (ze != null) {
                val fileName: String = ze.name
                val newFile = File(destination, fileName)
                if (ze.isDirectory) {
                    newFile.mkdirs()
                } else {
                    File(newFile.parent!!).mkdirs()
                    val fos = FileOutputStream(newFile)
                    var len: Int
                    while (zis.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                    fos.close()
                }
                ze = zis.nextEntry
            }
            zis.closeEntry()
            zis.close()
            stream.close()
        }
    }

    // Utility methods for Klyp data operations
    
    fun saveStudent(student: Student): Boolean {
        return try {
            klyptDatabase?.let { db ->
                val document = MutableDocument(student._id)
                document.setString("type", student.type)
                document.setString("firstName", student.firstName)
                document.setString("lastName", student.lastName)
                document.setString("recoveryCode", student.recoveryCode)
                document.setArray("enrolledClassIds", MutableArray(student.enrolledClassIds))
                document.setString("createdAt", student.createdAt)
                document.setString("updatedAt", student.updatedAt)
                db.save(document)
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error saving student: ${e.message}", e)
            false
        }
    }

    fun saveEducator(educator: Educator): Boolean {
        return try {
            klyptDatabase?.let { db ->
                val document = MutableDocument(educator._id)
                document.setString("type", educator.type)
                document.setString("fullName", educator.fullName)
                document.setInt("age", educator.age)
                document.setString("currentJob", educator.currentJob)
                document.setString("instituteName", educator.instituteName)
                document.setString("phoneNumber", educator.phoneNumber)
                document.setBoolean("verified", educator.verified)
                document.setString("recoveryCode", educator.recoveryCode)
                document.setArray("classIds", MutableArray(educator.classIds))
                db.save(document)
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error saving educator: ${e.message}", e)
            false
        }
    }

    fun saveClass(classDocument: ClassDocument): Boolean {
        return try {
            klyptDatabase?.let { db ->
                val document = MutableDocument(classDocument._id)
                document.setString("type", classDocument.type)
                document.setString("classCode", classDocument.classCode)
                document.setString("classTitle", classDocument.classTitle)
                document.setString("updatedAt", classDocument.updatedAt)
                document.setString("lastSyncedAt", classDocument.lastSyncedAt)
                document.setString("educatorId", classDocument.educatorId)
                document.setArray("studentIds", MutableArray(classDocument.studentIds))
                db.save(document)
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error saving class: ${e.message}", e)
            false
        }
    }

    fun saveKlyp(klyp: Klyp): Boolean {
        return try {
            klyptDatabase?.let { db ->
                val document = MutableDocument(klyp._id)
                document.setString("type", klyp.type)
                document.setString("classCode", klyp.classCode)
                document.setString("title", klyp.title)
                document.setString("mainBody", klyp.mainBody)
                document.setString("createdAt", klyp.createdAt)
                
                // Convert questions to array
                val questionsArray = MutableArray()
                klyp.questions.forEach { question ->
                    val questionDict = MutableDictionary()
                    questionDict.setString("questionText", question.questionText)
                    questionDict.setArray("options", MutableArray(question.options))
                    questionDict.setString("correctAnswer", question.correctAnswer.toString())
                    questionsArray.addDictionary(questionDict)
                }
                document.setArray("questions", questionsArray)
                
                db.save(document)
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error saving klyp: ${e.message}", e)
            false
        }
    }

    fun saveQuizAttempt(quizAttempt: QuizAttempt): Boolean {
        return try {
            klyptDatabase?.let { db ->
                val document = MutableDocument(quizAttempt._id)
                document.setString("type", quizAttempt.type)
                document.setString("studentId", quizAttempt.studentId)
                document.setString("klypId", quizAttempt.klypId)
                document.setString("classCode", quizAttempt.classCode)
                document.setDouble("percentageComplete", quizAttempt.percentageComplete)
                quizAttempt.score?.let { document.setDouble("score", it) }
                document.setString("startedAt", quizAttempt.startedAt)
                quizAttempt.completedAt?.let { document.setString("completedAt", it) }
                document.setBoolean("isSubmitted", quizAttempt.isSubmitted)
                
                // Convert answers to array
                val answersArray = MutableArray()
                quizAttempt.answers.forEach { answer ->
                    val answerDict = MutableDictionary()
                    answerDict.setInt("questionIndex", answer.questionIndex)
                    answer.selectedAnswer?.let { answerDict.setString("selectedAnswer", it.toString()) }
                    answer.isCorrect?.let { answerDict.setBoolean("isCorrect", it) }
                    answersArray.addDictionary(answerDict)
                }
                document.setArray("answers", answersArray)
                
                db.save(document)
                true
            } ?: false
        } catch (e: Exception) {
            android.util.Log.e("DatabaseManager", "Error saving quiz attempt: ${e.message}", e)
            false
        }
    }
}