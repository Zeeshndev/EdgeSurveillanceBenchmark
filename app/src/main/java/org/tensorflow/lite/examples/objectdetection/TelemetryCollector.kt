package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.os.BatteryManager
import android.os.Debug
import android.os.PowerManager
import android.util.Log
import java.util.concurrent.Executor

class TelemetryCollector(private val context: Context, private val mainExecutor: Executor) {
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    
    // Event-driven thermal logging (API 29+)
    private val thermalListener = PowerManager.OnThermalStatusChangedListener { status ->
        Log.i("Telemetry", "THERMAL_STATUS_CHANGED: \$status")
    }

    fun startTracking() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            powerManager.addThermalStatusListener(mainExecutor, thermalListener)
        }
    }

    fun pollMetrics() {
        // Low-frequency polling to avoid observation bias
        val currentMicroAmps = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        val memInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(memInfo)
        val physicalMemoryKB = memInfo.totalPss
        
        Log.i("Telemetry", "Battery µA: \$currentMicroAmps | Mem PSS KB: \$physicalMemoryKB")
    }

    fun stopTracking() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            powerManager.removeThermalStatusListener(thermalListener)
        }
    }
}