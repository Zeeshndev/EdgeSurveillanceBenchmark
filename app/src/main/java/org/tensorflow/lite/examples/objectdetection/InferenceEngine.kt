package org.tensorflow.lite.examples.objectdetection

import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Generalized interface to support multiple backends for JOSS compliance
interface InferenceEngine {
    fun loadModel(modelBuffer: ByteBuffer, delegateChoice: String)
    fun warmup(iterations: Int)
    fun runInference(frame: Any): Long 
}

class LiteRTEngine : InferenceEngine {
    private var interpreter: Interpreter? = null
    private var inputBuffers = arrayOf<Any>()
    private var outputBuffers = mutableMapOf<Int, Any>()

    override fun loadModel(modelBuffer: ByteBuffer, delegateChoice: String) {
        val options = Interpreter.Options()
        try {
            when (delegateChoice) {
                "GPU" -> {
                    val compatList = CompatibilityList()
                    if (compatList.isDelegateSupportedOnThisDevice) {
                        options.addDelegate(GpuDelegate(compatList.bestOptionsForThisDevice))
                        Log.i("Benchmarker", "GPU Delegate attached successfully.")
                    } else {
                        Log.w("Benchmarker", "GPU_UNSUPPORTED. Falling back to CPU.")
                        options.setNumThreads(4)
                    }
                }
                "NNAPI" -> {
                    options.setUseNNAPI(true)
                    Log.i("Benchmarker", "NNAPI Delegate attached.")
                }
                else -> {
                    options.setNumThreads(4)
                    Log.i("Benchmarker", "CPU Delegate attached (4 threads).")
                }
            }
            interpreter = Interpreter(modelBuffer, options)
            
            // CRITICAL: Force memory allocation before querying tensor sizes
            interpreter!!.allocateTensors()

            // Dynamically support models with ANY number of inputs
            val inputs = mutableListOf<ByteBuffer>()
            for (i in 0 until interpreter!!.inputTensorCount) {
                val tensor = interpreter!!.getInputTensor(i)
                inputs.add(ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder()))
            }
            inputBuffers = inputs.toTypedArray()

            // Dynamically support models with ANY number of outputs (YOLO standard)
            for (i in 0 until interpreter!!.outputTensorCount) {
                val tensor = interpreter!!.getOutputTensor(i)
                outputBuffers[i] = ByteBuffer.allocateDirect(tensor.numBytes()).order(ByteOrder.nativeOrder())
            }
            
        } catch (e: Exception) {
            Log.e("Benchmarker", "DELEGATE_INIT_FAILURE: $delegateChoice, ${e.stackTraceToString()}")
            throw e 
        }
    }

    override fun warmup(iterations: Int) {
        if (interpreter == null) return
        
        for (i in 0 until iterations) {
            prepareBuffers()
            // runForMultipleInputsOutputs safely handles multi-tensor YOLO architectures
            interpreter!!.runForMultipleInputsOutputs(inputBuffers, outputBuffers)
        }
    }

    override fun runInference(frame: Any): Long {
        if (interpreter == null) return 0L
        
        prepareBuffers()

        val startTime = System.nanoTime()
        interpreter!!.runForMultipleInputsOutputs(inputBuffers, outputBuffers)
        // Sanity check: Read the first float from the first output tensor
val firstOutputBuffer = outputBuffers[0] as ByteBuffer
val firstValue = firstOutputBuffer.getFloat(0) 
Log.d("Telemetry", "Output Sanity Check - Tensor 0, Index 0: $firstValue")
        val endTime = System.nanoTime()

        return (endTime - startTime) / 1_000_000 
    }

    private fun prepareBuffers() {
        // TFLite consumes buffers; use clear() to reset position to 0 before every single frame
        inputBuffers.forEach { (it as ByteBuffer).clear() }
        outputBuffers.values.forEach { (it as ByteBuffer).clear() }
    }
}