package apnitor.facerecognition.app.viewmodel

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import apnitor.facerecognition.app.FaceNet
import apnitor.facerecognition.app.database.FaceImageRecord
import apnitor.facerecognition.app.database.ImagesVectorDB
import apnitor.facerecognition.app.FaceDetection.PersonUseCase

@HiltViewModel
class AddFaceScreenViewModel @Inject constructor(
    private val personUseCase: PersonUseCase,
    private val faceNet: FaceNet,
    private val imagesVectorDB: ImagesVectorDB
) : ViewModel() {

    // ML Kit detector for enrollment-time face crop
    private val detector: FaceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                .enableTracking()
                .build()
        )
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }

    /**
     * Called with RAW, unmirrored, rotation-corrected bitmaps (from AddFaceScreen).
     * 1) Detect largest face in each bitmap and crop it.
     * 2) Compute FaceNet embedding on each crop.
     * 3) Create a single PersonRecord and persist all embeddings.
     */
    suspend fun saveApprovedBitmaps(personName: String, bitmaps: List<Bitmap>) {
        if (bitmaps.isEmpty()) return

        // 1) Crop faces
        val crops: List<Bitmap> = withContext(Dispatchers.Default) {
            bitmaps.mapNotNull { cropLargestFace(it) }
        }
        if (crops.isEmpty()) return

        // 2) Embeddings on crops (sequential to keep TFLite interpreter safe)
        val embeddings: List<FloatArray> = withContext(Dispatchers.Default) {
            crops.map { faceNet.getFaceEmbedding(it) }
        }

        // 3) Create person and persist embeddings
        val personId = personUseCase.addPerson(
            name = personName,
            numImages = embeddings.size.toLong()
        )
        embeddings.forEach { emb ->
            imagesVectorDB.addFaceImageRecord(
                FaceImageRecord(
                    personID = personId,
                    personName = personName,
                    faceEmbedding = emb
                )
            )
        }
    }

    private fun cropLargestFace(source: Bitmap): Bitmap? {
        val faces = try {
            Tasks.await(detector.process(InputImage.fromBitmap(source, 0)))
        } catch (_: Exception) {
            emptyList()
        }
        if (faces.isEmpty()) return null
        val largest = faces.maxBy { it.boundingBox.width() * it.boundingBox.height() }
        val box = clampRect(largest.boundingBox, source.width, source.height)
        if (box.width() <= 0 || box.height() <= 0) return null

        return try {
            Bitmap.createBitmap(source, box.left, box.top, box.width(), box.height())
        } catch (_: Exception) {
            null
        }
    }

    private fun clampRect(r: Rect, w: Int, h: Int): Rect {
        val left = r.left.coerceIn(0, (w - 1).coerceAtLeast(0))
        val top = r.top.coerceIn(0, (h - 1).coerceAtLeast(0))
        val right = r.right.coerceIn(left + 1, w)
        val bottom = r.bottom.coerceIn(top + 1, h)
        return Rect(left, top, right, bottom)
    }
}
