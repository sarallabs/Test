package com.newspaper.eventstimeline.data.local

import androidx.room.*
import com.newspaper.eventstimeline.data.model.ArticleEventCrossRef
import com.newspaper.eventstimeline.data.model.Event
import com.newspaper.eventstimeline.data.model.EventWithArticles
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY eventDate DESC, createdDate DESC")
    fun getAllEvents(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Long): Event?

    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventByIdFlow(eventId: Long): Flow<Event?>

    @Transaction
    @Query("SELECT * FROM events ORDER BY eventDate DESC, createdDate DESC")
    fun getAllEventsWithArticles(): Flow<List<EventWithArticles>>

    @Transaction
    @Query("SELECT * FROM events WHERE id = :eventId")
    fun getEventWithArticles(eventId: Long): Flow<EventWithArticles?>

    @Query("SELECT * FROM events WHERE eventDate BETWEEN :startDate AND :endDate ORDER BY eventDate DESC")
    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<Event>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Update
    suspend fun updateEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticleEventCrossRef(crossRef: ArticleEventCrossRef)

    @Delete
    suspend fun deleteArticleEventCrossRef(crossRef: ArticleEventCrossRef)

    @Query("DELETE FROM article_event_cross_ref WHERE articleId = :articleId")
    suspend fun deleteArticleFromAllEvents(articleId: Long)

    @Query("DELETE FROM article_event_cross_ref WHERE eventId = :eventId")
    suspend fun deleteAllArticlesFromEvent(eventId: Long)
}
