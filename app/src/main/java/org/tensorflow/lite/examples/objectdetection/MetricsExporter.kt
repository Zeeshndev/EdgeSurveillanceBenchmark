package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MetricsExporter(private val context: Context) {
    fun exportToCsv(result: BenchmarkResult, filename: String = "benchmark_results.csv") {
        // Saves to the app's external files directory (accessible via Android Studio/ADB for paper data extraction)
        val file = File(context.getExternalFilesDir(null), filename)
        val isNewFile = !file.exists()

        try {
            FileWriter(file, true).use { writer ->
                // Write standard CSV header if it is a brand new file
                if (isNewFile) {
                    writer.append("Model,Delegate,Threads,Warmup,Inference,AvgLatencyMs,PeakMemKb,BatteryMicroAmps,ThermalThrottling,Crash,Error\n")
                }
                
                // Append the benchmark data row
                writer.append("${result.config.modelName},")
                writer.append("${result.config.delegate},")
                writer.append("${result.config.numThreads},")
                writer.append("${result.config.warmupIterations},")
                writer.append("${result.config.inferenceIterations},")
                writer.append("${result.averageLatencyMs},")
                writer.append("${result.peakMemoryPssKb},")
                writer.append("${result.batteryDrainMicroAmps},")
                writer.append("${result.thermalThrottlingOccurred},")
                writer.append("${result.gpuCrashCaptured},")
                writer.append("${result.errorMessage ?: "None"}\n")
            }
            Log.i("MetricsExporter", "Results successfully appended to ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("MetricsExporter", "Failed to write CSV results: ${e.message}")
        }
    }
}