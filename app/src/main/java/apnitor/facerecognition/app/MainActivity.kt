package apnitor.facerecognition.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import apnitor.facerecognition.app.screen.AddFaceScreen
import apnitor.facerecognition.app.screen.DetectScreen
import apnitor.facerecognition.app.screen.FaceListScreen
import apnitor.facerecognition.app.ui.theme.PureattendanceSystemTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            NavHost(
                navController = navHostController,
                startDestination = "detect",
                enterTransition = { fadeIn() },
                exitTransition = { fadeOut() },
            ) {
                composable("add-face") { AddFaceScreen { navHostController.navigateUp() } }
                composable("detect") { DetectScreen { navHostController.navigate("face-list") } }
                composable("face-list") {
                    FaceListScreen(
                        onNavigateBack = { navHostController.navigateUp() },
                        onAddFaceClick = { navHostController.navigate("add-face") },
                    )
                }
            }
        }
    }
}
