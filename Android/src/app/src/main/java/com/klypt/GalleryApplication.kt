package com.klypt

import android.app.Application
import com.klypt.common.writeLaunchInfo
import com.klypt.data.DataStoreRepository
import com.klypt.data.database.DatabaseInitializer
import com.klypt.ui.theme.ThemeSettings
import com.google.firebase.FirebaseApp
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
    databaseInitializer.initializeOnStartup()
  }
  
  override fun onTerminate() {
    super.onTerminate()
    // Clean up database resources
    databaseInitializer.cleanup()
  }
}
