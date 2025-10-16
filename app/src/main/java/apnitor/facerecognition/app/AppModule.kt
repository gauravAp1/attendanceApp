package apnitor.facerecognition.app

import android.content.Context
import apnitor.facerecognition.app.FaceDetection.FaceSpoofDetector
import apnitor.facerecognition.app.FaceDetection.ImageVectorUseCase
import apnitor.facerecognition.app.FaceDetection.MediapipeFaceDetector
import apnitor.facerecognition.app.FaceDetection.PersonUseCase
import apnitor.facerecognition.app.database.ImagesVectorDB
import apnitor.facerecognition.app.database.PersonDB
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppModule {

    // --- Data layer ---
    @Provides
    @Singleton
    fun providePersonDB(): PersonDB = PersonDB()

    @Provides
    @Singleton
    fun provideImagesVectorDB(): ImagesVectorDB = ImagesVectorDB()

    // --- Domain layer ---
    @Provides
    @Singleton
    fun provideFaceNet(@ApplicationContext context: Context): FaceNet = FaceNet(context)

    @Provides
    @Singleton
    fun provideFaceSpoofDetector(@ApplicationContext context: Context): FaceSpoofDetector =
        FaceSpoofDetector(context)

    @Provides
    @Singleton
    fun provideMediapipeFaceDetector(@ApplicationContext context: Context): MediapipeFaceDetector =
        MediapipeFaceDetector(context)

    @Provides
    @Singleton
    fun providePersonUseCase(personDB: PersonDB): PersonUseCase =
        PersonUseCase(personDB)



    @Provides
    @Singleton
    fun provideImageVectorUseCase(
        mediapipeFaceDetector: MediapipeFaceDetector,
        faceSpoofDetector: FaceSpoofDetector,
        imagesVectorDB: ImagesVectorDB,
        faceNet: FaceNet
    ): ImageVectorUseCase =
        ImageVectorUseCase(mediapipeFaceDetector, faceSpoofDetector, imagesVectorDB, faceNet)
}