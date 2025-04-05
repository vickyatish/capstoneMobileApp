package com.example.capstone.detection

import android.content.Context
import android.util.Log

/**
 * Concrete detector implementation for Sign Detection (regressionmodel.tflite).
 */
class SignDetector(
    context: Context,
    modelPath: String = "sidewalkmodel.tflite", // Default path for this detector
    threshold: Float = 0.5f, // Can override defaults
    numThreads: Int = 2,
    maxResults: Int = 5
) : Detector(context, modelPath, threshold, numThreads, maxResults) {

    init {
        Log.i(TAG, "SignDetector initialized with model: $modelPath")
        // If SignDetector needs specific initialization beyond the base class, add it here.
        // For example, loading specific labels if not included in metadata.
    }

    // Override methods if SignDetector needs specific behavior different from base Detector
    // override fun detect(image: Bitmap, imageRotation: Int): List<Detection>? {
    //     // Custom detection logic if needed
    //     return super.detect(image, imageRotation) // Or call base implementation
    // }

    companion object {
        // Specific tag for this detector if needed, otherwise uses DetectorBase.TAG
        const val TAG = "SignDetector"
    }
}