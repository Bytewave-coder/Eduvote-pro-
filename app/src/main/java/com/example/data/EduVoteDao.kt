package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface EduVoteDao {
    // Students
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: Student)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudents(students: List<Student>)

    @Query("SELECT * FROM students ORDER BY name ASC")
    fun getAllStudents(): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE classNum = :classNum AND section = :section ORDER BY name ASC")
    fun getStudentsByClassAndSection(classNum: String, section: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE rollNumber = :rollNumber AND name COLLATE NOCASE = :name LIMIT 1")
    suspend fun getStudentByRollAndName(rollNumber: String, name: String): Student?
    
    @Query("UPDATE students SET hasVotedInCurrentEvent = :hasVoted WHERE id = :studentId")
    suspend fun updateStudentVoteStatus(studentId: String, hasVoted: Boolean)

    // Elections
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElectionEvent(event: ElectionEvent)
    
    @Update
    suspend fun updateElectionEvent(event: ElectionEvent)

    @Delete
    suspend fun deleteElectionEvent(event: ElectionEvent)

    @Query("SELECT * FROM election_events WHERE isDeleted = 0 ORDER BY startDateMillis DESC")
    fun getAllElections(): Flow<List<ElectionEvent>>
    
    @Query("SELECT * FROM election_events WHERE id = :id LIMIT 1")
    fun getElectionById(id: String): Flow<ElectionEvent?>
    
    @Query("SELECT * FROM election_events WHERE isCompleted = 0 AND isDeleted = 0")
    fun getOngoingElections(): Flow<List<ElectionEvent>>
    
    @Query("SELECT * FROM election_events WHERE isCompleted = 1 OR isDeleted = 1 ORDER BY startDateMillis DESC")
    fun getHistoryElections(): Flow<List<ElectionEvent>>

    @Query("DELETE FROM election_events WHERE isCompleted = 1 OR isDeleted = 1")
    suspend fun clearHistoryElections()

    // Candidates
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCandidate(candidate: Candidate)
    
    @Query("SELECT * FROM candidates WHERE electionId = :electionId ORDER BY voteCount DESC")
    fun getCandidatesForElection(electionId: String): Flow<List<Candidate>>
    
    @Query("SELECT * FROM candidates")
    fun getAllCandidates(): Flow<List<Candidate>>
    
    @Query("SELECT c.*, s.* FROM candidates c INNER JOIN students s ON c.studentId = s.id WHERE c.electionId = :electionId ORDER BY c.voteCount DESC")
    fun getCandidatesWithStudentsForElection(electionId: String): Flow<Map<Candidate, Student>>

    // Voting Transaction
    @Insert
    suspend fun insertVoteLog(log: VoteLog)

    @Query("UPDATE candidates SET voteCount = voteCount + 1 WHERE id = :candidateId")
    suspend fun incrementVoteCount(candidateId: String)
    
    @Query("UPDATE candidates SET voteCount = :newCount WHERE id = :candidateId")
    suspend fun updateCandidateVoteCount(candidateId: String, newCount: Int)

    @Transaction
    suspend fun castVoteGuest(candidateId: String, electionId: String, voteHash: String) {
        incrementVoteCount(candidateId)
        insertVoteLog(VoteLog(electionId = electionId, hashVerification = voteHash))
    }

    @Query("SELECT COUNT(*) FROM vote_logs WHERE electionId = :electionId")
    suspend fun getVoteCountForElection(electionId: String): Int
    
    @Query("SELECT COUNT(*) FROM students WHERE classNum = :classNum AND section = :section AND admissionNumber != 'CANDIDATE'")
    suspend fun getStudentCountForClass(classNum: String, section: String): Int
    
    @Query("SELECT COUNT(*) FROM vote_logs")
    fun getTotalVotesCast(): Flow<Int>
    
    @Query("DELETE FROM students WHERE classNum = :classNum AND section = :section AND admissionNumber != 'CANDIDATE'")
    suspend fun deleteStudentsByClassAndSection(classNum: String, section: String)

    // Devices
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateDevice(device: KnownDevice)

    @Query("SELECT * FROM known_devices")
    suspend fun getAllKnownDevices(): List<KnownDevice>

    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(user: BlockedUser)

    @androidx.room.Query("SELECT * FROM blocked_users")
    fun getAllBlockedUsers(): kotlinx.coroutines.flow.Flow<List<BlockedUser>>

    @androidx.room.Query("DELETE FROM blocked_users WHERE deviceId = :deviceId")
    suspend fun deleteBlockedUser(deviceId: String)

}
