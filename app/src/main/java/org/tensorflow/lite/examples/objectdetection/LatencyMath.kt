package org.tensorflow.lite.examples.objectdetection

object LatencyMath {
    // OS Interference Threshold: Any frame > 500ms is a garbage collection or OS thermal interrupt, not a neural net delay.
    private const val OUTLIER_THRESHOLD_MS = 500.0 

    fun calculateAverage(latencies: List<Double>): Double {
        val clean = latencies.filter { it <= OUTLIER_THRESHOLD_MS }
        return if (clean.isNotEmpty()) clean.average() else 0.0
    }

    fun calculateMedian(latencies: List<Double>): Double {
        val clean = latencies.filter { it <= OUTLIER_THRESHOLD_MS }.sorted()
        if (clean.isEmpty()) return 0.0
        return if (clean.size % 2 == 0) {
            (clean[clean.size / 2 - 1] + clean[clean.size / 2]) / 2.0
        } else {
            clean[clean.size / 2]
        }
    }
    
    fun countOSInterferences(latencies: List<Double>): Int {
        return latencies.count { it > OUTLIER_THRESHOLD_MS }
    }
}