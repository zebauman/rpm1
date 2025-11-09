package com.remotemotorcontroller

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.remotemotorcontroller.ble.BLEManager
import com.remotemotorcontroller.settings.SettingsRepository

// APPLICATION SUB CLASS
class App : Application(){
    lateinit var repo: SettingsRepository
        private set // MAKES THE SETTER FOR THE REPO ONLY ACCESSIBLE TO THE MAIN ACTIVITY
    companion object{
        val Context.settingsDataStore by preferencesDataStore(name = "app_prefs")
    }
    override fun onCreate() {
        super.onCreate()
        repo = SettingsRepository(applicationContext)
        BLEManager.init(this)
    }
}