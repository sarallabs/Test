package com.newspaper.eventstimeline.ui.timeline

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.newspaper.eventstimeline.databinding.ActivityTimelineBinding
import com.newspaper.eventstimeline.ui.adapter.EventAdapter
import com.newspaper.eventstimeline.ui.viewmodel.EventViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class TimelineActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTimelineBinding
    private val eventViewModel: EventViewModel by viewModels()
    
    private lateinit var eventAdapter: EventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTimelineBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Events Timeline"
        }
    }

    private fun setupRecyclerView() {
        eventAdapter = EventAdapter(
            onEventClick = { event ->
                // Could navigate to event details if needed
                Toast.makeText(this, "Event: ${event.title}", Toast.LENGTH_SHORT).show()
            },
            onGenerateSummaryClick = { event ->
                eventViewModel.generateEventSummary(event.id)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TimelineActivity)
            adapter = eventAdapter
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            eventViewModel.allEventsWithArticles.collectLatest { eventsWithArticles ->
                eventAdapter.submitList(eventsWithArticles)
                binding.emptyView.visibility = if (eventsWithArticles.isEmpty()) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }

        lifecycleScope.launch {
            eventViewModel.errorMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@TimelineActivity, it, Toast.LENGTH_LONG).show()
                    eventViewModel.clearErrorMessage()
                }
            }
        }

        lifecycleScope.launch {
            eventViewModel.successMessage.collectLatest { message ->
                message?.let {
                    Toast.makeText(this@TimelineActivity, it, Toast.LENGTH_SHORT).show()
                    eventViewModel.clearSuccessMessage()
                }
            }
        }

        lifecycleScope.launch {
            eventViewModel.isProcessing.collectLatest { isProcessing ->
                binding.progressBar.visibility = if (isProcessing) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
