with open('app/src/main/java/com/example/data/EduVoteDao.kt', 'r') as f:
    text = f.read()

patch = """
    @androidx.room.Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUser(user: BlockedUser)

    @androidx.room.Query("SELECT * FROM blocked_users")
    fun getAllBlockedUsers(): kotlinx.coroutines.flow.Flow<List<BlockedUser>>

    @androidx.room.Query("DELETE FROM blocked_users WHERE deviceId = :deviceId")
    suspend fun deleteBlockedUser(deviceId: String)
"""

text = text.replace('}', patch + '\n}')

with open('app/src/main/java/com/example/data/EduVoteDao.kt', 'w') as f:
    f.write(text)
