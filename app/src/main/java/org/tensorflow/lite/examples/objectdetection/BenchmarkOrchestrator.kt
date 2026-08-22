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

    fun runBenchmark(config: BenchmarkConfig, modelBuffer: ByteBuffer) {
        benchmarkExecutor.execute {
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