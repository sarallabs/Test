package com.newspaper.eventstimeline.data.api

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class GeminiService(private val apiKey: String) {
    private val tag = "GeminiService"
    private val model = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = apiKey
    )
    private val gson = Gson()

    data class EventSuggestion(
        @SerializedName("title") val title: String,
        @SerializedName("description") val description: String,
        @SerializedName("category") val category: String,
        @SerializedName("date") val date: String?,
        @SerializedName("location") val location: String?,
        @SerializedName("key_persons") val keyPersons: List<String>,
        @SerializedName("article_indices") val articleIndices: List<Int>
    )

    data class EventAnalysisResponse(
        @SerializedName("events") val events: List<EventSuggestion>,
        @SerializedName("summary") val summary: String
    )

    /**
     * Analyze articles and create event timeline
     */
    suspend fun analyzeArticlesForEvents(articles: List<Article>): Result<EventAnalysisResponse> =
        withContext(Dispatchers.IO) {
            try {
                val articlesData = articles.mapIndexed { index, article ->
                    """
                    Article ${index + 1}:
                    Newspaper: ${article.newspaperName ?: "Unknown"}
                    Date: ${article.publicationDate?.let { formatDate(it) } ?: "Unknown"}
                    Is Event Invitation: ${article.isEventInvitation}
                    Text: ${article.ocrText?.take(1000) ?: "No text"}
                    ---
                    """.trimIndent()
                }.joinToString("\n\n")

                val prompt = """
                    You are an AI assistant specialized in analyzing newspaper articles and creating chronological event timelines.
                    
                    Analyze the following newspaper articles (in English, Urdu, and Telugu) and:
                    1. Identify distinct events mentioned or described
                    2. Group related articles that discuss the same event
                    3. Extract key information: event title, description, date, location, key persons involved
                    4. Create a chronological timeline of events
                    5. Provide a summary of the overall narrative
                    
                    Articles:
                    $articlesData
                    
                    Please respond in JSON format with the following structure:
                    {
                        "events": [
                            {
                                "title": "Event title",
                                "description": "Detailed description",
                                "category": "Category (e.g., Political, Cultural, Sports, Business)",
                                "date": "YYYY-MM-DD or null if unknown",
                                "location": "Location or null",
                                "key_persons": ["Person 1", "Person 2"],
                                "article_indices": [0, 2, 5]
                            }
                        ],
                        "summary": "Overall narrative summary connecting all events"
                    }
                    
                    Important:
                    - article_indices should reference the article numbers (0-based index) that relate to this event
                    - If multiple articles discuss the same event, group them together
                    - Sort events chronologically where dates are known
                    - For event invitations, extract the event details they're inviting to
                    - Handle mixed languages (English, Urdu, Telugu) in the text
                """.trimIndent()

                val response = model.generateContent(prompt)
                val responseText = response.text ?: throw Exception("Empty response from Gemini")
                
                Log.d(tag, "Gemini response: $responseText")

                // Extract JSON from response (it might be wrapped in markdown)
                val jsonText = extractJson(responseText)
                val analysisResponse = gson.fromJson(jsonText, EventAnalysisResponse::class.java)

                Result.success(analysisResponse)
            } catch (e: Exception) {
                Log.e(tag, "Error analyzing articles with Gemini", e)
                Result.failure(e)
            }
        }

    /**
     * Generate summary for a single event
     */
    suspend fun generateEventSummary(
        event: Event,
        articles: List<Article>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val articlesText = articles.joinToString("\n\n") { article ->
                """
                From ${article.newspaperName ?: "Unknown"} (${article.publicationDate?.let { formatDate(it) } ?: "Unknown"}):
                ${article.ocrText?.take(500) ?: "No text"}
                """.trimIndent()
            }

            val prompt = """
                Create a comprehensive summary for the following event based on multiple newspaper articles:
                
                Event: ${event.title}
                Description: ${event.description ?: ""}
                Date: ${event.eventDate?.let { formatDate(it) } ?: "Unknown"}
                
                Related Articles:
                $articlesText
                
                Please provide a well-structured summary that:
                1. Combines information from all articles
                2. Maintains chronological order if applicable
                3. Highlights key facts, persons, and outcomes
                4. Resolves any conflicting information
                5. Keeps it concise but comprehensive (200-300 words)
            """.trimIndent()

            val response = model.generateContent(prompt)
            val summary = response.text ?: throw Exception("Empty response from Gemini")

            Result.success(summary.trim())
        } catch (e: Exception) {
            Log.e(tag, "Error generating event summary", e)
            Result.failure(e)
        }
    }

    /**
     * Detect language of text
     */
    suspend fun detectLanguage(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val prompt = """
                Identify the primary language(s) in the following text.
                Respond with one of: "ENGLISH", "URDU", "TELUGU", "MIXED", or "UNKNOWN"
                
                Text: ${text.take(200)}
            """.trimIndent()

            val response = model.generateContent(prompt)
            val language = response.text?.trim()?.uppercase() ?: "UNKNOWN"

            Result.success(language)
        } catch (e: Exception) {
            Log.e(tag, "Error detecting language", e)
            Result.failure(e)
        }
    }

    /**
     * Extract structured information from article text
     */
    suspend fun extractArticleInfo(ocrText: String): Result<ArticleInfo> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = """
                    Extract structured information from this newspaper article text:
                    
                    $ocrText
                    
                    Provide response in JSON format:
                    {
                        "is_event_invitation": boolean,
                        "event_date": "YYYY-MM-DD or null",
                        "location": "location or null",
                        "key_persons": ["person1", "person2"],
                        "tags": ["tag1", "tag2"],
                        "summary": "brief summary"
                    }
                """.trimIndent()

                val response = model.generateContent(prompt)
                val jsonText = extractJson(response.text ?: "")
                val info = gson.fromJson(jsonText, ArticleInfo::class.java)

                Result.success(info)
            } catch (e: Exception) {
                Log.e(tag, "Error extracting article info", e)
                Result.failure(e)
            }
        }

    data class ArticleInfo(
        @SerializedName("is_event_invitation") val isEventInvitation: Boolean,
        @SerializedName("event_date") val eventDate: String?,
        @SerializedName("location") val location: String?,
        @SerializedName("key_persons") val keyPersons: List<String>,
        @SerializedName("tags") val tags: List<String>,
        @SerializedName("summary") val summary: String
    )

    private fun extractJson(text: String): String {
        // Remove markdown code blocks if present
        var json = text.trim()
        if (json.startsWith("```json")) {
            json = json.removePrefix("```json").removeSuffix("```").trim()
        } else if (json.startsWith("```")) {
            json = json.removePrefix("```").removeSuffix("```").trim()
        }
        return json
    }

    private fun formatDate(date: java.util.Date): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return format.format(date)
    }
}
