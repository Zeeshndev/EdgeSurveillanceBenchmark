package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory

class DatasetFeeder(private val context: Context, private val datasetFolder: String = "dataset") {
    private val cachedFrames = mutableListOf<Bitmap>()
    private var currentIndex = 0

    init {
        // Fetch only .jpg files and limit to 5 frames to conserve RAM
        val allFiles = context.assets.list(datasetFolder)?.toList() ?: emptyList()
        val imageFiles = allFiles.filter { it.endsWith(".jpg", ignoreCase = true) }.take(50)
        
        require(imageFiles.isNotEmpty()) { "No .jpg files found!" }

        // Pre-decode frames into RAM ONCE so we don't leak memory in the inference loop
        val options = BitmapFactory.Options().apply { 
            inPreferredConfig = Bitmap.Config.ARGB_8888 
        }
        
        for (fileName in imageFiles) {
            context.assets.open("$datasetFolder/$fileName").use { inputStream ->
                val bitmap = BitmapFactory.decodeStream(inputStream, null, options)
                if (bitmap != null) {
                    cachedFrames.add(bitmap)
                }
            }
        }
    }

    fun getNextFrame(): Bitmap {
        val bitmap = cachedFrames[currentIndex]
        // Cycle back to the first frame infinitely
        currentIndex = (currentIndex + 1) % cachedFrames.size
        return bitmap
    }
}