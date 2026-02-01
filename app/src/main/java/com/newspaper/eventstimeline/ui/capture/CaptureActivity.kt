package com.newspaper.eventstimeline.ui.capture

import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.android.material.datepicker.MaterialDatePicker
import com.newspaper.eventstimeline.databinding.ActivityCaptureBinding
import com.newspaper.eventstimeline.ui.viewmodel.ArticleViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CaptureActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCaptureBinding
    private val articleViewModel: ArticleViewModel by viewModels()
    
    private var imageCapture: ImageCapture? = null
    private var capturedImagePath: String? = null
    private var selectedPublicationDate: Date? = null
    
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCaptureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupCamera()
        setupListeners()
        
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Capture Article"
        }
    }

    private fun setupCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                Log.e(TAG, "Camera binding failed", e)
                Toast.makeText(this, "Failed to start camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun setupListeners() {
        binding.btnCapture.setOnClickListener {
            captureImage()
        }

        binding.btnSelectDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            saveArticle()
        }

        binding.btnRetake.setOnClickListener {
            retakePhoto()
        }

        binding.switchEventInvitation.setOnCheckedChangeListener { _, _ ->
            // No action needed, just update the state
        }
    }

    private fun captureImage() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.firstOrNull(),
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImagePath = photoFile.absolutePath
                    showCapturePreview()
                    Toast.makeText(
                        this@CaptureActivity,
                        "Photo captured successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                    Toast.makeText(
                        this@CaptureActivity,
                        "Failed to capture photo",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }

    private fun showCapturePreview() {
        binding.apply {
            previewView.visibility = android.view.View.GONE
            btnCapture.visibility = android.view.View.GONE
            
            capturePreviewGroup.visibility = android.view.View.VISIBLE
            
            // Load and display captured image
            capturedImagePath?.let { path ->
                val bitmap = BitmapFactory.decodeFile(path)
                imagePreview.setImageBitmap(bitmap)
            }
        }
    }

    private fun retakePhoto() {
        binding.apply {
            capturePreviewGroup.visibility = android.view.View.GONE
            previewView.visibility = android.view.View.VISIBLE
            btnCapture.visibility = android.view.View.VISIBLE
            
            // Clear form
            editNewspaperName.text?.clear()
            selectedPublicationDate = null
            textSelectedDate.text = ""
            switchEventInvitation.isChecked = false
            editNotes.text?.clear()
        }
        
        capturedImagePath?.let { path ->
            File(path).delete()
        }
        capturedImagePath = null
    }

    private fun showDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select publication date")
            .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            selectedPublicationDate = Date(selection)
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.textSelectedDate.text = dateFormat.format(selectedPublicationDate!!)
        }

        datePicker.show(supportFragmentManager, "DATE_PICKER")
    }

    private fun saveArticle() {
        val imagePath = capturedImagePath
        if (imagePath == null) {
            Toast.makeText(this, "Please capture an image first", Toast.LENGTH_SHORT).show()
            return
        }

        val newspaperName = binding.editNewspaperName.text.toString().trim()
        val notes = binding.editNotes.text.toString().trim()
        val isEventInvitation = binding.switchEventInvitation.isChecked

        // Create thumbnail
        val thumbnailPath = createThumbnail(imagePath)

        // Save article
        articleViewModel.createArticle(
            imagePath = imagePath,
            thumbnailPath = thumbnailPath,
            newspaperName = newspaperName.ifEmpty { null },
            publicationDate = selectedPublicationDate,
            isEventInvitation = isEventInvitation,
            notes = notes.ifEmpty { null }
        )

        Toast.makeText(this, "Article saved! Processing OCR...", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun createThumbnail(imagePath: String): String? {
        return try {
            val originalBitmap = BitmapFactory.decodeFile(imagePath)
            val thumbnailBitmap = Bitmap.createScaledBitmap(
                originalBitmap,
                200,
                (200 * originalBitmap.height) / originalBitmap.width,
                true
            )

            val thumbnailFile = File(
                externalMediaDirs.firstOrNull(),
                "thumb_" + File(imagePath).name
            )

            FileOutputStream(thumbnailFile).use { out ->
                thumbnailBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            thumbnailFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create thumbnail", e)
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        private const val TAG = "CaptureActivity"
        private const val FILENAME_FORMAT = "yyyyMMdd_HHmmss"
    }
}
