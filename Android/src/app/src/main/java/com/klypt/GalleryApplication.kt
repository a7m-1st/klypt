package com.klypt

import android.app.Application
import android.util.Log
import com.klypt.common.writeLaunchInfo
import com.klypt.data.DataStoreRepository
import com.klypt.data.database.DatabaseInitializer
import com.klypt.ui.theme.ThemeSettings
import com.google.firebase.FirebaseApp
import com.klypt.ui.common.ApiKeyConfig
import com.klypt.ui.modelmanager.ModelManagerViewModel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class GalleryApplication : Application() {

  @Inject lateinit var dataStoreRepository: DataStoreRepository
  @Inject lateinit var databaseInitializer: DatabaseInitializer
  
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun onCreate() {
    super.onCreate()

    writeLaunchInfo(context = this)

    // Load saved theme.
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()

    FirebaseApp.initializeApp(this)
    
    // Initialize CouchDB database
    Log.d("GalleryApplication", "About to initialize database...")
    Log.d("GalleryApplication", "DatabaseInitializer instance: $databaseInitializer")
    databaseInitializer.initializeOnStartup()
    Log.d("GalleryApplication", "Database initialization call completed")

    if(ApiKeyConfig.isHuggingFaceConfigured()) {
      dataStoreRepository.saveAccessTokenData(
        accessToken = ApiKeyConfig.huggingFaceApiKey,
        refreshToken = "",
        expiresAt = System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 10,
      )
      Log.d("Settings", "Configuring HuggingFace API key")
    } else {
      Log.d("Settings", "HuggingFace API key not configured")
    }
  }
  
  override fun onTerminate() {
    super.onTerminate()
    // Clean up database resources
    databaseInitializer.cleanup()
  }
}
