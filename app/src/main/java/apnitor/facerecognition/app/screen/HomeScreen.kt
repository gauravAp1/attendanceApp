package apnitor.facerecognition.app.screen


import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddFace: () -> Unit,
    onHistory: () -> Unit,
    onMarkAttendance: () -> Unit,
    onFaceList: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Attendance • Home") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LargeActionButton("Add Face", Icons.Default.AddAPhoto, onAddFace)
            LargeActionButton("Check History", Icons.Default.History, onHistory)
            LargeActionButton("Mark Attendance", Icons.Default.Assignment, onMarkAttendance)
            LargeActionButton("Face List", Icons.Default.Face, onFaceList)
        }
    }
}

@Composable
private fun LargeActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
            .height(64.dp)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Text(label)
    }
}
