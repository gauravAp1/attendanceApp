// FirebaseTimeProvider.kt
package apnitor.facerecognition.app

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*

/**
 * Reads Realtime Database ".info/serverTimeOffset" and keeps a cached offset.
 * nowUtc() = System.currentTimeMillis() + offsetMs
 */
@Singleton
class FirebaseTimeProvider @Inject constructor() {
    @Volatile private var offsetMs: Long = 0L

    private val tzUtc: TimeZone = TimeZone.getTimeZone("UTC")
    private val tzIst: TimeZone = TimeZone.getTimeZone("Asia/Kolkata") // ✅ correct IST zone

    init {
        FirebaseDatabase.getInstance().reference.child(".info/serverTimeOffset")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val v = snapshot.getValue(Long::class.java)
                    offsetMs = v ?: 0L
                }
                override fun onCancelled(error: DatabaseError) { /* ignore */ }
            })
    }

    /** Millis in UTC using Firebase offset (system-independent) */
    fun nowUtc(): Long = System.currentTimeMillis() + offsetMs

    /** Day key using UTC (what you already store in DB) */
    fun dayKeyUtc(millisUtc: Long = nowUtc()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = tzUtc
        }.format(Date(millisUtc))

    /** Optional: Day key using IST if you want to *group by IST calendar days* for UI */
    fun dayKeyIst(millisUtc: Long = nowUtc()): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = tzIst
        }.format(Date(millisUtc))

    /** Format a wall-clock time in IST for display */
    fun formatIstTime(millisUtc: Long): String =
        SimpleDateFormat("HH:mm:ss 'IST'", Locale.US).apply {
            timeZone = tzIst
        }.format(Date(millisUtc))

    /** Format a date label in IST (useful for headers) */
    fun formatIstDate(millisUtc: Long): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = tzIst
        }.format(Date(millisUtc))
}
