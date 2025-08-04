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

package com.klypt.data.utils

import kotlin.random.Random

/**
 * Utility object for generating unique class codes
 */
object ClassCodeGenerator {
    
    private val allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val CODE_LENGTH = 8
    
    /**
     * Generates a random 8-character class code
     * @return An 8-character string consisting of uppercase letters and numbers
     */
    fun generateClassCode(): String {
        return (1..CODE_LENGTH)
            .map { allowedCharacters.random() }
            .joinToString("")
    }
    
    /**
     * Validates if a class code has the correct format
     * @param classCode The class code to validate
     * @return true if the class code is valid (8 characters, alphanumeric, uppercase)
     */
    fun isValidClassCode(classCode: String): Boolean {
        if (classCode.length != CODE_LENGTH) return false
        return classCode.all { it in allowedCharacters }
    }
    
    /**
     * Formats a class code for display (e.g., "ABC123XY" -> "ABC1-23XY")
     * @param classCode The class code to format
     * @return Formatted class code for better readability
     */
    fun formatClassCodeForDisplay(classCode: String): String {
        if (classCode.length != CODE_LENGTH) return classCode
        return "${classCode.substring(0, 4)}-${classCode.substring(4)}"
    }
}
