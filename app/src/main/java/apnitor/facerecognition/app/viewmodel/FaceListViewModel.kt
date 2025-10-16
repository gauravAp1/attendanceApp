package apnitor.facerecognition.app.viewmodel

import androidx.lifecycle.ViewModel
import apnitor.facerecognition.app.FaceDetection.ImageVectorUseCase
import apnitor.facerecognition.app.FaceDetection.PersonUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class FaceListScreenViewModel @Inject constructor(
    val imageVectorUseCase: ImageVectorUseCase,
    val personUseCase: PersonUseCase,
) : ViewModel() {
    val personFlow = personUseCase.getAll()

    // Remove the person from `PersonRecord`
    // and all associated face embeddings from `FaceImageRecord`
    fun removeFace(id: Long) {
        personUseCase.removePerson(id)
        imageVectorUseCase.removeImages(id)
    }
}