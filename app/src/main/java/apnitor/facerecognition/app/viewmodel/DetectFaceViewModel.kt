package apnitor.facerecognition.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import apnitor.facerecognition.app.FaceDetection.ImageVectorUseCase
import apnitor.facerecognition.app.FaceDetection.PersonUseCase
import apnitor.facerecognition.app.database.AttendanceUseCase
import apnitor.facerecognition.app.database.RecognitionMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetectScreenViewModel @Inject constructor(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
    private val attendanceUseCase: AttendanceUseCase
) : ViewModel() {

    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)

    // Single dialog string you can show in UI (Snackbar/AlertDialog)
    val dialogMessage = mutableStateOf<String?>(null)

    // simple re-trigger protection while we show dialog / mark attendance
    private var marking = false

    // Debounce same person name within a short window
    private var lastMarkTs: Long = 0
    private var lastName: String? = null

    fun getNumPeople(): Long = personUseCase.getCount()

    /**
     * Called by the overlay when a real person is recognized (non-spoof).
     * Uses the person's *name* to mark attendance; personId is available if you
     * later switch AttendanceUseCase to use IDs.
     */
    fun onRecognized(personId: Long, personName: String) {
        val now = System.currentTimeMillis()
        if (personName.isBlank() || personName == "Not recognized") return

        // debounce: same name in <2s -> ignore
        if (lastName == personName && (now - lastMarkTs) < 2000) return
        lastName = personName
        lastMarkTs = now

        if (marking) return
        marking = true

        viewModelScope.launch {
            try {
                val msg = attendanceUseCase.mark(personName) // returns String? for dialog
                if (msg != null) {
                    dialogMessage.value = msg
                }
                // small cooldown prevents rapid double marks
                delay(1500)
            } finally {
                marking = false
            }
        }
    }

    fun dismissDialog() {
        dialogMessage.value = null
    }
}
