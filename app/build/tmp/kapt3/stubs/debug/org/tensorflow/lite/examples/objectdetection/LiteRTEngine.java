package org.tensorflow.lite.examples.objectdetection;

import java.lang.System;

@kotlin.Metadata(mv = {1, 6, 0}, k = 1, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0011\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010%\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\t\n\u0002\b\u0004\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\u0018\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011H\u0016J\b\u0010\u0012\u001a\u00020\rH\u0002J\u0010\u0010\u0013\u001a\u00020\u00142\u0006\u0010\u0015\u001a\u00020\u0005H\u0016J\u0010\u0010\u0016\u001a\u00020\r2\u0006\u0010\u0017\u001a\u00020\u000bH\u0016R\u0016\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082\u000e\u00a2\u0006\u0004\n\u0002\u0010\u0006R\u0010\u0010\u0007\u001a\u0004\u0018\u00010\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001a\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00050\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0018"}, d2 = {"Lorg/tensorflow/lite/examples/objectdetection/LiteRTEngine;", "Lorg/tensorflow/lite/examples/objectdetection/InferenceEngine;", "()V", "inputBuffers", "", "", "[Ljava/lang/Object;", "interpreter", "Lorg/tensorflow/lite/Interpreter;", "outputBuffers", "", "", "loadModel", "", "modelBuffer", "Ljava/nio/ByteBuffer;", "delegateChoice", "", "prepareBuffers", "runInference", "", "frame", "warmup", "iterations", "app_debug"})
public final class LiteRTEngine implements org.tensorflow.lite.examples.objectdetection.InferenceEngine {
    private org.tensorflow.lite.Interpreter interpreter;
    private java.lang.Object[] inputBuffers = {};
    private java.util.Map<java.lang.Integer, java.lang.Object> outputBuffers;
    
    public LiteRTEngine() {
        super();
    }
    
    @java.lang.Override
    public void loadModel(@org.jetbrains.annotations.NotNull
    java.nio.ByteBuffer modelBuffer, @org.jetbrains.annotations.NotNull
    java.lang.String delegateChoice) {
    }
    
    @java.lang.Override
    public void warmup(int iterations) {
    }
    
    @java.lang.Override
    public long runInference(@org.jetbrains.annotations.NotNull
    java.lang.Object frame) {
        return 0L;
    }
    
    private final void prepareBuffers() {
    }
}