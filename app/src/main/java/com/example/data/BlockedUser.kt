package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "blocked_users")
data class BlockedUser(
    @PrimaryKey
    val deviceId: String,
    val name: String,
    val role: String,
    val model: String
)
