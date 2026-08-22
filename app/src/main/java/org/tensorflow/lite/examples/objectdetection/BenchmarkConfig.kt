package org.tensorflow.lite.examples.objectdetection

// Defines the strictly reproducible parameters for a benchmark run
data class BenchmarkConfig(
    val modelName: String = "yolov11n_float16.tflite",
    val delegate: String = "GPU", // Options: CPU, GPU, NNAPI
    val numThreads: Int = 4,
    val warmupIterations: Int = 10, // Crucial for stabilizing thermal loads
    val inferenceIterations: Int = 100
)

// Standardized output schema to ensure clean JSON/CSV export for your paper
data class BenchmarkResult(
    val config: BenchmarkConfig,
    val averageLatencyMs: Double,
    val peakMemoryPssKb: Int,
    val batteryDrainMicroAmps: Int,
    val thermalThrottlingOccurred: Boolean,
    val gpuCrashCaptured: Boolean,
    val errorMessage: String? = null
)