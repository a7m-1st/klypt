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

package com.klypt.di

import com.klypt.data.repository.EducationalContentRepository
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.repositories.ClassDocumentRepository
import com.klypt.data.repositories.KlypRepository
import com.klypt.data.utils.DatabaseSeeder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing educational content related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object EducationalContentModule {

    @Provides
    @Singleton
    fun provideEducationalContentRepository(
        studentRepository: StudentRepository,
        educatorRepository: EducatorRepository,
        classRepository: ClassDocumentRepository,
        klypRepository: KlypRepository,
        databaseSeeder: DatabaseSeeder
    ): EducationalContentRepository {
        return EducationalContentRepository(
            studentRepository,
            educatorRepository,
            classRepository,
            klypRepository,
            databaseSeeder
        )
    }
}
