package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

class EduVoteRepository(private val dao: EduVoteDao) {

    fun getAllElections(): Flow<List<ElectionEvent>> = dao.getAllElections()
    fun getOngoingElections(): Flow<List<ElectionEvent>> = dao.getOngoingElections()
    fun getElectionById(id: String): Flow<ElectionEvent?> = dao.getElectionById(id)
    fun getHistoryElections(): Flow<List<ElectionEvent>> = dao.getHistoryElections()
    
    suspend fun clearHistoryElections() = dao.clearHistoryElections()
    
    suspend fun insertElection(event: ElectionEvent) = dao.insertElectionEvent(event)
    suspend fun updateElection(event: ElectionEvent) = dao.updateElectionEvent(event)
    suspend fun deleteElection(event: ElectionEvent) = dao.deleteElectionEvent(event)
    
    fun getAllStudents(): Flow<List<Student>> = dao.getAllStudents()
    fun getStudentsByClassAndSection(classNum: String, section: String) = dao.getStudentsByClassAndSection(classNum, section)
    suspend fun insertStudent(student: Student) = dao.insertStudent(student)
    suspend fun insertStudents(students: List<Student>) = dao.insertStudents(students)
    
    fun getCandidatesForElection(electionId: String): Flow<List<CandidateWithStudent>> {
        return dao.getCandidatesWithStudentsForElection(electionId).map { map ->
            map.entries.map { CandidateWithStudent(it.key, it.value) }.sortedByDescending { it.candidate.voteCount }
        }
    }
    
    suspend fun insertCandidate(candidate: Candidate) = dao.insertCandidate(candidate)
    
    suspend fun verifyVoter(name: String, rollNumber: String): Student? {
        return dao.getStudentByRollAndName(rollNumber, name)
    }

    suspend fun castVote(voterName: String, candidateId: String, electionId: String) {
        val rawData = "$voterName-$candidateId-$electionId-${System.currentTimeMillis()}"
        val hash = hashString(rawData)
        dao.castVoteGuest(candidateId, electionId, hash)
    }
    
    suspend fun insertVoteLog(log: VoteLog) {
        dao.insertVoteLog(log)
    }
    
    suspend fun getVoteCountForElection(electionId: String): Int = dao.getVoteCountForElection(electionId)
    fun getTotalVotesCast(): Flow<Int> = dao.getTotalVotesCast()
    suspend fun getStudentCountForClass(classNum: String, section: String): Int = dao.getStudentCountForClass(classNum, section)
    suspend fun deleteStudentsByClassAndSection(classNum: String, section: String) = dao.deleteStudentsByClassAndSection(classNum, section)

    private fun hashString(input: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }
    }
}
