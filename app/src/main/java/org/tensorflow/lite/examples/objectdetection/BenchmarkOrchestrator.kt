package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit



class BenchmarkOrchestrator(
    private val context: Context,
    private val detectorHelper: ObjectDetectorHelper
) {
    private val benchmarkExecutor = Executors.newSingleThreadExecutor()
    private val telemetryExecutor = Executors.newSingleThreadScheduledExecutor()

    fun runBenchmark(config: BenchmarkConfig) {
        benchmarkExecutor.execute {
            try {
                // 1. The Physical Gatekeeper
                if (!checkPhysicalGates()) {
                    Log.e("Orchestrator", "Physical gates failed. Aborting run.")
                    return@execute
                }
                Log.i("Orchestrator", "Gates passed. Starting 30-minute controlled test for ${config.modelName}...")

                // 2. Initialize Dependencies
                val datasetFeeder = DatasetFeeder(context)
                val telemetryCollector = TelemetryCollector(context)
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                
                val samples = mutableListOf<TelemetryCollector.Sample>()
                val latenciesMs = mutableListOf<Long>()

                // 3. Start 1Hz Telemetry Loop (Claude's continuous tracker)
                telemetryExecutor.scheduleAtFixedRate({
                    samples.add(telemetryCollector.sampleOnce(powerManager))
                }, 0, 1, TimeUnit.SECONDS)

                // 4. Warmup Phase (10 frames)
                for (i in 0 until 10) {
                    val bitmap = datasetFeeder.getNextFrame()
                    detectorHelper.detect(bitmap, 0)
                }

                // 5. 30-Minute Wall-Clock Loop (Claude's fix)
                // For tonight's smoke test, let's set this to 1 minute (60,000 ms) to verify it works quickly.
                // For tomorrow's real matrix, change this to 30 minutes (30 * 60 * 1000L).
                val durationMs = 30 * 60 * 1000L 
                val startTime = System.currentTimeMillis()
                val endTime = startTime + durationMs

                Log.i("Orchestrator", "Entering main inference loop...")
                while (System.currentTimeMillis() < endTime) {
                    val bitmap = datasetFeeder.getNextFrame()
                    
                    val t0 = SystemClock.elapsedRealtimeNanos()
                    detectorHelper.detect(bitmap, 0)
                    val t1 = SystemClock.elapsedRealtimeNanos()
                    
                    latenciesMs.add((t1 - t0) / 1_000_000)
                }

                // 6. Stop Telemetry
                telemetryExecutor.shutdown()

                // 7. Export the 3 CSV Files
                exportData(config.modelName, latenciesMs, samples)
                Log.i("Orchestrator", "Benchmark complete. All data exported.")
                
            } catch (e: Exception) {
                Log.e("Orchestrator", "Crash during benchmark", e)
            }
        }
    }

    private fun checkPhysicalGates(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context.registerReceiver(null, ifilter)
        }
        val status: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = level * 100 / scale.toFloat()

        if (isCharging) {
            Log.e("Gatekeeper", "Device is charging. Unplug to continue.")
            return false
        }
        if (batteryPct !in 85.0..95.0) {
            Log.e("Gatekeeper", "Battery at $batteryPct%. Must be between 85-95%.")
            return false
        }
        return true
    }

    private fun exportData(modelName: String, latencies: List<Long>, samples: List<TelemetryCollector.Sample>) {
        val dir = context.getExternalFilesDir(null) ?: return
        val sanitizedName = modelName.replace(".tflite", "")
        
        // CSV 1: Raw Latency Array (For p95 Calculations)
        val latencyFile = File(dir, "trial_${sanitizedName}_raw_latencies.csv")
        FileWriter(latencyFile).use { writer ->
            writer.append("InferenceIndex,LatencyMs\n")
            latencies.forEachIndexed { index, ms ->
                writer.append("$index,$ms\n")
            }
        }

        // CSV 2: Continuous Telemetry Curve
        val telemetryFile = File(dir, "trial_${sanitizedName}_telemetry.csv")
        FileWriter(telemetryFile).use { writer ->
            writer.append("TimestampMs,PssKb,RawCurrentUa,BatteryPct,ThermalStatus\n")
            samples.forEach { s ->
                writer.append("${s.timestampMs},${s.pssKb},${s.rawCurrentUa},${s.batteryPct},${s.thermalStatus}\n")
            }
        }

        // CSV 3: Master Rollup Sheet
        val summaryFile = File(dir, "benchmark_results.csv")
        val isNewFile = !summaryFile.exists()
        FileWriter(summaryFile, true).use { writer ->
            if (isNewFile) {
                writer.append("Model,TotalInferences,AvgLatencyMs,PeakMemKb,StartBattery,EndBattery\n")
            }
            val avgLatency = if (latencies.isNotEmpty()) latencies.average() else 0.0
            val peakMem = samples.maxByOrNull { it.pssKb }?.pssKb ?: 0
            val startBat = samples.firstOrNull()?.batteryPct ?: 0
            val endBat = samples.lastOrNull()?.batteryPct ?: 0
            
            writer.append("$modelName,${latencies.size},${String.format("%.2f", avgLatency)},$peakMem,$startBat,$endBat\n")
        }
    }
}