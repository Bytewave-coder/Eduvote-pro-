package com.example.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "students",
    indices = [Index(value = ["rollNumber", "classNum", "section"], unique = true)]
)
data class Student(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val rollNumber: String,
    val classNum: String,
    val section: String,
    val admissionNumber: String,
    val photoUri: String?,
    val hasVotedInCurrentEvent: Boolean = false
)

@Entity(tableName = "election_events")
data class ElectionEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val classTarget: String,
    val sectionTarget: String,
    val electionType: String,
    val candidateLimit: Int,
    val startDateMillis: Long,
    val endDateMillis: Long,
    val description: String = "",
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
    val completedTimeMillis: Long? = null,
    val isImportant: Boolean = false,
    val isDeleted: Boolean = false,
    val winnerId: String? = null,
    val resultsPassword: String? = null
) {
    fun isWaitingPeriodActive(): Boolean {
        if (!isCompleted) return false
        val startWait = completedTimeMillis ?: return false
        val waitEnd = startWait + WAIT_DURATION_MILLIS
        return System.currentTimeMillis() < waitEnd
    }

    fun getRemainingWaitMillis(): Long {
        if (!isCompleted) return 0L
        val startWait = completedTimeMillis ?: return 0L
        val waitEnd = startWait + WAIT_DURATION_MILLIS
        val remaining = waitEnd - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0L
    }

    companion object {
        const val WAIT_DURATION_MILLIS: Long = (6 * 60 + 30) * 60 * 1000L // 6.5 hours
    }
}

@Entity(
    tableName = "candidates",
    foreignKeys = [
        ForeignKey(entity = ElectionEvent::class, parentColumns = ["id"], childColumns = ["electionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = Student::class, parentColumns = ["id"], childColumns = ["studentId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("electionId"), Index("studentId")]
)
data class Candidate(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val electionId: String,
    val studentId: String,
    val partyName: String,
    val partySymbolUri: String?,
    val manifesto: String,
    val candidateRole: String = "",
    val voteCount: Int = 0
)

@Entity(
    tableName = "vote_logs",
    foreignKeys = [
        ForeignKey(entity = ElectionEvent::class, parentColumns = ["id"], childColumns = ["electionId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [Index("electionId")]
)
data class VoteLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val electionId: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val hashVerification: String
)

@Entity(tableName = "known_devices")
data class KnownDevice(
    @PrimaryKey val deviceId: String,
    val lastSeenMillis: Long,
    val model: String,
    val info: String
)

data class CandidateWithStudent(
    val candidate: Candidate,
    val student: Student
)
