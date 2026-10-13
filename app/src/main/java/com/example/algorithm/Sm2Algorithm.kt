package com.example.algorithm

import com.example.model.Flashcard
import kotlin.math.max
import kotlin.math.roundToLong

/**
 * Implementation of the SuperMemo-2 (SM-2) Spaced Repetition Algorithm.
 * Optimizes study efficiency by scheduling card reviews based on user recall quality.
 */
object Sm2Algorithm {

    enum class ReviewRating(val quality: Int, val label: String) {
        AGAIN(quality = 1, label = "Again"),
        HARD(quality = 3, label = "Hard"),
        GOOD(quality = 4, label = "Good"),
        EASY(quality = 5, label = "Easy")
    }

    data class Sm2Result(
        val repetitions: Long,
        val intervalDays: Long,
        val easeFactor: Double,
        val nextReviewDate: Long,
        val lastReviewedDate: Long,
        val reviewCount: Long,
        val masteryLevel: Long
    )

    private const val MIN_EASE_FACTOR = 1.3
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L

    /**
     * Calculates updated SM-2 parameters for a card given a review rating.
     */
    fun calculateNextReview(
        card: Flashcard,
        rating: ReviewRating,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Sm2Result {
        val q = rating.quality

        // 1. Calculate new Ease Factor (EF')
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        val currentEf = if (card.easeFactor < MIN_EASE_FACTOR) MIN_EASE_FACTOR else card.easeFactor
        val delta = 0.1 - (5 - q) * (0.08 + (5 - q) * 0.02)
        val newEf = max(MIN_EASE_FACTOR, (currentEf + delta * 100.0).roundToLong() / 100.0)

        val newRepetitions: Long
        val newIntervalDays: Long

        when (rating) {
            ReviewRating.AGAIN -> {
                // Incorrect recall: Reset streak to 0, review again tomorrow (or in current session)
                newRepetitions = 0L
                newIntervalDays = 1L
            }
            ReviewRating.HARD -> {
                // Difficult recall: Increment repetition, shorter interval multiplier
                newRepetitions = card.repetitions + 1L
                newIntervalDays = if (card.repetitions == 0L) {
                    1L
                } else {
                    max(1L, (card.intervalDays * 1.2).roundToLong())
                }
            }
            ReviewRating.GOOD -> {
                // Standard SM-2 interval expansion
                newRepetitions = card.repetitions + 1L
                newIntervalDays = when (newRepetitions) {
                    1L -> 1L
                    2L -> 6L
                    else -> max(1L, (card.intervalDays * newEf).roundToLong())
                }
            }
            ReviewRating.EASY -> {
                // Effortless recall: Bonus interval multiplier
                newRepetitions = card.repetitions + 1L
                newIntervalDays = when (newRepetitions) {
                    1L -> 4L
                    2L -> 9L
                    else -> max(1L, (card.intervalDays * newEf * 1.3).roundToLong())
                }
            }
        }

        val nextReviewDate = currentTimeMillis + (newIntervalDays * ONE_DAY_MS)
        val newReviewCount = card.reviewCount + 1L

        // Mastery stage: 0=New, 1=Learning, 2=Reviewing, 3=Mastered
        val masteryLevel = when {
            newRepetitions >= 6L || newIntervalDays >= 21L -> 3L
            newRepetitions >= 3L || newIntervalDays >= 6L -> 2L
            newRepetitions >= 1L -> 1L
            else -> 0L
        }

        return Sm2Result(
            repetitions = newRepetitions,
            intervalDays = newIntervalDays,
            easeFactor = newEf,
            nextReviewDate = nextReviewDate,
            lastReviewedDate = currentTimeMillis,
            reviewCount = newReviewCount,
            masteryLevel = masteryLevel
        )
    }

    /**
     * Checks if a card is currently due for review based on SM-2 schedule.
     */
    fun isDue(card: Flashcard, currentTimeMillis: Long = System.currentTimeMillis()): Boolean {
        return card.repetitions == 0L || card.nextReviewDate <= currentTimeMillis
    }

    /**
     * Provides projected intervals for UI button subtitles (e.g. "1d", "6d").
     */
    fun getPreviewIntervalLabel(card: Flashcard, rating: ReviewRating): String {
        val result = calculateNextReview(card, rating)
        return when (rating) {
            ReviewRating.AGAIN -> "< 10m"
            else -> "${result.intervalDays}d"
        }
    }
}
