package com.newspaper.eventstimeline.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.common.api.ApiException
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.newspaper.eventstimeline.R
import com.newspaper.eventstimeline.databinding.ActivityMainBinding
import com.newspaper.eventstimeline.ui.adapter.ArticleAdapter
import com.newspaper.eventstimeline.ui.capture.CaptureActivity
import com.newspaper.eventstimeline.ui.details.ArticleDetailsActivity
import com.newspaper.eventstimeline.ui.timeline.TimelineActivity
import com.newspaper.eventstimeline.ui.viewmodel.ArticleViewModel
import com.newspaper.eventstimeline.ui.viewmodel.EventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val articleViewModel: ArticleViewModel by viewModels()
    private val eventViewModel: EventViewModel by viewModels()
    
    private lateinit var articleAdapter: ArticleAdapter
    private lateinit var googleSignInClient: GoogleSignInClient

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCaptureActivity()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (!allGranted) {
            Toast.makeText(this, "Storage permissions are required", Toast.LENGTH_SHORT).show()
        }
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            handleSignInSuccess(account)
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFab()
        setupTabs()
        requestPermissions()
        setupGoogleSignIn()
        observeViewModels()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Newspaper Articles"
    }

    private fun setupRecyclerView() {
        articleAdapter = ArticleAdapter(
            onArticleClick = { article ->
                val intent = Intent(this, ArticleDetailsActivity::class.java)
                intent.putExtra("ARTICLE_ID", article.id)
                startActivity(intent)
            },
            onArticleOcrClick = { article ->
                articleViewModel.processOcr(article.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(this@MainActivity, 2)
            adapter = articleAdapter
        }
    }

    private fun setupFab() {
        binding.fabCapture.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        binding.fabTimeline.setOnClickListener {
            val intent = Intent(this, TimelineActivity::class.java)
            startActivity(intent)
        }

        binding.fabAnalyze.setOnClickListener {
            showAnalyzeDialog()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("All"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Invitations"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Pending OCR"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> loadAllArticles()
                    1 -> loadEventInvitations()
                    2 -> loadPendingOcr()
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupGoogleSignIn() {
        val driveService = articleViewModel.getDriveService()
        googleSignInClient = GoogleSignIn.getClient(this, driveService.getSignInOptions())

        // Check if already signed in
        if (driveService.isSignedIn()) {
            driveService.getCredential()?.let { credential ->
                articleViewModel.initializeDriveService(credential)
            }
        }
    }

    private fun observeViewModels() {
        lifecycleScope.launch {
            articleViewModel.allArticles.collectLatest { articles ->
                articleAdapter.submitList(articles)
                binding.emptyView.visibility = if (articles.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        lifecycleScope.launch {
            articleViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    articleViewModel.clearErrorMessage()
                }
            }
        }

        lifecycleScope.launch {
            articleViewModel.successMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@MainActivity, it, Toast.LENGTH_SHORT).show()
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

    private fun loadAllArticles() {
        lifecycleScope.launch {
            articleViewModel.allArticles.collectLatest { articles ->
                articleAdapter.submitList(articles)
            }
        }
    }

    private fun loadEventInvitations() {
        lifecycleScope.launch {
            articleViewModel.eventInvitations.collectLatest { articles ->
                articleAdapter.submitList(articles)
            }
        }
    }

    private fun loadPendingOcr() {
        lifecycleScope.launch {
            articleViewModel.searchArticles("").collectLatest { articles ->
                val pending = articles.filter { 
                    it.ocrStatus == com.newspaper.eventstimeline.data.model.OcrStatus.PENDING
                }
                articleAdapter.submitList(pending)
            }
        }
    }

    private fun checkCameraPermissionAndCapture() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCaptureActivity()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCaptureActivity() {
        val intent = Intent(this, CaptureActivity::class.java)
        startActivity(intent)
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            storagePermissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handleSignInSuccess(account: GoogleSignInAccount) {
        val credential = GoogleAccountCredential.usingOAuth2(
            this,
            listOf(com.google.api.services.drive.DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account.account
        articleViewModel.initializeDriveService(credential)
        Toast.makeText(this, "Signed in successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showAnalyzeDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Analyze Articles")
            .setMessage("This will analyze all articles with OCR text and create events using AI. Continue?")
            .setPositiveButton("Analyze") { _, _ ->
                lifecycleScope.launch {
                    articleViewModel.allArticles.collect { articles ->
                        val articlesWithText = articles.filter { 
                            !it.ocrText.isNullOrBlank() 
                        }
                        if (articlesWithText.isNotEmpty()) {
                            eventViewModel.analyzeArticlesAndCreateEvents(
                                articlesWithText.map { it.id }
                            )
                        } else {
                            Toast.makeText(
                                this@MainActivity,
                                "No articles with OCR text found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@collect // Only collect once
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
