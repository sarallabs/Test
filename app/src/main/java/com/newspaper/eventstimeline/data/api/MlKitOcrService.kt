package com.newspaper.eventstimeline.data.api

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/**
 * ML Kit OCR Service for offline text recognition
 * Note: For Urdu and Telugu, you may need to use Google Cloud Vision API
 * as ML Kit's default recognizer primarily supports Latin-based scripts.
 */
class MlKitOcrService(private val context: Context) {
    private val tag = "MlKitOcrService"
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(imagePath: String): Result<String> {
        return try {
            val file = File(imagePath)
            if (!file.exists()) {
                return Result.failure(Exception("Image file not found: $imagePath"))
            }

            val bitmap = BitmapFactory.decodeFile(imagePath)
            if (bitmap == null) {
                return Result.failure(Exception("Failed to decode image"))
            }

            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val result = recognizer.process(image).await()
            
            val text = result.text
            Log.d(tag, "OCR completed. Text length: ${text.length}")
            
            if (text.isBlank()) {
                Result.failure(Exception("No text found in image"))
            } else {
                Result.success(text)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error performing OCR", e)
            Result.failure(e)
        }
    }

    fun close() {
        recognizer.close()
    }
}
