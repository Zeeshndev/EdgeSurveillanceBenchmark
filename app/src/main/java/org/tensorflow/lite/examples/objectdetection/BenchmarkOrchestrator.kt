package org.tensorflow.lite.examples.objectdetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class BenchmarkOrchestrator(
    private val context: Context,
    private val detectorHelper: ObjectDetectorHelper
) {
    private val benchmarkExecutor = Executors.newSingleThreadExecutor()
    private val telemetryExecutor = Executors.newSingleThreadScheduledExecutor()
    private val metricsExporter = MetricsExporter(context)

    @SuppressLint("WakelockTimeout")
    fun runBenchmark(config: BenchmarkConfig) {
        benchmarkExecutor.execute {
            try {
                if (!checkPhysicalGates()) {
                    Log.e("Orchestrator", "Physical gates failed. Aborting run.")
                    return@execute
                }
                
                // Fetch Battery Health at the absolute start of the trial
                val batteryHealth = getBatteryHealth()
                if (batteryHealth != "GOOD") {
                    Log.e("Orchestrator", "FATAL: Battery health is $batteryHealth. Aborting to prevent data contamination.")
                    return@execute
                }

                Log.i("Orchestrator", "Gates passed. Starting controlled test for ${config.modelName}...")

                val datasetFeeder = DatasetFeeder(context)
                val telemetryCollector = TelemetryCollector(context)
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager

                val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BenchmarkApp::InferenceLock")
                wakeLock.acquire(40 * 60 * 1000L) 

                val samples = mutableListOf<TelemetryCollector.Sample>()
                val latenciesMs = mutableListOf<Double>()

                telemetryExecutor.scheduleAtFixedRate({
                    samples.add(telemetryCollector.sampleOnce(powerManager))
                }, 0, 1, TimeUnit.SECONDS)

                // --- Claude's Dynamic Warmup Convergence ---
                Log.i("Orchestrator", "Starting dynamic warmup...")
                val warmupLatencies = mutableListOf<Double>()
                val maxWarmup = 50
                var convergedAt = maxWarmup

                for (i in 0 until maxWarmup) {
                    val bitmap = datasetFeeder.getNextFrame()
                    val t0 = SystemClock.elapsedRealtimeNanos()
                    detectorHelper.detect(bitmap, 0)
                    val t1 = SystemClock.elapsedRealtimeNanos()
                    
                    warmupLatencies.add((t1 - t0) / 1_000_000.0)

                    // Check convergence every 5 iterations after a floor of 10
                    if (i >= 10 && i % 5 == 0) {
                        val recent = warmupLatencies.takeLast(5)
                        val maxLat = recent.maxOrNull() ?: 0.0
                        val minLat = recent.minOrNull() ?: 0.0
                        val avgLat = recent.average()
                        val cv = (maxLat - minLat) / avgLat
                        
                        if (cv < 0.05) { 
                            convergedAt = i + 1
                            Log.i("Warmup", "Converged after $convergedAt iterations. CV: $cv")
                            break
                        }
                    }
                }
                if (convergedAt == maxWarmup) {
                    Log.w("Warmup", "Did NOT converge within $maxWarmup iterations. Latencies: $warmupLatencies")
                }
                // -------------------------------------------

                // Set to 30 * 60 * 1000L for the real Phase 1 matrix
                val durationMs = 30 * 60 * 1000L
                val startTime = System.currentTimeMillis()
                val endTime = startTime + durationMs

                Log.i("Orchestrator", "Entering main inference loop...")
                var maxLatencyMs = 0.0

                while (System.currentTimeMillis() < endTime) {
                    val bitmap = datasetFeeder.getNextFrame()

                    val t0 = SystemClock.elapsedRealtimeNanos()
                    detectorHelper.detect(bitmap, 0)
                    val t1 = SystemClock.elapsedRealtimeNanos()

                    val latencyMs = (t1 - t0) / 1_000_000.0
                    latenciesMs.add(latencyMs)

                    if (latencyMs > maxLatencyMs) {
                        maxLatencyMs = latencyMs
                    }
                }

                telemetryExecutor.shutdown()
                if (wakeLock.isHeld) wakeLock.release()

                exportData(config, latenciesMs, samples, maxLatencyMs, batteryHealth)
                Log.i("Orchestrator", "Benchmark complete. All data exported.")

            } catch (e: Exception) {
                Log.e("Orchestrator", "Crash during benchmark", e)
            }
        }
    }

    private fun getBatteryHealth(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        return when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
            BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            else -> "UNKNOWN"
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

    private fun exportData(
        config: BenchmarkConfig,
        latenciesMs: List<Double>,
        samples: List<TelemetryCollector.Sample>,
        maxLatencyMs: Double,
        batteryHealth: String
    ) {
        val dir = context.getExternalFilesDir(null) ?: return
        val sanitizedName = config.modelName.replace(".tflite", "")

        val latencyFile = File(dir, "trial_${sanitizedName}_raw_latencies.csv")
        FileWriter(latencyFile).use { writer ->
            writer.append("InferenceIndex,LatencyMs\n")
            latenciesMs.forEachIndexed { index, ms ->
                writer.append(String.format(Locale.US, "%d,%.3f\n", index, ms))
            }
        }

        val telemetryFile = File(dir, "trial_${sanitizedName}_telemetry.csv")
        FileWriter(telemetryFile).use { writer ->
            writer.append("TimestampMs,PssKb,RawCurrentUa,BatteryPct,TemperatureC,ThermalStatus\n")
            samples.forEach { s ->
                writer.append(
                    String.format(
                        Locale.US,
                        "%d,%d,%d,%d,%.1f,%d\n",
                        s.timestampMs, s.pssKb, s.rawCurrentUa, s.batteryPct, s.temperatureC, s.thermalStatus
                    )
                )
            }
        }

        val cleanLatencies = latenciesMs.filter { it <= 500.0 }
        val osInterferences = latenciesMs.count { it > 500.0 }
        
        val avgLatency = if (cleanLatencies.isNotEmpty()) cleanLatencies.average() else 0.0
        val medianLatency = if (cleanLatencies.isNotEmpty()) {
            val sorted = cleanLatencies.sorted()
            if (sorted.size % 2 == 0) {
                (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else {
                sorted[sorted.size / 2]
            }
        } else 0.0

        val peakMem = samples.maxByOrNull { it.pssKb }?.pssKb ?: 0
        val startBat = samples.firstOrNull()?.batteryPct ?: 0
        val endBat = samples.lastOrNull()?.batteryPct ?: 0
        
        val startTemp = samples.firstOrNull()?.temperatureC ?: 0.0
        val maxTemp = samples.maxByOrNull { it.temperatureC }?.temperatureC ?: 0.0

        metricsExporter.appendSummary(
            modelName = config.modelName,
            delegate = config.delegate,
            threads = config.numThreads,
            totalInferences = latenciesMs.size,
            avgLatencyMs = avgLatency,
            medianLatencyMs = medianLatency,
            maxLatencyMs = maxLatencyMs,
            osInterferences = osInterferences,
            peakMemKb = peakMem,
            startBattery = startBat,
            endBattery = endBat,
            startTempC = startTemp,
            maxTempC = maxTemp,
            batteryHealth = batteryHealth
        )
    }
}