package apnitor.facerecognition.app.viewmodel

import androidx.lifecycle.ViewModel
import apnitor.facerecognition.app.FirebaseTimeProvider
import apnitor.facerecognition.app.database.AttendanceDB
import apnitor.facerecognition.app.database.PersonDB
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class AttendanceHistoryViewModel @Inject constructor(
    private val attendanceDB: AttendanceDB,
    private val personDB: PersonDB,
    private val time: FirebaseTimeProvider
) : ViewModel() {

    data class Row(
        val name: String,
        val dayKey: String,
        val checkIn: Long?,
        val checkOut: Long?
    )

    fun loadLastDays(days: Int = 30): List<Row> {
        val rows = mutableListOf<Row>()
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.timeInMillis = time.nowUtc()
        repeat(days) {
            val dayKey = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(cal.time)
            val list = attendanceDB.getAllForDay(dayKey)
            list.forEach { rec ->
                rows.add(Row(rec.personName, rec.dayKeyUtc, rec.checkInMillisUtc, rec.checkOutMillisUtc))
            }
            cal.add(Calendar.DAY_OF_MONTH, -1)
        }
        return rows.sortedWith(compareBy<Row> { it.dayKey }.thenBy { it.name })
    }
    fun fmtIst(ts: Long?): String = when (ts) {
        null -> "—"
        else -> time.formatIstTime(ts)
    }
}