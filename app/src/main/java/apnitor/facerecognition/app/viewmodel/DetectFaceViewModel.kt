package apnitor.facerecognition.app.viewmodel


import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import apnitor.facerecognition.app.FaceDetection.ImageVectorUseCase
import apnitor.facerecognition.app.FaceDetection.PersonUseCase
import apnitor.facerecognition.app.database.RecognitionMetrics
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DetectScreenViewModel @Inject constructor(
    val personUseCase: PersonUseCase,
    val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {
    val faceDetectionMetricsState = mutableStateOf<RecognitionMetrics?>(null)


    fun getNumPeople(): Long = personUseCase.getCount()
}