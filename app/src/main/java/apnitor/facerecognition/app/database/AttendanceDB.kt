package apnitor.facerecognition.app.database

import io.objectbox.query.QueryBuilder

class AttendanceDB {
    private val box = ObjectBoxStore.store.boxFor(AttendanceRecord::class.java)

    fun getByPersonAndDay(personId: Long, dayKeyUtc: String): AttendanceRecord? =
        box.query()
            .equal(AttendanceRecord_.personID, personId)
            .equal(AttendanceRecord_.dayKeyUtc, dayKeyUtc, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .findFirst()

    fun put(record: AttendanceRecord): Long = box.put(record)

    
    fun getByPersonAndMonthUtc(personId: Long, year: Int, month1to12: Int): List<AttendanceRecord> {
        val (startKey, endKey) = monthRangeKeys(year, month1to12)
        return box.query()
            .equal(AttendanceRecord_.personID, personId)
            .greaterOrEqual(
                AttendanceRecord_.dayKeyUtc,
                startKey,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )
            .lessOrEqual(
                AttendanceRecord_.dayKeyUtc,
                endKey,
                QueryBuilder.StringOrder.CASE_SENSITIVE
            )
            .build()
            .find()
    }

    fun getAllForDay(dayKeyUtc: String): List<AttendanceRecord> =
        box.query()
            .equal(AttendanceRecord_.dayKeyUtc, dayKeyUtc, QueryBuilder.StringOrder.CASE_SENSITIVE)
            .build()
            .find()

    private fun monthRangeKeys(year: Int, month1to12: Int): Pair<String, String> {
        val cal = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(java.util.Calendar.YEAR, year)
        cal.set(java.util.Calendar.MONTH, month1to12 - 1)
        val maxDay = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val start = String.format(java.util.Locale.US, "%04d-%02d-01", year, month1to12)
        val end = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month1to12, maxDay)
        return start to end
    }
}


