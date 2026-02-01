package com.newspaper.eventstimeline.ui.details

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.newspaper.eventstimeline.R
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.databinding.ActivityArticleDetailsBinding
import com.newspaper.eventstimeline.ui.viewmodel.ArticleViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ArticleDetailsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityArticleDetailsBinding
    private val articleViewModel: ArticleViewModel by viewModels()
    
    private var currentArticle: Article? = null
    private var articleId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArticleDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        articleId = intent.getLongExtra("ARTICLE_ID", -1)
        if (articleId == -1L) {
            Toast.makeText(this, "Invalid article", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupListeners()
        loadArticle()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Article Details"
        }
    }

    private fun setupListeners() {
        binding.btnProcessOcr.setOnClickListener {
            currentArticle?.let { article ->
                articleViewModel.processOcr(article.id)
            }
        }

        binding.btnUploadDrive.setOnClickListener {
            currentArticle?.let { article ->
                articleViewModel.uploadToDrive(article.id)
            }
        }
    }

    private fun loadArticle() {
        lifecycleScope.launch {
            articleViewModel.getArticleById(articleId).collectLatest { article ->
                if (article != null) {
                    currentArticle = article
                    displayArticle(article)
                }
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            articleViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@ArticleDetailsActivity, it, Toast.LENGTH_LONG).show()
                    articleViewModel.clearErrorMessage()
                }
            }
        }

        lifecycleScope.launch {
            articleViewModel.successMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@ArticleDetailsActivity, it, Toast.LENGTH_SHORT).show()
                    articleViewModel.clearSuccessMessage()
                }
            }
        }

        lifecycleScope.launch {
            articleViewModel.isProcessing.collectLatest { isProcessing ->
                binding.progressBar.visibility = if (isProcessing) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    private fun displayArticle(article: Article) {
        binding.apply {
            // Load image
            val file = File(article.imagePath)
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(article.imagePath)
                imageView.setImageBitmap(bitmap)
            }

            // Display metadata
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            
            textNewspaperName.text = article.newspaperName ?: "Unknown Newspaper"
            textPublicationDate.text = "Publication: ${article.publicationDate?.let { dateFormat.format(it) } ?: "N/A"}"
            textCaptureDate.text = "Captured: ${dateFormat.format(article.captureDate)}"
            
            textLanguage.text = "Language: ${article.language?.name ?: "Unknown"}"
            textOcrStatus.text = "OCR Status: ${article.ocrStatus.name}"
            
            chipEventInvitation.visibility = if (article.isEventInvitation) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }

            // Display OCR text
            if (!article.ocrText.isNullOrBlank()) {
                textOcrContent.text = article.ocrText
                cardOcrContent.visibility = android.view.View.VISIBLE
                btnProcessOcr.visibility = android.view.View.GONE
            } else {
                cardOcrContent.visibility = android.view.View.GONE
                btnProcessOcr.visibility = android.view.View.VISIBLE
            }

            // Display notes
            if (!article.notes.isNullOrBlank()) {
                textNotes.text = article.notes
                cardNotes.visibility = android.view.View.VISIBLE
            } else {
                cardNotes.visibility = android.view.View.GONE
            }

            // Display tags
            if (!article.tags.isNullOrBlank()) {
                textTags.text = "Tags: ${article.tags}"
                textTags.visibility = android.view.View.VISIBLE
            } else {
                textTags.visibility = android.view.View.GONE
            }

            // Show Drive status
            if (article.driveFileId != null) {
                textDriveStatus.text = "Backed up to Google Drive"
                textDriveStatus.visibility = android.view.View.VISIBLE
                btnUploadDrive.visibility = android.view.View.GONE
            } else {
                textDriveStatus.visibility = android.view.View.GONE
                btnUploadDrive.visibility = android.view.View.VISIBLE
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_article_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmation()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showDeleteConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete Article")
            .setMessage("Are you sure you want to delete this article? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                currentArticle?.let { article ->
                    articleViewModel.deleteArticle(article)
                    finish()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
