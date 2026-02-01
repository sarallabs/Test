package com.newspaper.eventstimeline.data.api

import android.content.Context
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException

class DriveOcrService(private val context: Context) {
    private var driveService: Drive? = null
    private val tag = "DriveOcrService"

    companion object {
        const val REQUEST_CODE_SIGN_IN = 100
    }

    fun initializeDrive(credential: GoogleAccountCredential) {
        driveService = Drive.Builder(
            AndroidHttp.newCompatibleTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("Newspaper Events Timeline")
            .build()
    }

    fun getSignInOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE),
                com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY)
            )
            .build()
    }

    fun isSignedIn(): Boolean {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        return account != null && GoogleSignIn.hasPermissions(
            account,
            com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE)
        )
    }

    fun getCredential(): GoogleAccountCredential? {
        val account = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            listOf(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_READONLY)
        )
        credential.selectedAccount = account.account
        return credential
    }

    /**
     * Upload image to Google Drive and request OCR
     * Google Drive API doesn't directly provide OCR, but Google Cloud Vision API does.
     * For OCR, we'll use Google ML Kit or Cloud Vision API instead.
     * This method uploads the file to Drive for storage/backup.
     */
    suspend fun uploadImageToDrive(
        localFilePath: String,
        fileName: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized. Please sign in.")
            )

            val fileMetadata = File().apply {
                name = fileName
                mimeType = "image/jpeg"
            }

            val mediaContent = com.google.api.client.http.FileContent(
                "image/jpeg",
                java.io.File(localFilePath)
            )

            val file = drive.files().create(fileMetadata, mediaContent)
                .setFields("id, name, webViewLink")
                .execute()

            Log.d(tag, "File uploaded: ${file.name} (ID: ${file.id})")
            Result.success(file.id)
        } catch (e: IOException) {
            Log.e(tag, "Error uploading file to Drive", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(tag, "Unexpected error uploading file", e)
            Result.failure(e)
        }
    }

    /**
     * Download a file from Google Drive
     */
    suspend fun downloadFile(fileId: String, outputPath: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val drive = driveService ?: return@withContext Result.failure(
                    Exception("Drive service not initialized")
                )

                val outputFile = java.io.File(outputPath)
                FileOutputStream(outputFile).use { outputStream ->
                    drive.files().get(fileId).executeMediaAndDownloadTo(outputStream)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(tag, "Error downloading file from Drive", e)
                Result.failure(e)
            }
        }

    /**
     * Get file metadata from Drive
     */
    suspend fun getFileMetadata(fileId: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized")
            )

            val file = drive.files().get(fileId)
                .setFields("id, name, createdTime, modifiedTime, size, webViewLink")
                .execute()

            Result.success(file)
        } catch (e: Exception) {
            Log.e(tag, "Error getting file metadata", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a file from Drive
     */
    suspend fun deleteFile(fileId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val drive = driveService ?: return@withContext Result.failure(
                Exception("Drive service not initialized")
            )

            drive.files().delete(fileId).execute()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(tag, "Error deleting file from Drive", e)
            Result.failure(e)
        }
    }
}
