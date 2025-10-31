package apnitor.facerecognition.app.screen


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import apnitor.facerecognition.app.database.AttendanceDB
import apnitor.facerecognition.app.FirebaseTimeProvider
import apnitor.facerecognition.app.database.PersonDB
import javax.inject.Inject
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import apnitor.facerecognition.app.viewmodel.AttendanceHistoryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val vm: AttendanceHistoryViewModel = hiltViewModel()
    val rows = remember { mutableStateListOf<AttendanceHistoryViewModel.Row>() }

    LaunchedEffect(Unit) {
        rows.clear()
        rows.addAll(vm.loadLastDays(30)) // uses DB, returns millis (UTC)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History (IST)") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { pad ->
        LazyColumn(modifier = Modifier.padding(pad)) {
            items(rows) { r ->
                ListItem(
                    headlineContent = { Text("${r.name} • ${r.dayKey}") },
                    supportingContent = {
                        Text("IN: ${vm.fmtIst(r.checkIn)}    OUT: ${vm.fmtIst(r.checkOut)}")
                    }
                )
                Divider()
            }
        }
    }
}
