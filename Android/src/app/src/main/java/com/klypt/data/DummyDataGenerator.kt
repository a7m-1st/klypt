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

package com.klypt.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Mms
import androidx.compose.material.icons.outlined.Widgets
import com.klypt.R
import com.klypt.data.models.ClassDocument
import com.klypt.data.models.Educator
import com.klypt.data.models.Klyp
import com.klypt.data.models.Question
import com.klypt.data.models.Student
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dummy data generator for populating the home page with sample content.
 * This provides realistic sample data for testing and development purposes.
 */
object DummyDataGenerator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
    private val currentTime = dateFormat.format(Date())

    // Sample Students
    fun generateSampleStudents(): List<Student> = listOf(
        Student(
            _id = "student_001",
            firstName = "Alice",
            lastName = "Johnson",
            recoveryCode = "REC001",
            enrolledClassIds = listOf("class_cs101", "class_math201", "class_phys101"),
            createdAt = currentTime,
            updatedAt = currentTime
        ),
        Student(
            _id = "student_002",
            firstName = "Bob",
            lastName = "Smith",
            recoveryCode = "REC002",
            enrolledClassIds = listOf("class_cs101", "class_eng101"),
            createdAt = currentTime,
            updatedAt = currentTime
        ),
        Student(
            _id = "student_003",
            firstName = "Carol",
            lastName = "Davis",
            recoveryCode = "REC003",
            enrolledClassIds = listOf("class_math201", "class_phys101", "class_chem101"),
            createdAt = currentTime,
            updatedAt = currentTime
        ),
        Student(
            _id = "student_004",
            firstName = "David",
            lastName = "Wilson",
            recoveryCode = "REC004",
            enrolledClassIds = listOf("class_cs101", "class_math201"),
            createdAt = currentTime,
            updatedAt = currentTime
        ),
        Student(
            _id = "student_005",
            firstName = "Emma",
            lastName = "Brown",
            recoveryCode = "REC005",
            enrolledClassIds = listOf("class_eng101", "class_hist101"),
            createdAt = currentTime,
            updatedAt = currentTime
        )
    )

    // Sample Educators
    fun generateSampleEducators(): List<Educator> = listOf(
        Educator(
            _id = "educator_001",
            fullName = "Dr. Sarah Mitchell",
            age = 42,
            currentJob = "Professor of Computer Science",
            instituteName = "Tech University",
            phoneNumber = "+1234567890",
            verified = true,
            recoveryCode = "EDU001",
            classIds = listOf("class_cs101", "class_cs201")
        ),
        Educator(
            _id = "educator_002",
            fullName = "Prof. John Anderson",
            age = 38,
            currentJob = "Associate Professor of Mathematics",
            instituteName = "Science Institute",
            phoneNumber = "+1234567891",
            verified = true,
            recoveryCode = "EDU002",
            classIds = listOf("class_math201", "class_math301")
        ),
        Educator(
            _id = "educator_003",
            fullName = "Dr. Lisa Chen",
            age = 35,
            currentJob = "Assistant Professor of Physics",
            instituteName = "Research University",
            phoneNumber = "+1234567892",
            verified = true,
            recoveryCode = "EDU003",
            classIds = listOf("class_phys101", "class_phys201")
        ),
        Educator(
            _id = "educator_004",
            fullName = "Prof. Michael Taylor",
            age = 45,
            currentJob = "Professor of English Literature",
            instituteName = "Liberal Arts College",
            phoneNumber = "+1234567893",
            verified = false,
            recoveryCode = "EDU004",
            classIds = listOf("class_eng101", "class_eng201")
        )
    )

    // Sample Classes
    fun generateSampleClasses(): List<ClassDocument> = listOf(
        ClassDocument(
            _id = "class_cs101",
            classCode = "CS101",
            classTitle = "Introduction to Computer Science",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_001",
            studentIds = listOf("student_001", "student_002", "student_004", "a_a") // Include current user
        ),
        ClassDocument(
            _id = "class_math201",
            classCode = "MATH201",
            classTitle = "Calculus II",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_002",
            studentIds = listOf("student_001", "student_003", "student_004", "a_a") // Include current user
        ),
        ClassDocument(
            _id = "class_phys101",
            classCode = "PHYS101", 
            classTitle = "General Physics I",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_003",
            studentIds = listOf("student_001", "student_003", "a_a") // Include current user
        ),
        ClassDocument(
            _id = "class_eng101",
            classCode = "ENG101",
            classTitle = "English Composition",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_004",
            studentIds = listOf("student_002", "student_005")
        ),
        ClassDocument(
            _id = "class_chem101",
            classCode = "CHEM101",
            classTitle = "General Chemistry",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_003",
            studentIds = listOf("student_003")
        ),
        ClassDocument(
            _id = "class_hist101",
            classCode = "HIST101",
            classTitle = "World History",
            updatedAt = currentTime,
            lastSyncedAt = currentTime,
            educatorId = "educator_004",
            studentIds = listOf("student_005")
        )
    )

    // Sample Klyps (Educational Content)
    fun generateSampleKlyps(): List<Klyp> = listOf(
        Klyp(
            _id = "klyp_001",
            classCode = "CS101",
            title = "Introduction to Programming Concepts",
            mainBody = """
                Programming is the process of creating instructions for computers to execute. 
                It involves problem-solving, logical thinking, and creativity.
                
                Key concepts include:
                • Variables and data types
                • Control structures (loops, conditionals)
                • Functions and procedures
                • Object-oriented programming
                
                Understanding these fundamentals is crucial for becoming a successful programmer.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "What is a variable in programming?",
                    options = listOf(
                        "A fixed value that never changes",
                        "A container for storing data values",
                        "A type of loop structure",
                        "A programming language"
                    ),
                    correctAnswer = 'B'
                ),
                Question(
                    questionText = "Which of the following is NOT a fundamental programming concept?",
                    options = listOf(
                        "Variables",
                        "Loops",
                        "Graphics design",
                        "Functions"
                    ),
                    correctAnswer = 'C'
                ),
                Question(
                    questionText = "What does OOP stand for?",
                    options = listOf(
                        "Object-Oriented Programming",
                        "Open Office Project",
                        "Optimized Operation Process",
                        "Online Organization Platform"
                    ),
                    correctAnswer = 'A'
                )
            ),
            createdAt = currentTime
        ),
        Klyp(
            _id = "klyp_002",
            classCode = "MATH201",
            title = "Limits and Continuity",
            mainBody = """
                Limits are fundamental to calculus and help us understand the behavior of functions.
                
                A limit describes the value that a function approaches as the input approaches some value.
                
                Key concepts:
                • Definition of a limit
                • One-sided limits
                • Infinite limits
                • Continuity at a point
                • Properties of continuous functions
                
                Mastering limits is essential for understanding derivatives and integrals.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "What does lim(x→a) f(x) = L mean?",
                    options = listOf(
                        "f(a) equals L",
                        "f(x) approaches L as x approaches a",
                        "f(x) equals L for all x",
                        "x equals a when f(x) equals L"
                    ),
                    correctAnswer = 'B'
                ),
                Question(
                    questionText = "A function is continuous at a point if:",
                    options = listOf(
                        "The function exists at that point",
                        "The limit exists at that point",
                        "The function value equals the limit at that point",
                        "All of the above"
                    ),
                    correctAnswer = 'D'
                )
            ),
            createdAt = currentTime
        ),
        Klyp(
            _id = "klyp_003",
            classCode = "PHYS101",
            title = "Newton's Laws of Motion",
            mainBody = """
                Sir Isaac Newton formulated three fundamental laws that describe the relationship 
                between forces acting on a body and its motion.
                
                First Law (Law of Inertia):
                An object at rest stays at rest, and an object in motion stays in motion 
                unless acted upon by an external force.
                
                Second Law:
                The acceleration of an object is directly proportional to the net force 
                acting on it and inversely proportional to its mass. F = ma
                
                Third Law:
                For every action, there is an equal and opposite reaction.
                
                These laws form the foundation of classical mechanics.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "According to Newton's First Law, what happens to a moving object with no external forces?",
                    options = listOf(
                        "It gradually slows down",
                        "It continues moving at constant velocity",
                        "It accelerates",
                        "It immediately stops"
                    ),
                    correctAnswer = 'B'
                ),
                Question(
                    questionText = "Newton's Second Law is expressed as:",
                    options = listOf(
                        "F = mv",
                        "F = ma",
                        "F = m/a",
                        "F = a/m"
                    ),
                    correctAnswer = 'B'
                ),
                Question(
                    questionText = "If you push on a wall, the wall pushes back with:",
                    options = listOf(
                        "Less force",
                        "More force",
                        "Equal force",
                        "No force"
                    ),
                    correctAnswer = 'C'
                )
            ),
            createdAt = currentTime
        ),
        Klyp(
            _id = "klyp_004",
            classCode = "ENG101",
            title = "Essay Writing Fundamentals",
            mainBody = """
                Effective essay writing is a crucial skill for academic and professional success.
                
                Basic Essay Structure:
                • Introduction with thesis statement
                • Body paragraphs with supporting evidence
                • Conclusion that reinforces the main argument
                
                Writing Process:
                1. Brainstorming and planning
                2. Drafting
                3. Revising for content and organization
                4. Editing for grammar and style
                5. Proofreading for final errors
                
                Remember: Clear, concise writing communicates ideas effectively.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "What should the introduction paragraph contain?",
                    options = listOf(
                        "Only background information",
                        "The conclusion",
                        "A thesis statement",
                        "Statistical data"
                    ),
                    correctAnswer = 'C'
                ),
                Question(
                    questionText = "What is the primary purpose of body paragraphs?",
                    options = listOf(
                        "To restate the thesis",
                        "To provide supporting evidence",
                        "To introduce new topics",
                        "To summarize other essays"
                    ),
                    correctAnswer = 'B'
                )
            ),
            createdAt = currentTime
        ),
        Klyp(
            _id = "klyp_005",
            classCode = "CHEM101",
            title = "Atomic Structure and Periodic Table",
            mainBody = """
                Understanding atomic structure is fundamental to chemistry.
                
                Atomic Components:
                • Protons: Positively charged particles in the nucleus
                • Neutrons: Neutral particles in the nucleus
                • Electrons: Negatively charged particles orbiting the nucleus
                
                The Periodic Table:
                • Organized by atomic number (number of protons)
                • Rows are called periods
                • Columns are called groups or families
                • Elements in the same group have similar properties
                
                Electron configuration determines chemical behavior and bonding patterns.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "What determines an element's identity?",
                    options = listOf(
                        "Number of neutrons",
                        "Number of electrons",
                        "Number of protons",
                        "Atomic mass"
                    ),
                    correctAnswer = 'C'
                ),
                Question(
                    questionText = "Elements in the same group of the periodic table have:",
                    options = listOf(
                        "The same atomic mass",
                        "The same number of protons",
                        "Similar chemical properties",
                        "The same number of neutrons"
                    ),
                    correctAnswer = 'C'
                )
            ),
            createdAt = currentTime
        ),
        Klyp(
            _id = "klyp_006",
            classCode = "HIST101",
            title = "Ancient Civilizations Overview",
            mainBody = """
                Ancient civilizations laid the foundation for modern society through their 
                innovations in government, technology, and culture.
                
                Key Ancient Civilizations:
                • Mesopotamia: "Cradle of civilization" - invented writing, wheel, and laws
                • Ancient Egypt: Pyramids, hieroglyphics, and advanced medicine
                • Ancient Greece: Democracy, philosophy, and mathematics
                • Ancient Rome: Republic/Empire, engineering, and legal systems
                • Ancient China: Inventions like paper, gunpowder, and compass
                
                These civilizations contributed lasting legacies that continue to influence 
                our world today through their political systems, technological innovations, 
                and cultural achievements.
            """.trimIndent(),
            questions = listOf(
                Question(
                    questionText = "Which civilization is known as the 'Cradle of Civilization'?",
                    options = listOf(
                        "Ancient Egypt",
                        "Ancient Greece",
                        "Mesopotamia",
                        "Ancient Rome"
                    ),
                    correctAnswer = 'C'
                ),
                Question(
                    questionText = "Which ancient civilization is credited with inventing democracy?",
                    options = listOf(
                        "Ancient Rome",
                        "Ancient Greece",
                        "Ancient Egypt",
                        "Ancient China"
                    ),
                    correctAnswer = 'B'
                ),
                Question(
                    questionText = "What major invention is Ancient China known for?",
                    options = listOf(
                        "The wheel",
                        "Hieroglyphics",
                        "Paper",
                        "Democracy"
                    ),
                    correctAnswer = 'C'
                )
            ),
            createdAt = currentTime
        )
    )

    // Enhanced dummy models for different AI tasks
    fun generateSampleModels(): List<Model> = listOf(
        Model(
            name = "Gemini Pro",
            version = "1.5",
            downloadFileName = "gemini_pro_1_5.tflite",
            url = "https://example.com/models/gemini_pro_1_5.tflite",
            sizeInBytes = 2_500_000_000L, // 2.5GB
            info = "Google's most capable AI model for complex reasoning, creative tasks, and code generation. Optimized for on-device performance.",
            configs = createLlmChatConfigs(),
            llmSupportImage = true,
            llmSupportAudio = true,
            estimatedPeakMemoryInBytes = 4_000_000_000L
        ),
        Model(
            name = "Llama 3.2",
            version = "3B",
            downloadFileName = "llama_3_2_3b.tflite",
            url = "https://example.com/models/llama_3_2_3b.tflite",
            sizeInBytes = 1_800_000_000L, // 1.8GB
            info = "Meta's efficient language model fine-tuned for educational content and conversation. Excellent balance of performance and efficiency.",
            configs = createLlmChatConfigs(),
            llmSupportImage = false,
            llmSupportAudio = false,
            estimatedPeakMemoryInBytes = 3_200_000_000L
        ),
        Model(
            name = "Claude Haiku",
            version = "3.5",
            downloadFileName = "claude_haiku_3_5.tflite",
            url = "https://example.com/models/claude_haiku_3_5.tflite",
            sizeInBytes = 1_200_000_000L, // 1.2GB
            info = "Anthropic's fast and efficient model optimized for quick responses and educational assistance.",
            configs = createLlmChatConfigs(),
            llmSupportImage = true,
            llmSupportAudio = false,
            estimatedPeakMemoryInBytes = 2_000_000_000L
        ),
        Model(
            name = "GPT-4 Mini",
            version = "turbo",
            downloadFileName = "gpt4_mini_turbo.tflite",
            url = "https://example.com/models/gpt4_mini_turbo.tflite",
            sizeInBytes = 950_000_000L, // 950MB
            info = "OpenAI's compact model designed for mobile devices. Great for quick questions and learning assistance.",
            configs = createLlmChatConfigs(),
            llmSupportImage = false,
            llmSupportAudio = true,
            estimatedPeakMemoryInBytes = 1_500_000_000L
        ),
        Model(
            name = "Mistral 7B",
            version = "instruct",
            downloadFileName = "mistral_7b_instruct.tflite",
            url = "https://example.com/models/mistral_7b_instruct.tflite",
            sizeInBytes = 4_200_000_000L, // 4.2GB
            info = "Mistral AI's instruction-tuned model with strong reasoning capabilities for complex academic tasks.",
            configs = createLlmChatConfigs(),
            llmSupportImage = false,
            llmSupportAudio = false,
            estimatedPeakMemoryInBytes = 6_000_000_000L
        ),
        Model(
            name = "Phi-3 Medium",
            version = "4K",
            downloadFileName = "phi3_medium_4k.tflite",
            url = "https://example.com/models/phi3_medium_4k.tflite",
            sizeInBytes = 800_000_000L, // 800MB
            info = "Microsoft's efficient small language model trained on high-quality educational data.",
            configs = createLlmChatConfigs(),
            llmSupportImage = false,
            llmSupportAudio = false,
            estimatedPeakMemoryInBytes = 1_200_000_000L
        )
    )

    // Populate tasks with sample models
    private fun populateTasksWithModels() {
        val models = generateSampleModels()
        
        // Clear existing models
        TASK_LLM_CHAT.models.clear()
        TASK_LLM_PROMPT_LAB.models.clear()
        TASK_LLM_ASK_IMAGE.models.clear()
        TASK_LLM_ASK_AUDIO.models.clear()

        models.forEach { model ->
            // Add to chat task
            TASK_LLM_CHAT.models.add(model.copy())
            
            // Add to prompt lab
            TASK_LLM_PROMPT_LAB.models.add(model.copy())
            
            // Add to image task if supported
            if (model.llmSupportImage) {
                TASK_LLM_ASK_IMAGE.models.add(model.copy())
            }
            
            // Add to audio task if supported
            if (model.llmSupportAudio) {
                TASK_LLM_ASK_AUDIO.models.add(model.copy())
            }
        }

        // Update task triggers to refresh UI
        TASK_LLM_CHAT.updateTrigger.value = System.currentTimeMillis()
        TASK_LLM_PROMPT_LAB.updateTrigger.value = System.currentTimeMillis()
        TASK_LLM_ASK_IMAGE.updateTrigger.value = System.currentTimeMillis()
        TASK_LLM_ASK_AUDIO.updateTrigger.value = System.currentTimeMillis()
    }

    // Generate realistic class schedules
    fun generateClassSchedules(): Map<String, List<String>> = mapOf(
        "class_cs101" to listOf("Monday 9:00 AM", "Wednesday 9:00 AM", "Friday 9:00 AM"),
        "class_math201" to listOf("Tuesday 11:00 AM", "Thursday 11:00 AM"),
        "class_phys101" to listOf("Monday 2:00 PM", "Wednesday 2:00 PM"),
        "class_eng101" to listOf("Tuesday 10:00 AM", "Thursday 10:00 AM"),
        "class_chem101" to listOf("Monday 1:00 PM", "Friday 1:00 PM"),
        "class_hist101" to listOf("Wednesday 3:00 PM", "Friday 3:00 PM")
    )

    // Generate assignments for classes
    fun generateAssignments(): Map<String, List<String>> = mapOf(
        "class_cs101" to listOf(
            "Programming Assignment 1: Variables and Data Types",
            "Quiz: Control Structures",
            "Project: Simple Calculator App",
            "Midterm Exam: Programming Fundamentals"
        ),
        "class_math201" to listOf(
            "Homework 3: Limits and Continuity",
            "Quiz: Derivative Rules",
            "Project: Real-world Application of Calculus",
            "Final Exam: Integration Techniques"
        ),
        "class_phys101" to listOf(
            "Lab Report: Motion Experiments",
            "Problem Set: Newton's Laws",
            "Group Project: Physics in Daily Life",
            "Final Exam: Mechanics"
        ),
        "class_eng101" to listOf(
            "Essay 1: Personal Narrative",
            "Research Paper: Current Events Analysis",
            "Peer Review Workshop",
            "Final Portfolio Submission"
        ),
        "class_chem101" to listOf(
            "Lab Report: Atomic Structure",
            "Quiz: Periodic Table Trends",
            "Group Project: Chemical Reactions",
            "Final Exam: General Chemistry"
        ),
        "class_hist101" to listOf(
            "Essay: Ancient Civilizations Comparison",
            "Timeline Project: Historical Events",
            "Research Paper: Historical Figure Analysis",
            "Final Exam: World History Overview"
        )
    )

    /**
     * Initialize all dummy data for the application
     */
    fun initializeDummyData() {
        processTasks()
    }
}
