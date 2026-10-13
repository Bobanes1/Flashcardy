package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Flashcard(
    @DocumentId val id: String = "",
    val deckId: String = "",
    val front: String = "",
    val back: String = "",
    val hint: String = "",
    val notes: String = "",
    val isStarred: Boolean = false,
    val repetitions: Long = 0L,
    val intervalDays: Long = 1L,
    val easeFactor: Double = 2.5,
    val nextReviewDate: Long = 0L,
    val lastReviewedDate: Long = 0L,
    val reviewCount: Long = 0L,
    val masteryLevel: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
