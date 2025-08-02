package com.klypt.di

import android.content.Context
import com.klypt.DatabaseManager
import com.klypt.data.database.DatabaseInitializer
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseManager(
        @ApplicationContext context: Context
    ): DatabaseManager {
        return DatabaseManager(context)
    }

    @Provides
    @Singleton
    fun provideStudentRepository(
        databaseManager: DatabaseManager
    ): StudentRepository {
        return StudentRepository(databaseManager)
    }

    @Provides
    @Singleton
    fun provideEducatorRepository(
        databaseManager: DatabaseManager
    ): EducatorRepository {
        return EducatorRepository(databaseManager)
    }

    @Provides
    @Singleton
    fun provideDatabaseInitializer(
        databaseManager: DatabaseManager
    ): DatabaseInitializer {
        return DatabaseInitializer(databaseManager)
    }
}
