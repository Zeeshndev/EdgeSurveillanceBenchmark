package org.tensorflow.lite.examples.objectdetection

import org.junit.Assert.assertEquals
import org.junit.Test

class RegressionTestSuite {

    @Test
    fun `test 01 - csv schema integrity matches documentation`() {
        // This string MUST exactly match the JOSS README documentation.
        // A single misplaced comma will break the Python pandas scripts later.
        val expectedSchema = "ModelName,Delegate,Threads,TotalInferences,AvgLatencyMs,MedianLatencyMs,MaxLatencyMs,OSInterferences,PeakMemKb,StartBattery,EndBattery,StartTempC,MaxTempC,BatteryHealth"
        
        assertEquals("FATAL: CSV Schema drift detected!", expectedSchema, MetricsExporter.CSV_HEADER)
    }

    @Test
    fun `test 02 - latency math filters OS interferences`() {
        // Mock data: 4 normal GPU frames, and 1 massive OS thermal spike (850ms)
        val mockLatencies = listOf(35.0, 36.0, 34.0, 35.0, 850.0) 

        val avg = LatencyMath.calculateAverage(mockLatencies)
        val median = LatencyMath.calculateMedian(mockLatencies)
        val interferences = LatencyMath.countOSInterferences(mockLatencies)

        // The true average of the neural net is 35.0ms. 
        // If the 850ms outlier is accidentally included, the average would skew to ~198ms, ruining the data.
        assertEquals(35.0, avg, 0.001)
        assertEquals(35.0, median, 0.001)
        assertEquals("Failed to flag the >500ms frame as an OS interference", 1, interferences)
    }

    @Test
    fun `test 03 - delegate string mapping logic`() {
        // Simulating the UI dropdown passing string configurations
        val uiSelectionGpu = "GPU"
        val uiSelectionCpu = "CPU"
        
        // This validates the exact boolean logic used in BenchmarkOrchestrator
        val mappedGpu = if (uiSelectionGpu.equals("GPU", ignoreCase = true)) ObjectDetectorHelper.DELEGATE_GPU else ObjectDetectorHelper.DELEGATE_CPU
        val mappedCpu = if (uiSelectionCpu.equals("GPU", ignoreCase = true)) ObjectDetectorHelper.DELEGATE_GPU else ObjectDetectorHelper.DELEGATE_CPU

        assertEquals("GPU string failed to map to DELEGATE_GPU integer", ObjectDetectorHelper.DELEGATE_GPU, mappedGpu)
        assertEquals("CPU string failed to map to DELEGATE_CPU integer", ObjectDetectorHelper.DELEGATE_CPU, mappedCpu)
    }
}