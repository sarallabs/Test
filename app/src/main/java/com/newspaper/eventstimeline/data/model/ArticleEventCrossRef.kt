package com.newspaper.eventstimeline.data.model

import androidx.room.Entity

@Entity(
    tableName = "article_event_cross_ref",
    primaryKeys = ["articleId", "eventId"]
)
data class ArticleEventCrossRef(
    val articleId: Long,
    val eventId: Long
)
