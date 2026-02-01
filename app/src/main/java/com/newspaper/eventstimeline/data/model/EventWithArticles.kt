package com.newspaper.eventstimeline.data.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class EventWithArticles(
    @Embedded val event: Event,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ArticleEventCrossRef::class,
            parentColumn = "eventId",
            entityColumn = "articleId"
        )
    )
    val articles: List<Article>
)
