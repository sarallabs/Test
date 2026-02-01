package com.newspaper.eventstimeline.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val eventDate: Date? = null,
    val createdDate: Date = Date(),
    val category: String? = null,
    val location: String? = null,
    val keyPersons: String? = null, // Comma-separated names
    val summary: String? = null, // AI-generated summary
    val aiGenerated: Boolean = false
)
