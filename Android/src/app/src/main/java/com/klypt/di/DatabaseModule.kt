package com.klypt.di

import android.content.Context
import com.klypt.data.DatabaseManager
import com.klypt.data.database.DatabaseInitializer
import com.klypt.data.repositories.StudentRepository
import com.klypt.data.repositories.EducatorRepository
import com.klypt.data.services.ChatSummaryService
import com.klypt.data.services.UserContextProvider
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

    @Provides
    @Singleton
    fun provideChatSummaryService(
        databaseManager: DatabaseManager
    ): ChatSummaryService {
        return ChatSummaryService(databaseManager)
    }

    @Provides
    @Singleton
    fun provideUserContextProvider(): UserContextProvider {
        return UserContextProvider()
    }
}
