package com.remotemotorcontroller.settings

import android.bluetooth.le.ScanSettings
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object SettingsKeys{
    val COMPANY_ID = intPreferencesKey("company_id")
    val DEVICE_ID6_HEX = stringPreferencesKey("device_id6_hex")
    val TIMEOUT_MS = longPreferencesKey("timeout_ms")
    val RETRY_MS = longPreferencesKey("retry_ms")
    val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    val FILTER_SCAN_DEVICE = booleanPreferencesKey("filter_scan_device")
    val SCAN_MODE = intPreferencesKey("scan_mode")
    val CLEANUP_DURATION_MS = longPreferencesKey("cleanup_duration_ms")

}
data class AutoReconnectSettings(
    val autoReconnect: Boolean = true,
    val companyId: Int = 0x706D,
    val deviceId6: ByteArray? = null,
    val timeoutMs: Long = 20_000,
    val retryInterval: Long = 500,
)
data class BleSettings(
    val filterScanDevice: Boolean = true,
    val scanMode: Int = ScanSettings.SCAN_MODE_LOW_LATENCY,
    val cleanupDurationMs: Long = 5_000,
)
data class AppSettings(
    val ar: AutoReconnectSettings = AutoReconnectSettings(),
    val ble: BleSettings = BleSettings()
)