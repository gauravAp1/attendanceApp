//package apnitor.facerecognition.app.database
//
//
//import com.google.firebase.database.*
//import kotlinx.coroutines.suspendCancellableCoroutine
//import javax.inject.Inject
//import javax.inject.Singleton
//import kotlin.coroutines.resume
//import apnitor.facerecognition.app.database.AttendanceDB
//import apnitor.facerecognition.app.database.AttendanceRecord
//import java.util.*
//
//sealed class AttendanceResult {
//    data class CheckedIn(val atMillisUtc: Long): AttendanceResult()
//    data class CheckedOut(val atMillisUtc: Long): AttendanceResult()
//    data class TooSoon(val remainingMillis: Long): AttendanceResult()
//    object AlreadyCheckedOut: AttendanceResult()
//    data class Error(val message: String): AttendanceResult()
//}
//
//@Singleton
//class AttendanceUseCase @Inject constructor(
//    private val attendanceDB: AttendanceDB,
//    private val realtimeDb: FirebaseDatabase
//) {
//    private val MIN_CHECKOUT_DELAY = 3 * 60 * 1000L // 3 minutes
//
//    suspend fun markAttendance(personId: Long, personName: String): AttendanceResult {
//        val nowUtc = getServerTimeMillis()
//        val dayKey = utcDayKey(nowUtc)
//
//        val existing = attendanceDB.getByPersonAndDay(personId, dayKey)
//        return if (existing == null) {
//            // First time today => check-in
//            val rec = AttendanceRecord(
//                personID = personId,
//                personName = personName,
//                dayKeyUtc = dayKey,
//                checkInMillisUtc = nowUtc,
//                checkOutMillisUtc = null,
//                createdAtMillisUtc = nowUtc,
//                updatedAtMillisUtc = nowUtc
//            )
//            attendanceDB.put(rec)
//            AttendanceResult.CheckedIn(nowUtc)
//        } else {
//            // Already has a record today
//            val inMs = existing.checkInMillisUtc
//            val outMs = existing.checkOutMillisUtc
//            if (outMs != null) {
//                AttendanceResult.AlreadyCheckedOut
//            } else {
//                if (inMs == null) {
//                    // Edge: someone created record w/o check-in -> set it now
//                    existing.checkInMillisUtc = nowUtc
//                    existing.updatedAtMillisUtc = nowUtc
//                    attendanceDB.put(existing)
//                    AttendanceResult.CheckedIn(nowUtc)
//                } else {
//                    val elapsed = nowUtc - inMs
//                    if (elapsed >= MIN_CHECKOUT_DELAY) {
//                        existing.checkOutMillisUtc = nowUtc
//                        existing.updatedAtMillisUtc = nowUtc
//                        attendanceDB.put(existing)
//                        AttendanceResult.CheckedOut(nowUtc)
//                    } else {
//                        AttendanceResult.TooSoon(MIN_CHECKOUT_DELAY - elapsed)
//                    }
//                }
//            }
//        }
//    }
//
//    /** Public: get a person's month-wise records (for your future UI) */
//    fun getMonthRecordsUtc(personId: Long, year: Int, month1to12: Int) =
//        attendanceDB.getByPersonAndMonthUtc(personId, year, month1to12)
//
//    /** Reliable server time in ms using RTDB offset */
//    private suspend fun getServerTimeMillis(): Long {
//        val offset = readServerTimeOffset()
//        return System.currentTimeMillis() + offset
//    }
//
//    private suspend fun readServerTimeOffset(): Long = suspendCancellableCoroutine { cont ->
//        val ref = realtimeDb.getReference(".info/serverTimeOffset")
//        val listener = object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                val offset = snapshot.getValue(Long::class.java) ?: 0L
//                cont.resume(offset)
//            }
//            override fun onCancelled(error: DatabaseError) {
//                cont.resume(0L) // fallback: local time (rare)
//            }
//        }
//        ref.addListenerForSingleValueEvent(listener)
//        cont.invokeOnCancellation { /* no-op */ }
//    }
//
//    private fun utcDayKey(millisUtc: Long): String {
//        val tz = TimeZone.getTimeZone("UTC")
//        val cal = Calendar.getInstance(tz).apply { timeInMillis = millisUtc }
//        val y = cal.get(Calendar.YEAR)
//        val m = cal.get(Calendar.MONTH) + 1
//        val d = cal.get(Calendar.DAY_OF_MONTH)
//        return String.format(Locale.US, "%04d-%02d-%02d", y, m, d)
//    }
//}


package apnitor.facerecognition.app.database

import apnitor.facerecognition.app.FirebaseTimeProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

@Singleton
class AttendanceUseCase @Inject constructor(
    private val personDB: PersonDB,
    private val attendanceDB: AttendanceDB,
    private val time: FirebaseTimeProvider
) {
    private val minCheckoutGapMs = 3.minutes.inWholeMilliseconds

    /**
     * Mark attendance for this person name (from recognition).
     * Returns a user-facing message to show in a dialog, or null if nothing changed.
     */
    fun mark(name: String): String? {
        // Person must exist (added via your add-face flow).
        val person = personDB.findByName(name) ?: return null

        val now = time.nowUtc()
        val dayKey = time.dayKeyUtc(now)
        val record = attendanceDB.getByPersonAndDay(person.personID, dayKey)
            ?: AttendanceRecord(
                personID = person.personID,
                personName = person.personName,
                dayKeyUtc = dayKey,
                createdAtMillisUtc = now,
                updatedAtMillisUtc = now
            )

        // Already fully done today?
        if (record.checkInMillisUtc != null && record.checkOutMillisUtc != null) {
            return "Day finished for ${person.personName}."
        }

        // First hit today => check-in
        if (record.checkInMillisUtc == null) {
            record.checkInMillisUtc = now
            record.updatedAtMillisUtc = now
            attendanceDB.put(record)
            return "Check-in marked for ${person.personName}."
        }

        // Second hit => check-out (only if >= 3 minutes after check-in)
        val sinceIn = now - (record.checkInMillisUtc ?: now)
        if (sinceIn < minCheckoutGapMs) {
            val remain = ((minCheckoutGapMs - sinceIn) / 1000).coerceAtLeast(1)
            return "Too soon to check-out. Try again in ~${remain}s."
        }

        if (record.checkOutMillisUtc == null) {
            record.checkOutMillisUtc = now
            record.updatedAtMillisUtc = now
            attendanceDB.put(record)
            return "Check-out marked for ${person.personName}."
        }

        return null
    }
}
