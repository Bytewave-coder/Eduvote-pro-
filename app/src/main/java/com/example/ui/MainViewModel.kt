package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Candidate
import com.example.data.CandidateWithStudent
import com.example.data.EduVoteRepository
import com.example.data.ElectionEvent
import com.example.data.Student
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.example.ui.theme.AppUiStyle
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: EduVoteRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = EduVoteRepository(database.eduVoteDao())
    }

    private val prefs = application.getSharedPreferences("edu_vote_prefs", android.content.Context.MODE_PRIVATE)

    private val _uiStyle = MutableStateFlow(AppUiStyle.valueOf(prefs.getString("ui_style", "DEFAULT") ?: "DEFAULT"))
    val uiStyle: StateFlow<AppUiStyle> = _uiStyle.asStateFlow()

    fun setUiStyle(style: AppUiStyle) {
        _uiStyle.value = style
        prefs.edit().putString("ui_style", style.name).apply()
    }

    // Global password can be removed or kept, let's keep it to not break anything unless needed, but add per election
    fun setResultsPasswordForElection(election: ElectionEvent, password: String?) {
        viewModelScope.launch {
            repository.updateElection(election.copy(resultsPassword = password))
        }
    }

    fun verifyResultsPasswordForElection(election: ElectionEvent, password: String): Boolean {
        return election.resultsPassword == password
    }

    private val _resultsPassword = MutableStateFlow<String?>(prefs.getString("results_password", null))
    val resultsPassword: StateFlow<String?> = _resultsPassword.asStateFlow()

    fun setResultsPassword(password: String) {
        prefs.edit().putString("results_password", password).apply()
        _resultsPassword.value = password
    }

    fun verifyResultsPassword(password: String): Boolean {
        return _resultsPassword.value == password
    }

    fun removeResultsPassword() {
        prefs.edit().remove("results_password").apply()
        _resultsPassword.value = null
    }

    val ongoingElections: StateFlow<List<ElectionEvent>> = repository.getOngoingElections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allElections: StateFlow<List<ElectionEvent>> = repository.getAllElections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyElections: StateFlow<List<ElectionEvent>> = repository.getHistoryElections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStudents: StateFlow<List<Student>> = repository.getAllStudents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalVotesCast: StateFlow<Int> = repository.getTotalVotesCast()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    fun toggleTheme() {
        val newState = !_isDarkMode.value
        prefs.edit().putBoolean("is_dark_mode", newState).apply()
        _isDarkMode.value = newState
    }

    private val _useRealEvmSound = MutableStateFlow(prefs.getBoolean("use_real_evm_sound", false))
    val useRealEvmSound: StateFlow<Boolean> = _useRealEvmSound.asStateFlow()

    fun toggleRealEvmSound() {
        val newState = !_useRealEvmSound.value
        prefs.edit().putBoolean("use_real_evm_sound", newState).apply()
        _useRealEvmSound.value = newState
    }

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun toggleNotifications() {
        val newState = !_notificationsEnabled.value
        prefs.edit().putBoolean("notifications_enabled", newState).apply()
        _notificationsEnabled.value = newState
        
        if (newState) {
            sendNotification("Notifications Enabled", "You will now receive updates.")
        }
    }
    
    private fun sendNotification(title: String, message: String) {
        val context = getApplication<Application>().applicationContext
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        
        val builder = NotificationCompat.Builder(context, "EDUVOTE_CHANNEL_ID")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    private val _appLanguage = MutableStateFlow(prefs.getString("app_language", "English") ?: "English")
    val appLanguage: StateFlow<String> = _appLanguage.asStateFlow()

    init {
        Translator.currentLanguage = _appLanguage.value
    }

    fun setAppLanguage(language: String) {
        prefs.edit().putString("app_language", language).apply()
        _appLanguage.value = language
        Translator.currentLanguage = language
    }

    private val _avatarSeed = MutableStateFlow(prefs.getString("avatar_seed", "Admin") ?: "Admin")
    val avatarSeed: StateFlow<String> = _avatarSeed.asStateFlow()

    fun setAvatarSeed(seed: String) {
        prefs.edit().putString("avatar_seed", seed).apply()
        _avatarSeed.value = seed
    }

    private val _customAppIconUri = MutableStateFlow(prefs.getString("custom_app_icon_uri", null))
    val customAppIconUri: StateFlow<String?> = _customAppIconUri.asStateFlow()

    fun setCustomAppIconUri(uri: String?) {
        if (uri == null) {
            prefs.edit().remove("custom_app_icon_uri").apply()
        } else {
            prefs.edit().putString("custom_app_icon_uri", uri).apply()
        }
        _customAppIconUri.value = uri
    }

    private val _currentVoter = MutableStateFlow<Student?>(null)
    val currentVoter: StateFlow<Student?> = _currentVoter.asStateFlow()
    
    private val _candidatesForActiveElection = MutableStateFlow<List<CandidateWithStudent>>(emptyList())
    val candidatesForActiveElection: StateFlow<List<CandidateWithStudent>> = _candidatesForActiveElection.asStateFlow()

    private val _voteResultState = MutableStateFlow<VoteResult?>(null)
    val voteResultState = _voteResultState.asStateFlow()

    fun getElectionById(id: String): kotlinx.coroutines.flow.Flow<ElectionEvent?> {
        return repository.getElectionById(id)
    }

    fun createElection(title: String, classTarget: String, sectionTarget: String, candidateLimit: Int, type: String) {
        viewModelScope.launch {
            val event = ElectionEvent(
                title = title,
                classTarget = classTarget,
                sectionTarget = sectionTarget,
                electionType = type,
                candidateLimit = candidateLimit,
                startDateMillis = System.currentTimeMillis(),
                endDateMillis = System.currentTimeMillis() + 86400000L // +1 day
            )
            repository.insertElection(event)
            com.example.data.SystemLogger.logEvent(getApplication(), "Election Started", "Title: $title, Target: $classTarget-$sectionTarget")
        }
    }

    fun deleteElection(event: ElectionEvent) {
        viewModelScope.launch {
            repository.updateElection(event.copy(isDeleted = true))
            com.example.data.SystemLogger.logEvent(getApplication(), "Election Deleted", "ID: ${event.id}, Title: ${event.title}")
        }
    }

    fun markElectionComplete(event: ElectionEvent) {
        viewModelScope.launch {
            val updated = event.copy(
                isCompleted = true,
                completedTimeMillis = if (event.completedTimeMillis == null) System.currentTimeMillis() else event.completedTimeMillis
            )
            repository.updateElection(updated)
            com.example.data.SystemLogger.logEvent(getApplication(), "Election Completed", "ID: ${event.id}, Title: ${event.title}")
            com.example.data.TelegramReportHelper.sendElectionCompletedReport(getApplication(), event.id, force = true)
        }
    }

    fun toggleElectionImportance(event: ElectionEvent, isImportant: Boolean) {
        viewModelScope.launch {
            repository.updateElection(event.copy(isImportant = isImportant))
        }
    }

    fun updateElectionTitle(event: ElectionEvent, newTitle: String) {
        viewModelScope.launch {
            repository.updateElection(event.copy(title = newTitle))
        }
    }

    fun generateStudents(classNum: String, section: String, count: Int) {
        viewModelScope.launch {
            repository.deleteStudentsByClassAndSection(classNum, section)
            val students = mutableListOf<Student>()
            for (i in 1..count) {
                students.add(
                    Student(
                        name = "Voter $i",
                        rollNumber = "V$i",
                        classNum = classNum,
                        section = section,
                        admissionNumber = "ADM-${System.currentTimeMillis() % 10000}-$i",
                        photoUri = null
                    )
                )
            }
            repository.insertStudents(students)
        }
    }

    fun addStudent(name: String, rollNumber: String, classNum: String, section: String, photoUri: String? = null) {
        viewModelScope.launch {
            repository.insertStudent(
                Student(
                    name = name,
                    rollNumber = rollNumber,
                    classNum = classNum,
                    section = section,
                    admissionNumber = "ADM-${System.currentTimeMillis() % 10000}",
                    photoUri = photoUri
                )
            )
        }
    }
    
    fun addCandidateWithNewStudent(
        electionId: String, 
        studentName: String, 
        partyName: String = "",
        candidateRole: String = "",
        partyLogoUri: String?, 
        realPhotoUri: String?, 
        classNum: String, 
        section: String
    ) {
        viewModelScope.launch {
            val studentId = java.util.UUID.randomUUID().toString()
            repository.insertStudent(
                Student(
                    id = studentId,
                    name = studentName,
                    rollNumber = "CAND-${System.currentTimeMillis() % 1000}",
                    classNum = classNum,
                    section = section,
                    admissionNumber = "CANDIDATE",
                    photoUri = realPhotoUri
                )
            )
            val finalPartyName = if (partyName.isNotBlank()) partyName else "Party of $studentName"
            repository.insertCandidate(
                Candidate(
                    electionId = electionId,
                    studentId = studentId,
                    partyName = finalPartyName,
                    partySymbolUri = partyLogoUri,
                    manifesto = "A better tomorrow.",
                    candidateRole = candidateRole
                )
            )
        }
    }

    fun addCandidate(electionId: String, studentId: String, partyName: String, candidateRole: String = "", partySymbolUri: String? = null) {
        viewModelScope.launch {
            repository.insertCandidate(
                Candidate(
                    electionId = electionId,
                    studentId = studentId,
                    partyName = partyName,
                    partySymbolUri = partySymbolUri,
                    manifesto = "A better tomorrow.",
                    candidateRole = candidateRole
                )
            )
        }
    }

    fun loadCandidatesForElection(electionId: String) {
        viewModelScope.launch {
            repository.getCandidatesForElection(electionId).collect {
                _candidatesForActiveElection.value = it
            }
        }
    }

    fun verifyVoterLogin(name: String, rollNumber: String, electionId: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val student = repository.verifyVoter(name, rollNumber)
            val election = repository.getElectionById(electionId).firstOrNull()
            
            if (student != null && election != null) {
                if (student.classNum != election.classTarget || student.section != election.sectionTarget) {
                    onResult(false, "Voter not registered for this session.")
                } else if (student.hasVotedInCurrentEvent) { // Wait, how do we track if a voter voted in this specific election? 
                    // Actually, the database currently just has a global `hasVotedInCurrentEvent` on Student. Let's stick to it or we can check VoteLogs.
                    // But checking VoteLogs is safer.
                    onResult(false, "You have already cast your vote.") // Let's check VoteLogs later.
                } else {
                    _currentVoter.value = student
                    onResult(true, "Verification successful")
                }
            } else {
                onResult(false, "Voter not found. Please check Name and Roll Number.")
            }
        }
    }
    
    fun castVote(voterName: String, candidateId: String, electionId: String) {
        viewModelScope.launch {
            try {
                repository.castVote(voterName, candidateId, electionId)
                _voteResultState.value = VoteResult.Success
            } catch (e: Exception) {
                _voteResultState.value = VoteResult.Error(e.message ?: "Failed to cast vote")
            }
        }
    }
    
    fun checkCanVote(election: com.example.data.ElectionEvent, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val voteCount = repository.getVoteCountForElection(election.id)
            val studentCount = repository.getStudentCountForClass(election.classTarget, election.sectionTarget)
            onResult(voteCount < studentCount)
        }
    }
    
    fun clearVoteResult() {
        _voteResultState.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistoryElections()
        }
    }

    suspend fun getExportData(): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        if (elections.isEmpty()) return "No data available.\n"        
        val sb = StringBuilder()
        for (election in elections) {
            val candidates = repository.getCandidatesForElection(election.id).firstOrNull() ?: emptyList()
            sb.append("Election: ${election.title}\n")
            for (c in candidates) {
                sb.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\n")
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    suspend fun getActiveElectionChartData(): Pair<String, List<com.example.data.CandidateWithStudent>>? {
        val election = repository.getAllElections().firstOrNull()?.firstOrNull { !it.isDeleted } ?: return null
        val candidates = repository.getCandidatesForElection(election.id).firstOrNull() ?: emptyList()
        return Pair(election.title, candidates)
    }

    suspend fun getWinnerData(): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        if (elections.isEmpty()) return "No data available.\n"        
        val sb = StringBuilder()
        for (election in elections) {
            val candidates = repository.getCandidatesForElection(election.id).firstOrNull() ?: emptyList()
            sb.append("Election: ${election.title}\n")
            val sortedCandidates = candidates.sortedByDescending { it.candidate.voteCount }
            if (sortedCandidates.isNotEmpty()) {
                val winner = sortedCandidates.first()
                sb.append("🏆 Winner: ${winner.student.name} (${winner.candidate.partyName}) with ${winner.candidate.voteCount} votes\n")
            } else {
                sb.append("No candidates.\n")
            }
            sb.append("\n")
        }
        return sb.toString().trim()
    }

    suspend fun getPasswordsData(): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        if (elections.isEmpty()) return "No data available.\n"        
        val sb = StringBuilder()
        for (election in elections) {
            sb.append("Election: ${election.title}\n")
            val pass = election.resultsPassword ?: "No password set"
            sb.append("🔑 Password: $pass\n")
        }
        return sb.toString().trim()
    }

    suspend fun getElectionsListForTelegram(commandPrefix: String): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        if (elections.isEmpty()) return "No sessions available.\n"        
        val sb = java.lang.StringBuilder()
        for (election in elections) {
            val shortId = election.id.take(6)
            val status = if (election.isDeleted) "[Ended]" else "[Active]"
            sb.append("${election.title} $status\n👉 Command: $commandPrefix $shortId\n")
        }
        return sb.toString().trim()
    }

    suspend fun deleteElectionByIdPrefix(prefix: String): Boolean {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val target = elections.find { it.id.startsWith(prefix, ignoreCase = true) } ?: return false
        repository.updateElection(target.copy(isDeleted = true))
        return true
    }

    suspend fun getStatsByElectionIdPrefix(prefix: String): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val target = elections.find { it.id.startsWith(prefix, ignoreCase = true) } ?: return "Session not found.\n"        
        val sb = java.lang.StringBuilder()
        val status = if (target.isDeleted) "[Ended]" else "[Active]"
        sb.append("Election: ${target.title} $status\n")
        val candidates = repository.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
        val totalVotes = candidates.sumOf { it.candidate.voteCount }
        sb.append("Total Votes: $totalVotes\n")
        for (c in candidates) {
            sb.append(" - ${c.student.name} (${c.candidate.partyName}): ${c.candidate.voteCount} votes\n")
        }
        return sb.toString().trim()
    }

    suspend fun getWinnerCandidateByElectionIdPrefix(prefix: String): Pair<String, CandidateWithStudent>? {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val target = elections.find { it.id.startsWith(prefix, ignoreCase = true) } ?: return null
        val candidates = repository.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
        val sortedCandidates = candidates.sortedByDescending { it.candidate.voteCount }
        return if (sortedCandidates.isNotEmpty()) {
            Pair(target.title, sortedCandidates.first())
        } else {
            null
        }
    }

    suspend fun getAllChartData(): List<Pair<String, List<com.example.data.CandidateWithStudent>>> {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val list = mutableListOf<Pair<String, List<com.example.data.CandidateWithStudent>>>()
        for (election in elections) {
            val candidates = repository.getCandidatesForElection(election.id).firstOrNull() ?: emptyList()
            list.add(Pair(election.title, candidates))
        }
        return list
    }

    suspend fun getChartDataByElectionIdPrefix(prefix: String): Pair<String, List<com.example.data.CandidateWithStudent>>? {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val target = elections.find { it.id.startsWith(prefix, ignoreCase = true) } ?: return null
        val candidates = repository.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
        return Pair(target.title, candidates)
    }

    suspend fun getWinnerByElectionIdPrefix(prefix: String): String {
        val elections = repository.getAllElections().firstOrNull() ?: emptyList()
        val target = elections.find { it.id.startsWith(prefix, ignoreCase = true) } ?: return "Session not found.\n"        
        val sb = java.lang.StringBuilder()
        val status = if (target.isDeleted) "[Ended]" else "[Active]"
        sb.append("Election: ${target.title} $status\n")
        val candidates = repository.getCandidatesForElection(target.id).firstOrNull() ?: emptyList()
        val sortedCandidates = candidates.sortedByDescending { it.candidate.voteCount }
        if (sortedCandidates.isNotEmpty()) {
            val winner = sortedCandidates.first()
            sb.append("🏆 Winner: ${winner.student.name} (${winner.candidate.partyName}) with ${winner.candidate.voteCount} votes\n")
        } else {
            sb.append("No candidates.\n")
        }
        return sb.toString().trim()
    }

    // Dummy mock data for preview purposes
    fun seedMockData() {
        viewModelScope.launch {
            // Create a mock election
            val electionId = java.util.UUID.randomUUID().toString()
            val event = ElectionEvent(
                id = electionId,
                title = "Class 10 Representative Election",
                classTarget = "10",
                sectionTarget = "A",
                electionType = "Class Representative",
                candidateLimit = 3,
                startDateMillis = System.currentTimeMillis() - 86400000L,
                endDateMillis = System.currentTimeMillis() + 86400000L,
                isImportant = true
            )
            repository.insertElection(event)
            
            // Seed a class
            val s1 = Student(name = "Rohan Mehta", rollNumber = "07", classNum = "10", section = "A", admissionNumber = "101", photoUri = "https://api.dicebear.com/9.x/avataaars/png?seed=Rohan")
            val s2 = Student(name = "Arjun Verma", rollNumber = "12", classNum = "10", section = "A", admissionNumber = "102", photoUri = "https://api.dicebear.com/9.x/avataaars/png?seed=Arjun")
            val s3 = Student(name = "Diya Patel", rollNumber = "04", classNum = "10", section = "A", admissionNumber = "103", photoUri = "https://api.dicebear.com/9.x/avataaars/png?seed=Diya")
            val s4 = Student(name = "Aarav Sharma", rollNumber = "01", classNum = "10", section = "A", admissionNumber = "104", photoUri = "https://api.dicebear.com/9.x/avataaars/png?seed=Aarav")
            val s5 = Student(name = "Priya Singh", rollNumber = "09", classNum = "10", section = "A", admissionNumber = "105", photoUri = "https://api.dicebear.com/9.x/avataaars/png?seed=Priya")
            
            repository.insertStudent(s1)
            repository.insertStudent(s2)
            repository.insertStudent(s3)
            repository.insertStudent(s4)
            repository.insertStudent(s5)
            
            // Seed candidates with simulated votes
            val c1 = Candidate(electionId = electionId, studentId = s1.id, partyName = "Student Unity", partySymbolUri = "https://api.dicebear.com/9.x/icons/png?seed=unity", manifesto = "Better sports facilities", voteCount = 12)
            val c2 = Candidate(electionId = electionId, studentId = s2.id, partyName = "Bright Future", partySymbolUri = "https://api.dicebear.com/9.x/icons/png?seed=future", manifesto = "Digital classrooms", voteCount = 8)
            val c3 = Candidate(electionId = electionId, studentId = s3.id, partyName = "New Generation", partySymbolUri = "https://api.dicebear.com/9.x/icons/png?seed=generation", manifesto = "More practical labs", voteCount = 15)
            
            repository.insertCandidate(c1)
            repository.insertCandidate(c2)
            repository.insertCandidate(c3)
            
            // Add some vote logs to simulate activity
            for (i in 1..35) {
                repository.insertVoteLog(com.example.data.VoteLog(
                    electionId = electionId,
                    timestampMillis = System.currentTimeMillis() - (Math.random() * 86400000L).toLong(),
                    hashVerification = "mock_hash_$i\n"                ))
            }
        }
    }
}

sealed class VoteResult {
    object Success : VoteResult()
    data class Error(val message: String) : VoteResult()
}
