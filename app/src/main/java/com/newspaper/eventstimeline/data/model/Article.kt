package com.newspaper.eventstimeline.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val imagePath: String,
    val thumbnailPath: String? = null,
    val ocrText: String? = null,
    val newspaperName: String? = null,
    val publicationDate: Date? = null,
    val captureDate: Date = Date(),
    val language: ArticleLanguage? = null,
    val isEventInvitation: Boolean = false,
    val driveFileId: String? = null,
    val ocrStatus: OcrStatus = OcrStatus.PENDING,
    val eventId: Long? = null,
    val tags: String? = null, // Comma-separated tags
    val notes: String? = null
)

enum class ArticleLanguage {
    URDU,
    TELUGU,
    ENGLISH,
    MIXED,
    UNKNOWN
}

enum class OcrStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
}
