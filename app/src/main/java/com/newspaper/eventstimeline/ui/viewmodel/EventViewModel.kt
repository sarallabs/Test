package com.newspaper.eventstimeline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newspaper.eventstimeline.data.api.GeminiService
import com.newspaper.eventstimeline.data.local.AppDatabase
import com.newspaper.eventstimeline.data.model.Event
import com.newspaper.eventstimeline.data.repository.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    
    // TODO: Get API key from secure storage or configuration
    private val geminiService = GeminiService("YOUR_GEMINI_API_KEY")
    
    private val repository = EventRepository(
        database.eventDao(),
        geminiService
    )

    val allEvents = repository.getAllEvents()
    val allEventsWithArticles = repository.getAllEventsWithArticles()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun getEventById(id: Long) = repository.getEventByIdFlow(id)

    fun getEventWithArticles(eventId: Long) = repository.getEventWithArticles(eventId)

    fun createEvent(
        title: String,
        description: String?,
        eventDate: Date?,
        category: String?,
        location: String?,
        keyPersons: String?
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                val event = Event(
                    title = title,
                    description = description,
                    eventDate = eventDate,
                    category = category,
                    location = location,
                    keyPersons = keyPersons,
                    aiGenerated = false
                )
                
                repository.insertEvent(event)
                _successMessage.value = "Event created successfully"
            } catch (e: Exception) {
                _errorMessage.value = "Error creating event: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateEvent(event: Event) {
        viewModelScope.launch {
            try {
                repository.updateEvent(event)
                _successMessage.value = "Event updated"
            } catch (e: Exception) {
                _errorMessage.value = "Error updating event: ${e.message}"
            }
        }
    }

    fun deleteEvent(event: Event) {
        viewModelScope.launch {
            try {
                repository.deleteEvent(event)
                _successMessage.value = "Event deleted"
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting event: ${e.message}"
            }
        }
    }

    fun linkArticleToEvent(articleId: Long, eventId: Long) {
        viewModelScope.launch {
            try {
                repository.linkArticleToEvent(articleId, eventId)
                _successMessage.value = "Article linked to event"
            } catch (e: Exception) {
                _errorMessage.value = "Error linking article: ${e.message}"
            }
        }
    }

    fun unlinkArticleFromEvent(articleId: Long, eventId: Long) {
        viewModelScope.launch {
            try {
                repository.unlinkArticleFromEvent(articleId, eventId)
                _successMessage.value = "Article unlinked from event"
            } catch (e: Exception) {
                _errorMessage.value = "Error unlinking article: ${e.message}"
            }
        }
    }

    fun analyzeArticlesAndCreateEvents(articleIds: List<Long>) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                val articles = articleIds.mapNotNull { id ->
                    database.articleDao().getArticleById(id)
                }
                
                if (articles.isEmpty()) {
                    _errorMessage.value = "No articles found"
                    return@launch
                }

                val result = repository.analyzeAndCreateEvents(articles)
                if (result.isSuccess) {
                    val events = result.getOrNull()!!
                    _successMessage.value = "Created ${events.size} events from AI analysis"
                } else {
                    _errorMessage.value = "Error analyzing articles: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun generateEventSummary(eventId: Long) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                
                // Get articles linked to this event
                val eventWithArticles = repository.getEventWithArticles(eventId)
                // We need to collect the flow once
                eventWithArticles.collect { ewa ->
                    if (ewa != null) {
                        val result = repository.generateEventSummary(eventId, ewa.articles)
                        if (result.isSuccess) {
                            _successMessage.value = "Summary generated"
                        } else {
                            _errorMessage.value = "Error generating summary: ${result.exceptionOrNull()?.message}"
                        }
                    }
                    _isProcessing.value = false
                    return@collect // Only collect once
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error: ${e.message}"
                _isProcessing.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }
}
