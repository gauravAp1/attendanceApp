package apnitor.facerecognition.app.viewmodel

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import apnitor.facerecognition.app.FaceDetection.AppException
import apnitor.facerecognition.app.FaceDetection.ImageVectorUseCase
import apnitor.facerecognition.app.FaceDetection.PersonUseCase
import apnitor.facerecognition.app.components.setProgressDialogText
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddFaceScreenViewModel @Inject constructor(
    private val personUseCase: PersonUseCase,
    private val imageVectorUseCase: ImageVectorUseCase,
) : ViewModel() {
    val personNameState: MutableState<String> = mutableStateOf("")
    val selectedImageURIs: MutableState<List<Uri>> = mutableStateOf(emptyList())

    val isProcessingImages: MutableState<Boolean> = mutableStateOf(false)
    val numImagesProcessed: MutableState<Int> = mutableIntStateOf(0)

    fun addImages() {
        isProcessingImages.value = true
        CoroutineScope(Dispatchers.Default).launch {
            val id =
                personUseCase.addPerson(
                    personNameState.value,
                    selectedImageURIs.value.size.toLong(),
                )
            selectedImageURIs.value.forEach {
                imageVectorUseCase
                    .addImage(id, personNameState.value, it)
                    .onFailure {
                        val errorMessage = (it as AppException).errorCode.message
                        setProgressDialogText(errorMessage)
                    }.onSuccess {
                        numImagesProcessed.value += 1
                        setProgressDialogText("Processed ${numImagesProcessed.value} image(s)")
                    }
            }
            isProcessingImages.value = false
        }
    }
}
