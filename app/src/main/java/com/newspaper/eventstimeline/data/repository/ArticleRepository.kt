package com.newspaper.eventstimeline.data.repository

import com.newspaper.eventstimeline.data.api.DriveOcrService
import com.newspaper.eventstimeline.data.api.GeminiService
import com.newspaper.eventstimeline.data.api.MlKitOcrService
import com.newspaper.eventstimeline.data.local.ArticleDao
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.ArticleLanguage
import com.newspaper.eventstimeline.data.model.OcrStatus
import kotlinx.coroutines.flow.Flow
import java.util.Date

class ArticleRepository(
    private val articleDao: ArticleDao,
    private val mlKitOcrService: MlKitOcrService,
    private val driveOcrService: DriveOcrService,
    private val geminiService: GeminiService
) {
    fun getAllArticles(): Flow<List<Article>> = articleDao.getAllArticles()

    suspend fun getArticleById(id: Long): Article? = articleDao.getArticleById(id)

    fun getArticleByIdFlow(id: Long): Flow<Article?> = articleDao.getArticleByIdFlow(id)

    fun getArticlesByOcrStatus(status: OcrStatus): Flow<List<Article>> =
        articleDao.getArticlesByOcrStatus(status)

    fun getEventInvitations(): Flow<List<Article>> = articleDao.getEventInvitations()

    fun getArticlesByDateRange(startDate: Date, endDate: Date): Flow<List<Article>> =
        articleDao.getArticlesByDateRange(startDate, endDate)

    fun searchArticles(query: String): Flow<List<Article>> = articleDao.searchArticles(query)

    suspend fun insertArticle(article: Article): Long = articleDao.insertArticle(article)

    suspend fun updateArticle(article: Article) = articleDao.updateArticle(article)

    suspend fun deleteArticle(article: Article) = articleDao.deleteArticle(article)

    suspend fun deleteArticleById(articleId: Long) = articleDao.deleteArticleById(articleId)

    /**
     * Process OCR for an article
     */
    suspend fun processOcr(articleId: Long): Result<String> {
        return try {
            val article = articleDao.getArticleById(articleId)
                ?: return Result.failure(Exception("Article not found"))

            // Update status to processing
            articleDao.updateOcrResult(articleId, OcrStatus.PROCESSING, null)

            // Perform OCR using ML Kit
            val ocrResult = mlKitOcrService.recognizeText(article.imagePath)

            if (ocrResult.isSuccess) {
                val ocrText = ocrResult.getOrNull() ?: ""
                
                // Update article with OCR result
                articleDao.updateOcrResult(articleId, OcrStatus.COMPLETED, ocrText)

                // Optionally, use Gemini to detect language and extract info
                try {
                    val languageResult = geminiService.detectLanguage(ocrText)
                    if (languageResult.isSuccess) {
                        val language = when (languageResult.getOrNull()) {
                            "ENGLISH" -> ArticleLanguage.ENGLISH
                            "URDU" -> ArticleLanguage.URDU
                            "TELUGU" -> ArticleLanguage.TELUGU
                            "MIXED" -> ArticleLanguage.MIXED
                            else -> ArticleLanguage.UNKNOWN
                        }
                        articleDao.updateArticle(article.copy(language = language))
                    }

                    // Extract article info
                    val infoResult = geminiService.extractArticleInfo(ocrText)
                    if (infoResult.isSuccess) {
                        val info = infoResult.getOrNull()!!
                        articleDao.updateArticle(
                            article.copy(
                                isEventInvitation = info.isEventInvitation,
                                tags = info.tags.joinToString(",")
                            )
                        )
                    }
                } catch (e: Exception) {
                    // Continue even if Gemini processing fails
                }

                Result.success(ocrText)
            } else {
                articleDao.updateOcrResult(articleId, OcrStatus.FAILED, null)
                ocrResult
            }
        } catch (e: Exception) {
            articleDao.updateOcrResult(articleId, OcrStatus.FAILED, null)
            Result.failure(e)
        }
    }

    /**
     * Upload article image to Google Drive
     */
    suspend fun uploadToDrive(articleId: Long): Result<String> {
        return try {
            val article = articleDao.getArticleById(articleId)
                ?: return Result.failure(Exception("Article not found"))

            val fileName = "article_${articleId}_${System.currentTimeMillis()}.jpg"
            val uploadResult = driveOcrService.uploadImageToDrive(article.imagePath, fileName)

            if (uploadResult.isSuccess) {
                val driveFileId = uploadResult.getOrNull()!!
                articleDao.updateDriveFileId(articleId, driveFileId)
            }

            uploadResult
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateArticleEvent(articleId: Long, eventId: Long?) {
        articleDao.updateArticleEvent(articleId, eventId)
    }
}
