package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException

class MetricsExporter(private val context: Context) {
    fun exportToCsv(result: BenchmarkResult, filename: String = "benchmark_results.csv") {
        // Saves to the app's external files directory (accessible via Android Studio/ADB for paper data extraction)
        val file = File(context.getExternalFilesDir(null), filename)
        val isNewFile = !file.exists()

        try {
            // Write standard CSV header if it is a brand new file
            if (isNewFile) {
                file.appendText("Model,Delegate,Threads,Warmup,Inference,AvgLatencyMs,PeakMemKb,BatteryMicroAmps,ThermalThrottling,Crash,Error\n")
            }
            
            // Build and append the benchmark summary data row
            val csvRow = "${result.config.modelName}," +
                    "${result.config.delegate}," +
                    "${result.config.numThreads}," +
                    "${result.config.warmupIterations}," +
                    "${result.config.inferenceIterations}," +
                    "${result.averageLatencyMs}," +
                    "${result.peakMemoryPssKb}," +
                    "${result.batteryDrainMicroAmps}," +
                    "${result.thermalThrottlingOccurred}," +
                    "${result.gpuCrashCaptured}," +
                    "${result.errorMessage ?: "None"}\n"
            
            file.appendText(csvRow)
            
            Log.i("MetricsExporter", "Results successfully appended to ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("MetricsExporter", "Failed to write CSV results: ${e.message}")
        }
    }
}