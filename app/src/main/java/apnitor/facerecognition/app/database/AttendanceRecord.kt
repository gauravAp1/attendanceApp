//package apnitor.facerecognition.app.database
//
//import io.objectbox.annotation.Entity
//import io.objectbox.annotation.Id
//import io.objectbox.annotation.Index
//
//@Entity
//data class AttendanceRecord(
//    @Id var id: Long = 0,
//    @Index var personID: Long = 0,
//    var personName: String = "",
//    @Index var dayKeyUtc: String = "",   // YYYY-MM-DD (UTC)
//    var checkInMillisUtc: Long? = null,
//    var checkOutMillisUtc: Long? = null,
//    var createdAtMillisUtc: Long = 0,
//    var updatedAtMillisUtc: Long = 0,
//)
