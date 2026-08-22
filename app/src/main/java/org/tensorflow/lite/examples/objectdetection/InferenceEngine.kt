package org.tensorflow.lite.examples.objectdetection

import android.util.Log
import com.google.ai.edge.litert.Interpreter
import com.google.ai.edge.litert.gpu.CompatibilityList
import com.google.ai.edge.litert.gpu.GpuDelegate
import java.nio.ByteBuffer

// Generalized interface to support multiple backends for JOSS compliance
interface InferenceEngine {
    fun loadModel(modelBuffer: ByteBuffer, delegateChoice: String)
    fun warmup(iterations: Int)
    fun runInference(frame: Any): Long 
}

class LiteRTEngine : InferenceEngine {
    private var interpreter: Interpreter? = null

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
                        Log.w("Benchmarker", "GPU_UNSUPPORTED. Falling back to CPU (4 threads).")
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
            
        } catch (e: Exception) {
            // CRITICAL: Gracefully captures the YOLOv11n GPU delegate crash for your TMLR paper
            Log.e("Benchmarker", "DELEGATE_INIT_FAILURE: $delegateChoice, ${e.stackTraceToString()}")
            throw e 
        }
    }

    override fun warmup(iterations: Int) {
        // Implementation coming next
    }

    override fun runInference(frame: Any): Long {
        // Implementation coming next
        return 0L
    }
}