package org.tensorflow.lite.examples.objectdetection;

import java.lang.System;

@kotlin.Metadata(mv = {1, 6, 0}, k = 1, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\u0003\n\u0002\u0010\b\n\u0000\bf\u0018\u00002\u00020\u0001J\u0018\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u0007H&J\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u0001H&J\u0010\u0010\u000b\u001a\u00020\u00032\u0006\u0010\f\u001a\u00020\rH&\u00a8\u0006\u000e"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/InferenceEngine;", "", "loadModel", "", "modelBuffer", "Ljava/nio/ByteBuffer;", "delegateChoice", "", "runInference", "", "frame", "warmup", "iterations", "", "app_debug"})
public abstract interface InferenceEngine {
    
    public abstract void loadModel(@org.jetbrains.annotations.NotNull
    java.nio.ByteBuffer modelBuffer, @org.jetbrains.annotations.NotNull
    java.lang.String delegateChoice);
    
    public abstract void warmup(int iterations);
    
    public abstract long runInference(@org.jetbrains.annotations.NotNull
    java.lang.Object frame);
}