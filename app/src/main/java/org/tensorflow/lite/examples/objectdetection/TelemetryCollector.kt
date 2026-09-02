package org.tensorflow.lite.examples.objectdetection

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.Process

class TelemetryCollector(private val context: Context) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val myPid = Process.myPid()

    data class Sample(
        val timestampMs: Long,
        val pssKb: Int,
        val rawCurrentUa: Int,
        val batteryPct: Int,
        val temperatureC: Double,
        val thermalStatus: Int
    )

    fun sampleOnce(powerManager: PowerManager): Sample {
        val memInfo = activityManager.getProcessMemoryInfo(intArrayOf(myPid))[0]

        // Read sticky battery intent for real continuous temperature extraction
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = rawTemp / 10.0

        return Sample(
            timestampMs = System.currentTimeMillis(),
            pssKb = memInfo.totalPss,
            rawCurrentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            temperatureC = tempCelsius,
            thermalStatus = powerManager.currentThermalStatus
        )
    }
}