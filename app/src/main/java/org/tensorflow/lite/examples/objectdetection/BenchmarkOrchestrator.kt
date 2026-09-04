package org.tensorflow.lite.examples.objectdetection

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
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
                
                val batteryHealth = getBatteryHealth()
                if (batteryHealth != "GOOD" && !batteryHealth.startsWith("UNKNOWN")) {
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

                Log.i("Orchestrator", "Received UI Config -> Delegate: '${config.delegate}'")
                detectorHelper.currentDelegate = if (config.delegate.equals("GPU", ignoreCase = true)) ObjectDetectorHelper.DELEGATE_GPU else ObjectDetectorHelper.DELEGATE_CPU

                // --- RESEARCH OVERRIDE: Force strict thread counts ---
                val forcedThreads = if (detectorHelper.currentDelegate == ObjectDetectorHelper.DELEGATE_CPU) 4 else 1
                detectorHelper.numThreads = forcedThreads
                Log.i("Orchestrator", "UI bypassed. Forcing threads to: $forcedThreads")
                // -----------------------------------------------------

                detectorHelper.setupObjectDetector()

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

                    if (i >= 10 && i % 5 == 0) {
                        val recent = warmupLatencies.takeLast(5)
                        val cv = (recent.maxOrNull()!! - recent.minOrNull()!!) / recent.average()
                        if (cv < 0.05) { 
                            convergedAt = i + 1
                            break
                        }
                    }
                }

                // --- Automated Kernel & Backend Gate ---
                var finalDelegateName = config.delegate
                if (config.delegate.equals("GPU", ignoreCase = true)) {
                    val (isValid, backend) = validateGpuState()
                    if (!isValid) {
                        Log.e("Orchestrator", "FATAL: GPU requested but 0 kernels created (Silent CPU Fallback). Aborting trial.")
                        telemetryExecutor.shutdown()
                        if (wakeLock.isHeld) wakeLock.release()
                        return@execute
                    }
                    // Dynamically append the backend (e.g., "GPU (OpenGL)") so the CSV logger captures it
                    finalDelegateName = "GPU ($backend)"
                    Log.i("Orchestrator", "GPU Verification Passed. Proceeding with backend: $finalDelegateName")
                }
                // ------------------------------------------------

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

                // Pass the dynamically updated delegate name and forcedThreads to the exporter
                exportData(config.modelName, finalDelegateName, forcedThreads, latenciesMs, samples, maxLatencyMs, batteryHealth)
                Log.i("Orchestrator", "Benchmark complete. All data exported.")

            } catch (e: Exception) {
                Log.e("Orchestrator", "Crash during benchmark", e)
            }
        }
    }

    private fun validateGpuState(): Pair<Boolean, String> {
        try {
            // Scrape the app's own logcat buffer for native C++ hardware flags
            val process = Runtime.getRuntime().exec("logcat -d -t 1500 --pid=${android.os.Process.myPid()}")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            var backend = "Unknown"
            var kernelCount = -1
            
            reader.forEachLine { line ->
                if (line.contains("Falling back to OpenGL")) backend = "OpenGL"
                else if (line.contains("OpenCL library on this device")) backend = "OpenCL (Failed)"
                else if (line.contains("Created") && line.contains("GPU delegate kernels")) {
                    val match = Regex("Created (\\d+) GPU").find(line)
                    if (match != null) kernelCount = match.groupValues[1].toInt()
                }
            }
            
            if (kernelCount == 0) return Pair(false, backend)
            return Pair(true, backend)
        } catch (e: Exception) {
            return Pair(true, "ErrorParsingLogcat")
        }
    }

    private fun getBatteryHealth(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawHealth = intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1) ?: -1
        return when (rawHealth) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "GOOD"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "OVERHEAT"
            BatteryManager.BATTERY_HEALTH_DEAD -> "DEAD"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "OVER_VOLTAGE"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "UNSPECIFIED_FAILURE"
            BatteryManager.BATTERY_HEALTH_COLD -> "COLD"
            else -> "UNKNOWN_$rawHealth" 
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
        modelName: String,
        delegateName: String,
        numThreads: Int,
        latenciesMs: List<Double>,
        samples: List<TelemetryCollector.Sample>,
        maxLatencyMs: Double,
        batteryHealth: String
    ) {
        val rootDir = context.getExternalFilesDir(null) ?: return
        val sanitizedName = modelName.replace(".tflite", "")

        var trialNum = 1
        var trialDir = File(rootDir, "trial_data/${sanitizedName}_${delegateName.replace(" ", "")}_trial$trialNum")
        while (trialDir.exists()) {
            trialNum++
            trialDir = File(rootDir, "trial_data/${sanitizedName}_${delegateName.replace(" ", "")}_trial$trialNum")
        }
        trialDir.mkdirs()

        val latencyFile = File(trialDir, "raw_latencies.csv")
        FileWriter(latencyFile).use { writer ->
            writer.append("InferenceIndex,LatencyMs\n")
            latenciesMs.forEachIndexed { index, ms ->
                writer.append(String.format(Locale.US, "%d,%.3f\n", index, ms))
            }
        }

        val telemetryFile = File(trialDir, "telemetry.csv")
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

        val avgLatency = LatencyMath.calculateAverage(latenciesMs)
        val medianLatency = LatencyMath.calculateMedian(latenciesMs)
        val osInterferences = LatencyMath.countOSInterferences(latenciesMs)

        val peakMem = samples.maxByOrNull { it.pssKb }?.pssKb ?: 0
        val startBat = samples.firstOrNull()?.batteryPct ?: 0
        val endBat = samples.lastOrNull()?.batteryPct ?: 0
        val startTemp = samples.firstOrNull()?.temperatureC ?: 0.0
        val maxTemp = samples.maxByOrNull { it.temperatureC }?.temperatureC ?: 0.0

        metricsExporter.appendSummary(
            modelName = modelName, delegate = delegateName, threads = numThreads,
            totalInferences = latenciesMs.size, avgLatencyMs = avgLatency, medianLatencyMs = medianLatency,
            maxLatencyMs = maxLatencyMs, osInterferences = osInterferences, peakMemKb = peakMem,
            startBattery = startBat, endBattery = endBat, startTempC = startTemp, maxTempC = maxTemp,
            batteryHealth = batteryHealth, filename = "master_benchmark_results.csv"
        )
        
        metricsExporter.appendSummary(
            modelName = modelName, delegate = delegateName, threads = numThreads,
            totalInferences = latenciesMs.size, avgLatencyMs = avgLatency, medianLatencyMs = medianLatency,
            maxLatencyMs = maxLatencyMs, osInterferences = osInterferences, peakMemKb = peakMem,
            startBattery = startBat, endBattery = endBat, startTempC = startTemp, maxTempC = maxTemp,
            batteryHealth = batteryHealth, filename = "trial_data/${sanitizedName}_${delegateName.replace(" ", "")}_trial$trialNum/summary.csv"
        )
    }
}