package org.tensorflow.lite.examples.objectdetection;

import java.lang.System;

@kotlin.Metadata(mv = {1, 6, 0}, k = 1, d1 = {"\u0000<\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u0018\u00002\u00020\u0001B%\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\t\u00a2\u0006\u0002\u0010\nJ\u0016\u0010\r\u001a\u00020\u000e2\u0006\u0010\u000f\u001a\u00020\u00102\u0006\u0010\u0011\u001a\u00020\u0012R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/BenchmarkOrchestrator;", "", "context", "Landroid/content/Context;", "engine", "Lorg/tensorflow/lite/examples/objectdetection/InferenceEngine;", "telemetry", "Lorg/tensorflow/lite/examples/objectdetection/TelemetryCollector;", "exporter", "Lorg/tensorflow/lite/examples/objectdetection/MetricsExporter;", "(Landroid/content/Context;Lorg/tensorflow/lite/examples/objectdetection/InferenceEngine;Lorg/tensorflow/lite/examples/objectdetection/TelemetryCollector;Lorg/tensorflow/lite/examples/objectdetection/MetricsExporter;)V", "benchmarkExecutor", "Ljava/util/concurrent/Executor;", "runBenchmark", "", "config", "Lorg/tensorflow/lite/examples/objectdetection/BenchmarkConfig;", "modelBuffer", "Ljava/nio/ByteBuffer;", "app_debug"})
public final class BenchmarkOrchestrator {
    private final android.content.Context context = null;
    private final org.tensorflow.lite.examples.objectdetection.InferenceEngine engine = null;
    private final org.tensorflow.lite.examples.objectdetection.TelemetryCollector telemetry = null;
    private final org.tensorflow.lite.examples.objectdetection.MetricsExporter exporter = null;
    private final java.util.concurrent.Executor benchmarkExecutor = null;
    
    public BenchmarkOrchestrator(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.InferenceEngine engine, @org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.TelemetryCollector telemetry, @org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.MetricsExporter exporter) {
        super();
    }
    
    public final void runBenchmark(@org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.BenchmarkConfig config, @org.jetbrains.annotations.NotNull
    java.nio.ByteBuffer modelBuffer) {
    }
}