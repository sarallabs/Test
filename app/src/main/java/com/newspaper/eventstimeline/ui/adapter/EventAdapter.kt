package com.newspaper.eventstimeline.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.newspaper.eventstimeline.data.model.Event
import com.newspaper.eventstimeline.data.model.EventWithArticles
import com.newspaper.eventstimeline.databinding.ItemEventBinding
import java.text.SimpleDateFormat
import java.util.Locale

class EventAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onGenerateSummaryClick: (Event) -> Unit
) : ListAdapter<EventWithArticles, EventAdapter.EventViewHolder>(EventDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EventViewHolder(
        private val binding: ItemEventBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(eventWithArticles: EventWithArticles) {
            val event = eventWithArticles.event
            val articles = eventWithArticles.articles

            binding.apply {
                // Event title
                textEventTitle.text = event.title

                // Event date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textEventDate.text = event.eventDate?.let { dateFormat.format(it) } ?: "Date unknown"

                // Category
                if (!event.category.isNullOrBlank()) {
                    textEventCategory.text = event.category
                    textEventCategory.visibility = android.view.View.VISIBLE
                } else {
                    textEventCategory.visibility = android.view.View.GONE
                }

                // Location
                if (!event.location.isNullOrBlank()) {
                    textEventLocation.text = event.location
                    textEventLocation.visibility = android.view.View.VISIBLE
                } else {
                    textEventLocation.visibility = android.view.View.GONE
                }

                // Description
                if (!event.description.isNullOrBlank()) {
                    textEventDescription.text = event.description
                    textEventDescription.visibility = android.view.View.VISIBLE
                } else {
                    textEventDescription.visibility = android.view.View.GONE
                }

                // Key persons
                if (!event.keyPersons.isNullOrBlank()) {
                    textKeyPersons.text = "Key persons: ${event.keyPersons}"
                    textKeyPersons.visibility = android.view.View.VISIBLE
                } else {
                    textKeyPersons.visibility = android.view.View.GONE
                }

                // AI generated badge
                chipAiGenerated.visibility = if (event.aiGenerated) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Article count
                val articleCount = articles.size
                textArticleCount.text = "$articleCount related article${if (articleCount != 1) "s" else ""}"

                // Summary
                if (!event.summary.isNullOrBlank()) {
                    textSummary.text = event.summary
                    cardSummary.visibility = android.view.View.VISIBLE
                    btnGenerateSummary.visibility = android.view.View.GONE
                } else {
                    cardSummary.visibility = android.view.View.GONE
                    btnGenerateSummary.visibility = if (articles.isNotEmpty()) {
                        android.view.View.VISIBLE
                    } else {
                        android.view.View.GONE
                    }
                }

                // Click listeners
                root.setOnClickListener {
                    onEventClick(event)
                }

                btnGenerateSummary.setOnClickListener {
                    onGenerateSummaryClick(event)
                }
            }
        }
    }

    private class EventDiffCallback : DiffUtil.ItemCallback<EventWithArticles>() {
        override fun areItemsTheSame(
            oldItem: EventWithArticles,
            newItem: EventWithArticles
        ): Boolean {
            return oldItem.event.id == newItem.event.id
        }

        override fun areContentsTheSame(
            oldItem: EventWithArticles,
            newItem: EventWithArticles
        ): Boolean {
            return oldItem == newItem
        }
    }
}
