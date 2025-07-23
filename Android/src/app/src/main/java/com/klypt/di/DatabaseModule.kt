package com.klypt.di

import android.content.Context
import com.google.gson.Gson
import com.klypt.data.database.CouchDBManager
import com.klypt.data.repository.UserRepositoryImpl
import com.klypt.data.sync.SyncService
import com.klypt.domain.repository.UserRepository
import com.klypt.network.AuthApiService
import com.klypt.storage.TokenManager
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
    fun provideCouchDBManager(
        @ApplicationContext context: Context
    ): CouchDBManager = CouchDBManager(context)
    
    @Provides
    @Singleton
    fun provideUserRepository(
        dbManager: CouchDBManager
    ): UserRepository = UserRepositoryImpl(dbManager)
    
    @Provides
    @Singleton
    fun provideSyncService(
        userRepository: UserRepository,
        authApiService: AuthApiService,
        tokenManager: TokenManager
    ): SyncService = SyncService(userRepository, authApiService, tokenManager)
}
