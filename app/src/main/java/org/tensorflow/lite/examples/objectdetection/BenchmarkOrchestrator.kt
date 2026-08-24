package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class BenchmarkOrchestrator(
    private val context: Context,
    private val engine: InferenceEngine,
    private val telemetry: TelemetryCollector,
    private val exporter: MetricsExporter
) {
    // Dedicated background thread to ensure UI rendering doesn't interfere with CPU/GPU timing
    private val benchmarkExecutor: Executor = Executors.newSingleThreadExecutor()

    private fun enforcePreTrialGate(context: Context): Boolean {
        // Register receiver to get live battery metrics
        val batteryStatus: android.content.Intent? = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        
        // 1. Calculate State of Charge (SoC)
        val level: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (scale > 0) level * 100 / scale.toFloat() else -1f
        
        // 2. Check Power State (Must be DISCHARGING)
        val status: Int = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isDischarging = status == android.os.BatteryManager.BATTERY_STATUS_DISCHARGING || status == android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING
        
        // 3. Numeric Temperature (°C)
        val tempInt = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempInt / 10.0f
        
        // 4. Hardware Thermal Enum
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        val thermalStatus = powerManager.currentThermalStatus

        var gatePassed = true

        // --- GATE EVALUATIONS ---
        if (batteryPct !in 85.0..95.0) {
            android.util.Log.e("Orchestrator", "GATE FAILED: Battery at ${batteryPct}%. Must be between 85-95%.")
            gatePassed = false
        }
        if (!isDischarging) {
            android.util.Log.e("Orchestrator", "GATE FAILED: Device is charging. Unplug to start trial.")
            gatePassed = false
        }
        if (thermalStatus > android.os.PowerManager.THERMAL_STATUS_LIGHT) {
            android.util.Log.e("Orchestrator", "GATE FAILED: OS Thermal Status too high ($thermalStatus).")
            gatePassed = false
        }

        if (gatePassed) {
            android.util.Log.i("Orchestrator", "GATE PASSED: Battery ${batteryPct}%, Temp: ${tempCelsius}°C, Discharging, Thermal Enum: $thermalStatus")
        } else {
            android.util.Log.e("Orchestrator", "CURRENT TEMP: ${tempCelsius}°C. Please wait for cooldown or correct conditions.")
        }
        
        return gatePassed
    }

    fun runBenchmark(config: BenchmarkConfig, modelBuffer: ByteBuffer) {
        benchmarkExecutor.execute {
            // ---> THE NEW PRE-TRIAL GATE <---
            if (!enforcePreTrialGate(context)) {
                Log.e("Orchestrator", "TRIAL ABORTED: Experimental controls violated. See anomaly log.")
                return@execute
            }

            Log.i("Orchestrator", "INIT: Starting benchmark for ${config.modelName} on ${config.delegate}")
            
            var crashCaptured = false
            var errorMessage: String? = null
            var averageLatencyMs = 0.0

            try {
                // Step 1: Engage telemetry and load the model
                telemetry.startTracking()
                engine.loadModel(modelBuffer, config.delegate)

                // Step 2: Warmup phase (stabilizes thermals and memory)
                Log.i("Orchestrator", "PHASE 1: Executing ${config.warmupIterations} warmup iterations...")
                engine.warmup(config.warmupIterations)

                // Step 3: Measured inference loop
                Log.i("Orchestrator", "PHASE 2: Executing ${config.inferenceIterations} measured iterations...")
                var totalLatency = 0L
                val dummyFrame = Any() // Will be replaced by actual CameraX ImageProxy later
                
                for (i in 0 until config.inferenceIterations) {
                    val start = System.currentTimeMillis()
                    engine.runInference(dummyFrame)
                    totalLatency += (System.currentTimeMillis() - start)
                }
                averageLatencyMs = totalLatency / config.inferenceIterations.toDouble()

            } catch (e: Exception) {
                // Gracefully catch the YOLOv11 GPU delegate crash
                crashCaptured = true
                errorMessage = e.stackTraceToString()
                Log.e("Orchestrator", "CRASH INTERCEPTED: ${e.message}")
            } finally {
                // Step 4: Stop tracking and extract hardware metrics
                telemetry.pollMetrics()
                telemetry.stopTracking()

                // Step 5: Package and export the results
                val result = BenchmarkResult(
                    config = config,
                    averageLatencyMs = averageLatencyMs,
                    peakMemoryPssKb = 0, // Hooked up to TelemetryCollector in next phase
                    batteryDrainMicroAmps = 0, // Hooked up to TelemetryCollector in next phase
                    thermalThrottlingOccurred = false,
                    gpuCrashCaptured = crashCaptured,
                    errorMessage = errorMessage
                )
                
                exporter.exportToCsv(result)
                Log.i("Orchestrator", "COMPLETE: Benchmark finished and results safely exported to CSV.")
            }
        }
    }
}