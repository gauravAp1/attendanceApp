package apnitor.facerecognition.app

import android.graphics.Rect
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Head pose estimator using ML Kit on ImageProxy (YUV) with rotation applied.
 * Returns Pose(yaw, pitch, roll, bounds, faceFraction[0..1]) or null if no face.
 *  yaw: +right / -left, pitch: +down / -up, roll: +cw / -ccw
 */
class HeadPoseEstimator {

    data class Pose(
        val yaw: Float,
        val pitch: Float,
        val roll: Float,
        val bounds: Rect,
        val faceFraction: Float
    )

    private val detector =
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
        )

    @OptIn(ExperimentalGetImage::class)
    fun estimate(imageProxy: ImageProxy): Pose? {
        val mediaImage = imageProxy.image ?: return null
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        return try {
            val faces = Tasks.await(detector.process(image))
            val face = faces.firstOrNull() ?: return null
            val yaw = face.headEulerAngleY     // +right, -left
            val pitch = face.headEulerAngleX   // +down , -up
            val roll = face.headEulerAngleZ    // +cw   , -ccw
            val box = face.boundingBox
            val frac = (box.width().toFloat() * box.height()) /
                    (image.width.toFloat() * image.height.toFloat())
            Pose(yaw, pitch, roll, box, frac.coerceIn(0f, 1f))
        } catch (_: Exception) {
            null
        }
    }

    fun close() { detector.close() }
}
