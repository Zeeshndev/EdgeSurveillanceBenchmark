package org.tensorflow.lite.examples.objectdetection

import android.app.ActivityManager
import android.content.Context
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
        val thermalStatus: Int
    )

    fun sampleOnce(powerManager: PowerManager): Sample {
        val memInfo = activityManager.getProcessMemoryInfo(intArrayOf(myPid))[0]
        return Sample(
            timestampMs = System.currentTimeMillis(),
            pssKb = memInfo.totalPss,
            rawCurrentUa = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW),
            batteryPct = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY),
            thermalStatus = powerManager.currentThermalStatus
        )
    }
}