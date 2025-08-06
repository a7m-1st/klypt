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

package com.klypt.ui.newclass

data class NewClassUiState(
    val className: String = "",
    val classNameError: String? = null,
    val classCode: String = "",
    val classCodeError: String? = null,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val showClassCodeInput: Boolean = false,
    val showCreateNewForm: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val showDuplicateDialog: Boolean = false,
    val duplicateClassInfo: DuplicateClassInfo? = null
)

data class DuplicateClassInfo(
    val existingClassName: String,
    val existingClassCode: String,
    val importData: Map<String, Any>,
    val klypsData: List<Map<String, Any>>
)
