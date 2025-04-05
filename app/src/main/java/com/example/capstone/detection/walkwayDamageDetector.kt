package com.example.capstone.detection

import android.content.Context
import android.util.Log

/**
 * Concrete detector implementation for Walkway Damage (sidewalkmodel.tflite).
 */
class WalkwayDamageDetector(
    context: Context,
    modelPath: String = "sidewalkmodel.tflite", // Default path for this detector
    threshold: Float = 0.5f, // Can override defaults
    numThreads: Int = 2,
    maxResults: Int = 5 // Maybe allow more results for damage detection?
) : Detector(context, modelPath, threshold, numThreads, maxResults) {

    init {
        Log.i(TAG, "WalkwayDamageDetector initialized with model: $modelPath")
        // If WalkwayDamageDetector needs specific initialization, add it here.
    }

    // Override methods if WalkwayDamageDetector needs specific behavior
    // override fun detect(image: Bitmap, imageRotation: Int): List<Detection>? {
    //     // Custom detection logic if needed
    //     return super.detect(image, imageRotation)
    // }

    companion object {
        // Specific tag for this detector if needed
        const val TAG = "WalkwayDamageDetector"
    }
}