package com.remotemotorcontroller.settings

import android.bluetooth.le.ScanSettings
import android.content.Context
import androidx.datastore.preferences.core.edit
import com.remotemotorcontroller.App.Companion.settingsDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import com.remotemotorcontroller.utils.hexToBytesOrNull
import com.remotemotorcontroller.utils.toHex

class SettingsRepository(private val ctx: Context) {

    val settings: Flow<AppSettings> = ctx.settingsDataStore.data.map { preferences ->
        val companyId = preferences[SettingsKeys.COMPANY_ID] ?: 0x706D
        val deviceId6Hex = preferences[SettingsKeys.DEVICE_ID6_HEX]
        val deviceId6 = deviceId6Hex?.hexToBytesOrNull()
        val timeoutMs = preferences[SettingsKeys.TIMEOUT_MS] ?: 20_000L
        val retryMs = preferences[SettingsKeys.RETRY_MS] ?: 500L
        val autoReconnect = preferences[SettingsKeys.AUTO_RECONNECT] ?: true
        val filterScanDevice = preferences[SettingsKeys.FILTER_SCAN_DEVICE] ?: true
        val scanMode = preferences[SettingsKeys.SCAN_MODE] ?: ScanSettings.SCAN_MODE_LOW_LATENCY
        val cleanupDurationMs = preferences[SettingsKeys.CLEANUP_DURATION_MS] ?: 5_000L

        val maxPoints = preferences[SettingsKeys.MAX_POINTS] ?: 600

        AppSettings(ar = AutoReconnectSettings(autoReconnect,companyId,
            deviceId6, timeoutMs = timeoutMs,
            retryInterval = retryMs),
            ble= BleSettings(filterScanDevice, scanMode, cleanupDurationMs),
            analy = AnalyticSettings(maxPoints))
    }

    // HELPER FUNCTIONS TO WRITE

    // AUTO RECONNECT SETTINGS
    suspend fun setDeviceId6(deviceId6: ByteArray) = ctx.settingsDataStore.edit{
        it[SettingsKeys.DEVICE_ID6_HEX] = deviceId6.toHex()
    }
    suspend fun clearDeviceId6() = ctx.settingsDataStore.edit{ it.remove(SettingsKeys.DEVICE_ID6_HEX) }
    suspend fun setCompanyId(v: Int) = ctx.settingsDataStore.edit{ it[SettingsKeys.COMPANY_ID] = v }
    suspend fun enableAutoReconnect(enable: Boolean) = ctx.settingsDataStore.edit { it[SettingsKeys.AUTO_RECONNECT] = enable }
    suspend fun setAutoReconnectTimes(timeoutMs: Long, retryInterval: Long){
        ctx.settingsDataStore.edit{
            it[SettingsKeys.TIMEOUT_MS] = timeoutMs
            it[SettingsKeys.RETRY_MS] = retryInterval
        }
    }

    // BLUETOOTH SCAN SETTINGS
    suspend fun enableFilter(enable: Boolean) = ctx.settingsDataStore.edit{ it[SettingsKeys.FILTER_SCAN_DEVICE] = enable}
    suspend fun setScanMode(mode: Int) = ctx.settingsDataStore.edit { it[SettingsKeys.SCAN_MODE] = mode }
    suspend fun setCleanupDuration(cleanupDurationMs: Long) = ctx.settingsDataStore.edit { it[SettingsKeys.CLEANUP_DURATION_MS] = cleanupDurationMs}

    // ANALYTIC SETTINGS
    suspend fun setMaxPoints(points: Int) = ctx.settingsDataStore.edit{ it[SettingsKeys.MAX_POINTS] = points}
}