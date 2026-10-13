package com.example.data

import com.example.algorithm.Sm2Algorithm
import com.example.base.FirestoreEmulatorTestBase
import com.example.model.Deck
import com.example.model.Flashcard
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class DeckRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createDeck_validPayload_createsDocumentAndReturnsId(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = DeckRepository(firestore)
        val deckId = "deck_${UUID.randomUUID()}"
        val deck = Deck(
            id = deckId,
            userId = aliceUid,
            title = "Test Biology Deck",
            description = "Cell structures",
            category = "Science",
            colorHex = "#10B981"
        )

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.createDeck(aliceUid, deck)
        }
        assertTrue("Deck creation failed: ${result.exceptionOrNull()}", result.isSuccess)
        assertEquals(deckId, result.getOrNull())
    }

    @Test
    fun addCard_withSm2Parameters_success(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = DeckRepository(firestore)
        val deckId = "deck_${UUID.randomUUID()}"
        val deck = Deck(id = deckId, userId = aliceUid, title = "Physics")
        withTimeout(DEFAULT_TIMEOUT_MS) { repo.createDeck(aliceUid, deck) }

        val card = Flashcard(
            deckId = deckId,
            front = "What is F=ma?",
            back = "Newton's Second Law",
            repetitions = 0L,
            intervalDays = 1L,
            easeFactor = 2.5,
            nextReviewDate = System.currentTimeMillis()
        )

        val cardResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.addCard(aliceUid, deckId, card)
        }
        assertTrue("Card creation failed: ${cardResult.exceptionOrNull()}", cardResult.isSuccess)

        // Read back via observeCards Flow
        val cards = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCards(aliceUid, deckId).first { list -> list.isNotEmpty() }
        }
        assertTrue(cards.any { it.front == "What is F=ma?" })
    }

    @Test
    fun recordCardReview_sm2Quality_updatesScheduleAndMastery(): Unit = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repo = DeckRepository(firestore)
        val deckId = "deck_${UUID.randomUUID()}"
        repo.createDeck(aliceUid, Deck(id = deckId, userId = aliceUid, title = "Math"))

        val card = Flashcard(
            id = "card_${UUID.randomUUID()}",
            deckId = deckId,
            front = "2 + 2",
            back = "4",
            repetitions = 0L,
            intervalDays = 1L,
            easeFactor = 2.5
        )
        repo.addCard(aliceUid, deckId, card)

        // Review card as GOOD
        val reviewResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.recordCardReview(aliceUid, deckId, card, Sm2Algorithm.ReviewRating.GOOD)
        }
        assertTrue(reviewResult.isSuccess)
        val updatedCard = reviewResult.getOrThrow()
        assertEquals(1L, updatedCard.repetitions)
        assertEquals(1L, updatedCard.intervalDays)
        assertTrue(updatedCard.masteryLevel >= 1L)
    }

    @Test
    fun observeDecks_unauthenticatedUser_failsWithPermissionDenied(): Unit = runBlocking {
        auth.signOut()
        val repo = DeckRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeDecks("unauthenticated_uid").first()
            }
            fail("Expected FirebaseFirestoreException PERMISSION_DENIED")
        } catch (e: Exception) {
            val firestoreEx = (e as? FirebaseFirestoreException)
                ?: (e.cause as? FirebaseFirestoreException)
                ?: (e.cause?.cause as? FirebaseFirestoreException)
            assertTrue("Expected FirebaseFirestoreException in cause chain: $e", firestoreEx != null)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@example.com"
        const val DEFAULT_TIMEOUT_MS = 6000L
        const val FLOW_TIMEOUT_MS = 5000L
    }
}
