package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.Locale

class MetricsExporter(private val context: Context) {

    companion object {
        // Now 14 columns
        const val CSV_HEADER = "Model,Delegate,Threads,TotalInferences,AvgLatencyMs,MedianLatencyMs,MaxLatencyMs,OsInterferences,PeakMemKb,BatteryPctStart,BatteryPctEnd,StartTempC,MaxTempC,BatteryHealth\n"
    }

    fun appendSummary(
        modelName: String,
        delegate: String,
        threads: Int,
        totalInferences: Int,
        avgLatencyMs: Double,
        medianLatencyMs: Double,
        maxLatencyMs: Double,
        osInterferences: Int,
        peakMemKb: Int,
        startBattery: Int,
        endBattery: Int,
        startTempC: Double,
        maxTempC: Double,
        batteryHealth: String, // Added this parameter
        filename: String = "benchmark_results.csv"
    ) {
        val dir = context.getExternalFilesDir(null) ?: return
        val file = File(dir, filename)
        val isNewFile = !file.exists()

        try {
            FileWriter(file, true).use { writer ->
                if (isNewFile) {
                    writer.append(CSV_HEADER)
                }

                val row = String.format(
                    Locale.US,
                    "%s,%s,%d,%d,%.2f,%.2f,%.2f,%d,%d,%d,%d,%.1f,%.1f,%s\n",
                    modelName, delegate, threads, totalInferences,
                    avgLatencyMs, medianLatencyMs, maxLatencyMs, osInterferences,
                    peakMemKb, startBattery, endBattery, startTempC, maxTempC, batteryHealth
                )
                writer.append(row)
            }
            Log.i("MetricsExporter", "Summary appended to ${file.absolutePath}")
        } catch (e: IOException) {
            Log.e("MetricsExporter", "Failed to write CSV: ${e.message}", e)
        }
    }
}