package com.newspaper.eventstimeline.data.repository

import com.newspaper.eventstimeline.data.api.GeminiService
import com.newspaper.eventstimeline.data.local.EventDao
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.ArticleEventCrossRef
import com.newspaper.eventstimeline.data.model.Event
import com.newspaper.eventstimeline.data.model.EventWithArticles
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventRepository(
    private val eventDao: EventDao,
    private val geminiService: GeminiService
) {
    fun getAllEvents(): Flow<List<Event>> = eventDao.getAllEvents()

    suspend fun getEventById(id: Long): Event? = eventDao.getEventById(id)

    fun getEventByIdFlow(id: Long): Flow<Event?> = eventDao.getEventByIdFlow(id)

    fun getAllEventsWithArticles(): Flow<List<EventWithArticles>> =
        eventDao.getAllEventsWithArticles()

    fun getEventWithArticles(eventId: Long): Flow<EventWithArticles?> =
        eventDao.getEventWithArticles(eventId)

    fun getEventsByDateRange(startDate: Date, endDate: Date): Flow<List<Event>> =
        eventDao.getEventsByDateRange(startDate, endDate)

    suspend fun insertEvent(event: Event): Long = eventDao.insertEvent(event)

    suspend fun updateEvent(event: Event) = eventDao.updateEvent(event)

    suspend fun deleteEvent(event: Event) = eventDao.deleteEvent(event)

    suspend fun linkArticleToEvent(articleId: Long, eventId: Long) {
        eventDao.insertArticleEventCrossRef(ArticleEventCrossRef(articleId, eventId))
    }

    suspend fun unlinkArticleFromEvent(articleId: Long, eventId: Long) {
        eventDao.deleteArticleEventCrossRef(ArticleEventCrossRef(articleId, eventId))
    }

    suspend fun deleteArticleFromAllEvents(articleId: Long) {
        eventDao.deleteArticleFromAllEvents(articleId)
    }

    suspend fun deleteAllArticlesFromEvent(eventId: Long) {
        eventDao.deleteAllArticlesFromEvent(eventId)
    }

    /**
     * Analyze articles and create events using Gemini AI
     */
    suspend fun analyzeAndCreateEvents(articles: List<Article>): Result<List<Event>> {
        return try {
            val analysisResult = geminiService.analyzeArticlesForEvents(articles)
            
            if (analysisResult.isFailure) {
                return Result.failure(analysisResult.exceptionOrNull()!!)
            }

            val analysis = analysisResult.getOrNull()!!
            val createdEvents = mutableListOf<Event>()

            for (eventSuggestion in analysis.events) {
                // Parse date if available
                val eventDate = eventSuggestion.date?.let { dateStr ->
                    try {
                        SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                    } catch (e: Exception) {
                        null
                    }
                }

                // Create event
                val event = Event(
                    title = eventSuggestion.title,
                    description = eventSuggestion.description,
                    eventDate = eventDate,
                    category = eventSuggestion.category,
                    location = eventSuggestion.location,
                    keyPersons = eventSuggestion.keyPersons.joinToString(","),
                    aiGenerated = true
                )

                val eventId = eventDao.insertEvent(event)
                createdEvents.add(event.copy(id = eventId))

                // Link articles to this event
                for (articleIndex in eventSuggestion.articleIndices) {
                    if (articleIndex < articles.size) {
                        val article = articles[articleIndex]
                        eventDao.insertArticleEventCrossRef(
                            ArticleEventCrossRef(article.id, eventId)
                        )
                    }
                }
            }

            Result.success(createdEvents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Generate or update summary for an event
     */
    suspend fun generateEventSummary(eventId: Long, articles: List<Article>): Result<String> {
        return try {
            val event = eventDao.getEventById(eventId)
                ?: return Result.failure(Exception("Event not found"))

            val summaryResult = geminiService.generateEventSummary(event, articles)
            
            if (summaryResult.isSuccess) {
                val summary = summaryResult.getOrNull()!!
                eventDao.updateEvent(event.copy(summary = summary))
            }

            summaryResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
