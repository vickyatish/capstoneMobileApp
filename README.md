# Android Object Detection App

This Android application utilizes TensorFlow Lite to perform object detection. It is designed with an abstract `Detector` class to allow for easy switching between different detection models.


![Sample image](<https://media-hosting.imagekit.io/eb75008ef2914ac9/Screenshot%202025-04-05%20at%2012.59.55%E2%80%AFPM.png?Expires=1838492013&Key-Pair-Id=K2ZIVPTIP2VGHC&Signature=k4rYD6VO3wifCUl9NkA5hdiFNht2wz4FLGDYzN-sF98PCYjKTBKwyeiGMV19nekCKPhhRt8qO7-xvc5PvzMNvq~9ieM7vGa9uQP5WcafiQbk-83v3yQcMOdDmHQr1IMR99aKHsHy3-cbq8vqZ8VL0zbk9bFvF~-boTYXK0QXT8DXI2M51088OQJfxsEUJSbZb8mOUZ2Vq0OWP1a59-lU15j8cpn8sj7SFyz3VJlX7~laWu8n2H4HW-hek39fVSpJE2~0tZ7RiLErBuCfYXBkpaQ-a-igb8UhxryBLP85QyQKtN7qKeRRJkGn2LMS1nqLWqB2TIUOVnqLRMJzRnGFpA__>)

## Project Structure

The core structure of this project is based on the following class design:

* **`Detector` (Abstract Class)**: This class defines the common interface for all detector subclasses. It includes the following abstract methods:
    * `restart()`
    * `init()`
    * `create()`
    * `detect()` [cite: 1, 2, 3]
* **`SignDetector` (Subclass of `Detector`)**:  This class implements the `Detector` interface to detect signs. [cite: 2]
* **`WalkwayDamageDetector` (Subclass of `Detector`)**: This class implements the `Detector` interface to detect walkway damage. [cite: 3]

This design allows for polymorphic behavior, where the specific detection logic is determined at runtime. [cite: 5, 6, 9]

## Implementation Details

The application's main logic resides within the `MainActivity` (or a similar component), where detector instances are created and used.

```kotlin
lateinit var detector: Detector
lateinit var signDetector: SignDetector
lateinit var walkwayDamageDetector: WalkwayDamageDetector

fun setupDetectors() {
    // Pass in real constructor args as needed
    signDetector  SignDetector(/* ... */)
    walkwayDamageDetector  WalkwayDamageDetector(/* ... */)

    val useSignDetection = true // or some switch/flag

    detector = if (useSignDetection) {
        signDetector
    } else {
        walkwayDamageDetector
    }

    // Later in your code:
    detector.init()
    detector.detect()
}
```


## Key Points:

Kotlin's polymorphism allows us to assign subclasses (SignDetector, WalkwayDamageDetector) to a superclass variable (detector) without casting.
Method calls on the detector instance will be dynamically dispatched to the correct subclass implementation.
Constructors in the subclass can accept arguments, such as context and model path.
TensorFlow Lite Models
This application uses two TensorFlow Lite models for object detection:

'sidewalkmodel.tflite'
'regressionmodel.tflite'
Important: These models are renamed detection models with metadata downloaded from TensorFlow Hub. Ensure that these files are placed in the appropriate directory within the Android project before running the application.

## Note on Model Recognition:

It is likely that issues encountered with the app not recognizing the intended model earlier were due to missing metadata or the app's inability to properly read the metadata of the TFLite models.

Additional Considerations
Model Instancing: The document raises the question of single vs. dual model instancing.
Dual Model: Faster switching but higher GPU usage.
Single Model: Efficient GPU usage but slower switching.    
Further development should consider the optimal approach for the specific use case.

## Author
Yatish Sekaran