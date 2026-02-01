package com.newspaper.eventstimeline.data.local

import androidx.room.*
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.OcrStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface ArticleDao {
    @Query("SELECT * FROM articles ORDER BY captureDate DESC")
    fun getAllArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticleById(articleId: Long): Article?

    @Query("SELECT * FROM articles WHERE id = :articleId")
    fun getArticleByIdFlow(articleId: Long): Flow<Article?>

    @Query("SELECT * FROM articles WHERE ocrStatus = :status ORDER BY captureDate DESC")
    fun getArticlesByOcrStatus(status: OcrStatus): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE isEventInvitation = 1 ORDER BY publicationDate DESC")
    fun getEventInvitations(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE publicationDate BETWEEN :startDate AND :endDate ORDER BY publicationDate DESC")
    fun getArticlesByDateRange(startDate: Date, endDate: Date): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE ocrText LIKE '%' || :searchQuery || '%' ORDER BY captureDate DESC")
    fun searchArticles(searchQuery: String): Flow<List<Article>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticle(article: Article): Long

    @Update
    suspend fun updateArticle(article: Article)

    @Delete
    suspend fun deleteArticle(article: Article)

    @Query("DELETE FROM articles WHERE id = :articleId")
    suspend fun deleteArticleById(articleId: Long)

    @Query("UPDATE articles SET ocrStatus = :status, ocrText = :ocrText WHERE id = :articleId")
    suspend fun updateOcrResult(articleId: Long, status: OcrStatus, ocrText: String?)

    @Query("UPDATE articles SET driveFileId = :driveFileId WHERE id = :articleId")
    suspend fun updateDriveFileId(articleId: Long, driveFileId: String)

    @Query("UPDATE articles SET eventId = :eventId WHERE id = :articleId")
    suspend fun updateArticleEvent(articleId: Long, eventId: Long?)
}
