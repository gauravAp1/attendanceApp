package apnitor.facerecognition.app.FaceDetection

import apnitor.facerecognition.app.database.PersonDB
import apnitor.facerecognition.app.database.PersonRecord
import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton


@Singleton
class PersonUseCase(
    private val personDB: PersonDB,
) {
    fun addPerson(
        name: String,
        numImages: Long,
    ): Long =
        personDB.addPerson(
            PersonRecord(
                personName = name,
                numImages = numImages,
                addTime = System.currentTimeMillis(),
            ),
        )

    fun removePerson(id: Long) {
        personDB.removePerson(id)
    }

    fun getAll(): Flow<List<PersonRecord>> = personDB.getAll()

    fun getCount(): Long = personDB.getCount()
}
