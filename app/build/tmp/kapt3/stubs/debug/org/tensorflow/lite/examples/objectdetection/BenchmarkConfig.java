package org.tensorflow.lite.examples.objectdetection;

import java.lang.System;

@kotlin.Metadata(mv = {1, 6, 0}, k = 1, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0011\n\u0002\u0010\u000b\n\u0002\b\u0004\b\u0086\b\u0018\u00002\u00020\u0001B7\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\u0006\u0012\b\b\u0002\u0010\b\u001a\u00020\u0006\u00a2\u0006\u0002\u0010\tJ\t\u0010\u0011\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0006H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0006H\u00c6\u0003J;\u0010\u0016\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\u00062\b\b\u0002\u0010\b\u001a\u00020\u0006H\u00c6\u0001J\u0013\u0010\u0017\u001a\u00020\u00182\b\u0010\u0019\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001a\u001a\u00020\u0006H\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0004\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000bR\u0011\u0010\b\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000bR\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\rR\u0011\u0010\u0007\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\r\u00a8\u0006\u001c"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/BenchmarkConfig;", "", "modelName", "", "delegate", "numThreads", "", "warmupIterations", "inferenceIterations", "(Ljava/lang/String;Ljava/lang/String;III)V", "getDelegate", "()Ljava/lang/String;", "getInferenceIterations", "()I", "getModelName", "getNumThreads", "getWarmupIterations", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "", "other", "hashCode", "toString", "app_debug"})
public final class BenchmarkConfig {
    @org.jetbrains.annotations.NotNull
    private final java.lang.String modelName = null;
    @org.jetbrains.annotations.NotNull
    private final java.lang.String delegate = null;
    private final int numThreads = 0;
    private final int warmupIterations = 0;
    private final int inferenceIterations = 0;
    
    @org.jetbrains.annotations.NotNull
    public final org.tensorflow.lite.examples.objectdetection.BenchmarkConfig copy(@org.jetbrains.annotations.NotNull
    java.lang.String modelName, @org.jetbrains.annotations.NotNull
    java.lang.String delegate, int numThreads, int warmupIterations, int inferenceIterations) {
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
    
    public BenchmarkConfig() {
        super();
    }
    
    public BenchmarkConfig(@org.jetbrains.annotations.NotNull
    java.lang.String modelName, @org.jetbrains.annotations.NotNull
    java.lang.String delegate, int numThreads, int warmupIterations, int inferenceIterations) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component1() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getModelName() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String component2() {
        return null;
    }
    
    @org.jetbrains.annotations.NotNull
    public final java.lang.String getDelegate() {
        return null;
    }
    
    public final int component3() {
        return 0;
    }
    
    public final int getNumThreads() {
        return 0;
    }
    
    public final int component4() {
        return 0;
    }
    
    public final int getWarmupIterations() {
        return 0;
    }
    
    public final int component5() {
        return 0;
    }
    
    public final int getInferenceIterations() {
        return 0;
    }
}