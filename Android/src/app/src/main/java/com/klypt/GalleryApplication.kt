package com.klypt

import android.app.Application
import com.klypt.common.writeLaunchInfo
import com.klypt.data.DataStoreRepository
import com.klypt.data.database.CouchDBManager
import com.klypt.data.sync.SyncService
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
  @Inject lateinit var couchDBManager: CouchDBManager
  @Inject lateinit var syncService: SyncService
  
  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

  override fun onCreate() {
    super.onCreate()

    writeLaunchInfo(context = this)

    // Load saved theme.
    ThemeSettings.themeOverride.value = dataStoreRepository.readTheme()

    FirebaseApp.initializeApp(this)
    
    // Initialize CouchDB (already done in CouchDBManager constructor)
    // The CouchDBManager is initialized when injected
  }
}
