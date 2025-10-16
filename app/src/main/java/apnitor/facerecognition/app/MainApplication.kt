package apnitor.facerecognition.app

import android.app.Application
import apnitor.facerecognition.app.database.ObjectBoxStore
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ObjectBoxStore.init(this)
    }
}