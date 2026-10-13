package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.algorithm.Sm2Algorithm
import com.example.model.Deck
import com.example.model.Flashcard
import com.example.model.PublicDeck
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(exception: Exception, operationType: OperationType, path: String?): String {
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class DeckRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = FirebaseAuth.getInstance()

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    // -------------------------------------------------------------
    // Deck Operations
    // -------------------------------------------------------------

    fun observeDecks(userId: String): Flow<List<Deck>> {
        val path = "users/$userId/decks"
        return db.collection("users").document(userId).collection("decks")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Deck::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun getDeck(userId: String, deckId: String): Deck? {
        val path = "users/$userId/decks/$deckId"
        return try {
            db.collection("users").document(userId).collection("decks").document(deckId)
                .get()
                .await()
                .toObject(Deck::class.java)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            null
        }
    }

    suspend fun createDeck(userId: String, deck: Deck): Result<String> {
        val path = "users/$userId/decks"
        val deckId = if (deck.id.isNotBlank()) deck.id else "deck_${UUID.randomUUID()}"
        val docRef = db.collection("users").document(userId).collection("decks").document(deckId)
        return try {
            val payload = mapOf(
                "userId" to userId,
                "title" to deck.title,
                "description" to deck.description,
                "category" to deck.category,
                "colorHex" to deck.colorHex,
                "cardCount" to deck.cardCount,
                "isPublic" to deck.isPublic,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            Result.success(deckId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDeck(userId: String, deck: Deck): Result<Unit> {
        val path = "users/$userId/decks/${deck.id}"
        val docRef = db.collection("users").document(userId).collection("decks").document(deck.id)
        return try {
            val payload = mapOf(
                "title" to deck.title,
                "description" to deck.description,
                "category" to deck.category,
                "colorHex" to deck.colorHex,
                "cardCount" to deck.cardCount,
                "isPublic" to deck.isPublic,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteDeck(userId: String, deckId: String): Result<Unit> {
        val path = "users/$userId/decks/$deckId"
        val docRef = db.collection("users").document(userId).collection("decks").document(deckId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Flashcard Operations (with SM-2 Spaced Repetition)
    // -------------------------------------------------------------

    fun observeCards(userId: String, deckId: String): Flow<List<Flashcard>> {
        val path = "users/$userId/decks/$deckId/cards"
        return db.collection("users").document(userId).collection("decks").document(deckId)
            .collection("cards")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(Flashcard::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun addCard(userId: String, deckId: String, card: Flashcard): Result<String> {
        val path = "users/$userId/decks/$deckId/cards"
        val cardId = if (card.id.isNotBlank()) card.id else "card_${UUID.randomUUID()}"
        val cardRef = db.collection("users").document(userId).collection("decks").document(deckId)
            .collection("cards").document(cardId)
        val deckRef = db.collection("users").document(userId).collection("decks").document(deckId)

        return try {
            val payload = mapOf(
                "deckId" to deckId,
                "front" to card.front,
                "back" to card.back,
                "hint" to card.hint,
                "notes" to card.notes,
                "isStarred" to card.isStarred,
                "repetitions" to card.repetitions,
                "intervalDays" to card.intervalDays,
                "easeFactor" to card.easeFactor,
                "nextReviewDate" to card.nextReviewDate,
                "lastReviewedDate" to card.lastReviewedDate,
                "reviewCount" to card.reviewCount,
                "masteryLevel" to card.masteryLevel,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            cardRef.set(payload).await()
            deckRef.update("cardCount", FieldValue.increment(1), "updatedAt", FieldValue.serverTimestamp()).await()
            Result.success(cardId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCard(userId: String, deckId: String, card: Flashcard): Result<Unit> {
        val path = "users/$userId/decks/$deckId/cards/${card.id}"
        val cardRef = db.collection("users").document(userId).collection("decks").document(deckId)
            .collection("cards").document(card.id)

        return try {
            val payload = mapOf(
                "front" to card.front,
                "back" to card.back,
                "hint" to card.hint,
                "notes" to card.notes,
                "isStarred" to card.isStarred,
                "repetitions" to card.repetitions,
                "intervalDays" to card.intervalDays,
                "easeFactor" to card.easeFactor,
                "nextReviewDate" to card.nextReviewDate,
                "lastReviewedDate" to card.lastReviewedDate,
                "reviewCount" to card.reviewCount,
                "masteryLevel" to card.masteryLevel,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            cardRef.update(payload).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteCard(userId: String, deckId: String, cardId: String): Result<Unit> {
        val path = "users/$userId/decks/$deckId/cards/$cardId"
        val cardRef = db.collection("users").document(userId).collection("decks").document(deckId)
            .collection("cards").document(cardId)
        val deckRef = db.collection("users").document(userId).collection("decks").document(deckId)

        return try {
            cardRef.delete().await()
            deckRef.update("cardCount", FieldValue.increment(-1), "updatedAt", FieldValue.serverTimestamp()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    /**
     * Applies the SM-2 Spaced Repetition Algorithm to a card review and updates Firestore.
     */
    suspend fun recordCardReview(
        userId: String,
        deckId: String,
        card: Flashcard,
        rating: Sm2Algorithm.ReviewRating
    ): Result<Flashcard> {
        val sm2Result = Sm2Algorithm.calculateNextReview(card, rating)
        val updatedCard = card.copy(
            repetitions = sm2Result.repetitions,
            intervalDays = sm2Result.intervalDays,
            easeFactor = sm2Result.easeFactor,
            nextReviewDate = sm2Result.nextReviewDate,
            lastReviewedDate = sm2Result.lastReviewedDate,
            reviewCount = sm2Result.reviewCount,
            masteryLevel = sm2Result.masteryLevel
        )
        return updateCard(userId, deckId, updatedCard).map { updatedCard }
    }

    suspend fun toggleCardStarred(userId: String, deckId: String, card: Flashcard): Result<Unit> {
        val updatedCard = card.copy(isStarred = !card.isStarred)
        return updateCard(userId, deckId, updatedCard)
    }

    // -------------------------------------------------------------
    // Community / Public Deck Sharing
    // -------------------------------------------------------------

    fun observePublicDecks(): Flow<List<PublicDeck>> {
        val path = "public_decks"
        return db.collection("public_decks")
            .snapshots()
            .map { snapshot -> snapshot.toObjects(PublicDeck::class.java) }
            .catch { e ->
                if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                throw e
            }
    }

    suspend fun publishDeck(
        userId: String,
        authorName: String,
        deck: Deck,
        cards: List<Flashcard>
    ): Result<String> {
        val publicDeckId = "pub_${deck.id.ifBlank { UUID.randomUUID().toString() }}"
        val docRef = db.collection("public_decks").document(publicDeckId)

        // Serialize cards to JSON string for easy download and import
        val cardsJsonArray = JSONArray()
        cards.forEach { card ->
            val obj = JSONObject().apply {
                put("front", card.front)
                put("back", card.back)
                put("hint", card.hint)
                put("notes", card.notes)
            }
            cardsJsonArray.put(obj)
        }

        return try {
            val payload = mapOf(
                "authorId" to userId,
                "authorName" to authorName,
                "title" to deck.title,
                "description" to deck.description,
                "category" to deck.category,
                "cardCount" to cards.size.toLong(),
                "cardsJson" to cardsJsonArray.toString(),
                "downloads" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            docRef.set(payload).await()
            // Mark original deck as public
            updateDeck(userId, deck.copy(isPublic = true))
            Result.success(publicDeckId)
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "public_decks/$publicDeckId")
            Result.failure(e)
        }
    }

    suspend fun importPublicDeck(userId: String, publicDeck: PublicDeck): Result<String> {
        val newDeckId = "deck_${UUID.randomUUID()}"
        val newDeck = Deck(
            id = newDeckId,
            userId = userId,
            title = publicDeck.title,
            description = "Imported from ${publicDeck.authorName}: ${publicDeck.description}",
            category = publicDeck.category,
            cardCount = 0L,
            isPublic = false
        )

        return try {
            createDeck(userId, newDeck).getOrThrow()

            // Parse cards from JSON and insert with fresh SM-2 state
            val jsonArray = JSONArray(publicDeck.cardsJson)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val card = Flashcard(
                    deckId = newDeckId,
                    front = obj.optString("front", ""),
                    back = obj.optString("back", ""),
                    hint = obj.optString("hint", ""),
                    notes = obj.optString("notes", ""),
                    repetitions = 0L,
                    intervalDays = 1L,
                    easeFactor = 2.5,
                    nextReviewDate = System.currentTimeMillis()
                )
                addCard(userId, newDeckId, card)
            }

            // Increment download count on public deck
            db.collection("public_decks").document(publicDeck.id)
                .update("downloads", FieldValue.increment(1))
                .await()

            Result.success(newDeckId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -------------------------------------------------------------
    // Built-in Starter Decks
    // -------------------------------------------------------------

    suspend fun seedSampleDecksIfEmpty(userId: String) {
        val existing = db.collection("users").document(userId).collection("decks").get().await()
        if (!existing.isEmpty) return

        // 1. Spanish Essentials
        val spanishDeckId = "deck_spanish_essentials"
        val spanishDeck = Deck(
            id = spanishDeckId,
            userId = userId,
            title = "Spanish Essentials",
            description = "High-frequency conversational phrases & words",
            category = "Languages",
            colorHex = "#4F46E5",
            cardCount = 0L
        )
        createDeck(userId, spanishDeck)
        val spanishCards = listOf(
            Triple("Hola, ¿cómo estás?", "Hello, how are you?", "Common greeting"),
            Triple("Mucho gusto", "Nice to meet you", "Polite introduction"),
            Triple("Por favor / Gracias", "Please / Thank you", "Essential manners"),
            Triple("¿Dónde está el baño?", "Where is the bathroom?", "Travel necessity"),
            Triple("La cuenta, por favor", "The check, please", "Restaurant phrase"),
            Triple("No entiendo", "I don't understand", "Helpful when learning")
        )
        spanishCards.forEach { (front, back, hint) ->
            addCard(userId, spanishDeckId, Flashcard(deckId = spanishDeckId, front = front, back = back, hint = hint))
        }

        // 2. CS Algorithms & Data Structures
        val csDeckId = "deck_cs_algorithms"
        val csDeck = Deck(
            id = csDeckId,
            userId = userId,
            title = "CS Algorithms & Big-O",
            description = "Core computer science time complexities & data structures",
            category = "Technology",
            colorHex = "#0EA5E9",
            cardCount = 0L
        )
        createDeck(userId, csDeck)
        val csCards = listOf(
            Triple("Binary Search Time Complexity", "O(log n)", "Divide and conquer"),
            Triple("Hash Table Lookup (Average)", "O(1) Constant Time", "Direct key hashing"),
            Triple("QuickSort Worst Case", "O(n²)", "When pivot is poorly chosen"),
            Triple("Stack Data Structure", "LIFO (Last In First Out)", "Like a stack of plates"),
            Triple("Queue Data Structure", "FIFO (First In First Out)", "Like a grocery line")
        )
        csCards.forEach { (front, back, hint) ->
            addCard(userId, csDeckId, Flashcard(deckId = csDeckId, front = front, back = back, hint = hint))
        }
    }
}
