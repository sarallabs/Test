package com.newspaper.eventstimeline.ui.adapter

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.newspaper.eventstimeline.data.model.Article
import com.newspaper.eventstimeline.data.model.OcrStatus
import com.newspaper.eventstimeline.databinding.ItemArticleBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class ArticleAdapter(
    private val onArticleClick: (Article) -> Unit,
    private val onArticleOcrClick: (Article) -> Unit
) : ListAdapter<Article, ArticleAdapter.ArticleViewHolder>(ArticleDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArticleViewHolder {
        val binding = ItemArticleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ArticleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ArticleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ArticleViewHolder(
        private val binding: ItemArticleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(article: Article) {
            binding.apply {
                // Load image
                val file = File(article.imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(article.imagePath)
                    imageView.setImageBitmap(bitmap)
                }

                // Set newspaper name
                textNewspaperName.text = article.newspaperName ?: "Unknown Newspaper"

                // Set date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = article.publicationDate?.let { dateFormat.format(it) }
                    ?: dateFormat.format(article.captureDate)

                // Set OCR status
                textOcrStatus.text = when (article.ocrStatus) {
                    OcrStatus.PENDING -> "OCR Pending"
                    OcrStatus.PROCESSING -> "Processing..."
                    OcrStatus.COMPLETED -> "OCR Completed"
                    OcrStatus.FAILED -> "OCR Failed"
                }

                // Show event invitation chip
                chipEventInvitation.visibility = if (article.isEventInvitation) {
                    android.view.View.VISIBLE
                } else {
                    android.view.View.GONE
                }

                // Click listeners
                root.setOnClickListener {
                    onArticleClick(article)
                }

                textOcrStatus.setOnClickListener {
                    if (article.ocrStatus == OcrStatus.PENDING || article.ocrStatus == OcrStatus.FAILED) {
                        onArticleOcrClick(article)
                    }
                }
            }
        }
    }

    private class ArticleDiffCallback : DiffUtil.ItemCallback<Article>() {
        override fun areItemsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Article, newItem: Article): Boolean {
            return oldItem == newItem
        }
    }
}
