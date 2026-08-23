package org.tensorflow.lite.examples.objectdetection;

import java.lang.System;

@kotlin.Metadata(mv = {1, 6, 0}, k = 1, d1 = {"\u0000.\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0006\n\u0000\n\u0002\u0010\b\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u001a\b\u0086\b\u0018\u00002\u00020\u0001BA\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0007\u0012\u0006\u0010\t\u001a\u00020\n\u0012\u0006\u0010\u000b\u001a\u00020\n\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\r\u00a2\u0006\u0002\u0010\u000eJ\t\u0010\u001b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u001c\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u001d\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u001e\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u001f\u001a\u00020\nH\u00c6\u0003J\t\u0010 \u001a\u00020\nH\u00c6\u0003J\u000b\u0010!\u001a\u0004\u0018\u00010\rH\u00c6\u0003JQ\u0010\"\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\b\b\u0002\u0010\t\u001a\u00020\n2\b\b\u0002\u0010\u000b\u001a\u00020\n2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\rH\u00c6\u0001J\u0013\u0010#\u001a\u00020\n2\b\u0010$\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010%\u001a\u00020\u0007H\u00d6\u0001J\t\u0010&\u001a\u00020\rH\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0013\u0010\f\u001a\u0004\u0018\u00010\r\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0015\u0010\u0016R\u0011\u0010\u000b\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0019\u0010\u0012R\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u0018\u00a8\u0006\'"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/BenchmarkResult;", "", "config", "Lorg/tensorflow/lite/examples/objectdetection/BenchmarkConfig;", "averageLatencyMs", "", "peakMemoryPssKb", "", "batteryDrainMicroAmps", "thermalThrottlingOccurred", "", "gpuCrashCaptured", "errorMessage", "", "(Lorg/tensorflow/lite/examples/objectdetection/BenchmarkConfig;DIIZZLjava/lang/String;)V", "getAverageLatencyMs", "()D", "getBatteryDrainMicroAmps", "()I", "getConfig", "()Lorg/tensorflow/lite/examples/objectdetection/BenchmarkConfig;", "getErrorMessage", "()Ljava/lang/String;", "getGpuCrashCaptured", "()Z", "getPeakMemoryPssKb", "getThermalThrottlingOccurred", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "copy", "equals", "other", "hashCode", "toString", "app_debug"})
public final class BenchmarkResult {
    @org.jetbrains.annotations.NotNull
    private final org.tensorflow.lite.examples.objectdetection.BenchmarkConfig config = null;
    private final double averageLatencyMs = 0.0;
    private final int peakMemoryPssKb = 0;
    private final int batteryDrainMicroAmps = 0;
    private final boolean thermalThrottlingOccurred = false;
    private final boolean gpuCrashCaptured = false;
    @org.jetbrains.annotations.Nullable
    private final java.lang.String errorMessage = null;
    
    @org.jetbrains.annotations.NotNull
    public final org.tensorflow.lite.examples.objectdetection.BenchmarkResult copy(@org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.BenchmarkConfig config, double averageLatencyMs, int peakMemoryPssKb, int batteryDrainMicroAmps, boolean thermalThrottlingOccurred, boolean gpuCrashCaptured, @org.jetbrains.annotations.Nullable
    java.lang.String errorMessage) {
        return null;
    }
    
    @java.lang.Override
    public boolean equals(@org.jetbrains.annotations.Nullable
    java.lang.Object other) {
        return false;
    }
    
    @java.lang.Override
    public int hashCode() {
        return 0;
    }
    
    @org.jetbrains.annotations.NotNull
    @java.lang.Override
    public java.lang.String toString() {
        return null;
    }
    
    public BenchmarkResult(@org.jetbrains.annotations.NotNull
    org.tensorflow.lite.examples.objectdetection.BenchmarkConfig config, double averageLatencyMs, int peakMemoryPssKb, int batteryDrainMicroAmps, boolean thermalThrottlingOccurred, boolean gpuCrashCaptured, @org.jetbrains.annotations.Nullable
    java.lang.String errorMessage) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final org.tensorflow.lite.examples.objectdetection.BenchmarkConfig component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final org.tensorflow.lite.examples.objectdetection.BenchmarkConfig getConfig() {
        return null;
    }
    
    public final double component2() {
        return 0.0;
    }
    
    public final double getAverageLatencyMs() {
        return 0.0;
    }
    
    public final int component3() {
        return 0;
    }
    
    public final int getPeakMemoryPssKb() {
        return 0;
    }
    
    public final int component4() {
        return 0;
    }
    
    public final int getBatteryDrainMicroAmps() {
        return 0;
    }
    
    public final boolean component5() {
        return false;
    }
    
    public final boolean getThermalThrottlingOccurred() {
        return false;
    }
    
    public final boolean component6() {
        return false;
    }
    
    public final boolean getGpuCrashCaptured() {
        return false;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String component7() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.String getErrorMessage() {
        return null;
    }
}