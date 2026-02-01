package com.newspaper.eventstimeline.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newspaper.eventstimeline.data.api.DriveOcrService
import com.newspaper.eventstimeline.data.api.GeminiService
import com.newspaper.eventstimeline.data.api.MlKitOcrService
import com.newspaper.eventstimeline.data.local.AppDatabase
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.OcrStatus
import com.newspaper.eventstimeline.data.repository.ArticleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

class ArticleViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val mlKitOcrService = MlKitOcrService(application)
    private val driveOcrService = DriveOcrService(application)
    
    // TODO: Get API key from secure storage or configuration
    private val geminiService = GeminiService("YOUR_GEMINI_API_KEY")
    
    private val repository = ArticleRepository(
        database.articleDao(),
        mlKitOcrService,
        driveOcrService,
        geminiService
    )

    val allArticles = repository.getAllArticles()
    val eventInvitations = repository.getEventInvitations()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun getArticleById(id: Long) = repository.getArticleByIdFlow(id)

    fun searchArticles(query: String) = repository.searchArticles(query)

    fun createArticle(
        imagePath: String,
        thumbnailPath: String?,
        newspaperName: String?,
        publicationDate: Date?,
        isEventInvitation: Boolean,
        notes: String?
    ) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                val article = Article(
                    imagePath = imagePath,
                    thumbnailPath = thumbnailPath,
                    newspaperName = newspaperName,
                    publicationDate = publicationDate,
                    captureDate = Date(),
                    isEventInvitation = isEventInvitation,
                    notes = notes,
                    ocrStatus = OcrStatus.PENDING
                )
                
                val articleId = repository.insertArticle(article)
                _successMessage.value = "Article saved successfully"
                
                // Start OCR processing in background
                processOcr(articleId)
            } catch (e: Exception) {
                _errorMessage.value = "Error saving article: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun processOcr(articleId: Long) {
        viewModelScope.launch {
            try {
                val result = repository.processOcr(articleId)
                if (result.isSuccess) {
                    _successMessage.value = "OCR completed successfully"
                } else {
                    _errorMessage.value = "OCR failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error processing OCR: ${e.message}"
            }
        }
    }

    fun uploadToDrive(articleId: Long) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                val result = repository.uploadToDrive(articleId)
                if (result.isSuccess) {
                    _successMessage.value = "Uploaded to Google Drive"
                } else {
                    _errorMessage.value = "Upload failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error uploading: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun updateArticle(article: Article) {
        viewModelScope.launch {
            try {
                repository.updateArticle(article)
                _successMessage.value = "Article updated"
            } catch (e: Exception) {
                _errorMessage.value = "Error updating article: ${e.message}"
            }
        }
    }

    fun deleteArticle(article: Article) {
        viewModelScope.launch {
            try {
                repository.deleteArticle(article)
                _successMessage.value = "Article deleted"
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting article: ${e.message}"
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    fun initializeDriveService(credential: com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential) {
        driveOcrService.initializeDrive(credential)
    }

    fun getDriveService() = driveOcrService

    override fun onCleared() {
        super.onCleared()
        mlKitOcrService.close()
    }
}
