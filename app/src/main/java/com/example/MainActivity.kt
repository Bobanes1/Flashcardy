package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.algorithm.Sm2Algorithm
import com.example.data.DeckRepository
import com.example.model.Deck
import com.example.model.Flashcard
import com.example.model.PublicDeck
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DeckDetailScreen
import com.example.ui.screens.DeckEditorScreen
import com.example.ui.screens.DeckListScreen
import com.example.ui.screens.ExploreDecksScreen
import com.example.ui.screens.StudyFlashcardsScreen
import com.example.ui.screens.StudyLearnScreen
import com.example.ui.screens.StudyMatchScreen
import com.example.ui.screens.StudyMode
import com.example.ui.screens.StudyQuizScreen
import com.example.ui.screens.attemptAutoSignIn
import com.example.ui.screens.signOutUser
import com.example.ui.theme.StudyPulseTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

sealed interface Screen {
    data object DeckList : Screen
    data class DeckDetail(val deckId: String) : Screen
    data class DeckEditor(val deckId: String? = null) : Screen
    data class Study(val deckId: String, val mode: StudyMode) : Screen
    data object Explore : Screen
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudyPulseTheme {
                StudyPulseApp()
            }
        }
    }
}

@Composable
fun StudyPulseApp() {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var currentUser by remember { mutableStateOf<FirebaseUser?>(auth.currentUser) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        if (currentUser == null) {
            attemptAutoSignIn(
                context = context,
                credentialManager = credentialManager,
                onAuthSuccess = { currentUser = auth.currentUser },
                onUnauthenticated = {},
                scope = this
            )
        }
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(
            onAuthSuccess = { currentUser = auth.currentUser },
            modifier = Modifier.fillMaxSize()
        )
    } else {
        MainStudyPulseFlow(
            userId = user.uid,
            userName = user.displayName ?: user.email ?: "Learner",
            onSignOut = {
                signOutUser(
                    context = context,
                    credentialManager = credentialManager,
                    onSignOutComplete = { currentUser = null },
                    scope = coroutineScope
                )
            }
        )
    }
}

@Composable
fun MainStudyPulseFlow(
    userId: String,
    userName: String,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val repository = remember(context) { DeckRepository(context) }

    var currentScreen by remember { mutableStateOf<Screen>(Screen.DeckList) }

    // Real-time user decks stream
    val decks by repository.observeDecks(userId).collectAsStateWithLifecycle(initialValue = emptyList())
    // Real-time public decks stream
    val publicDecks by repository.observePublicDecks().collectAsStateWithLifecycle(initialValue = emptyList())

    // Auto-seed starter sample decks if user has none
    LaunchedEffect(userId) {
        repository.seedSampleDecksIfEmpty(userId)
    }

    when (val screen = currentScreen) {
        is Screen.DeckList -> {
            DeckListScreen(
                decks = decks,
                isLoading = false,
                onSelectDeck = { deck -> currentScreen = Screen.DeckDetail(deck.id) },
                onCreateDeck = { title, desc, cat ->
                    coroutineScope.launch {
                        val newDeck = Deck(
                            userId = userId,
                            title = title,
                            description = desc,
                            category = cat
                        )
                        val res = repository.createDeck(userId, newDeck)
                        if (res.isSuccess) {
                            currentScreen = Screen.DeckDetail(res.getOrThrow())
                        }
                    }
                },
                onExploreClicked = { currentScreen = Screen.Explore },
                onSignOut = onSignOut
            )
        }

        is Screen.DeckDetail -> {
            val selectedDeck = decks.find { it.id == screen.deckId } ?: Deck(id = screen.deckId, title = "Deck")
            val cards by repository.observeCards(userId, screen.deckId).collectAsStateWithLifecycle(initialValue = emptyList())

            DeckDetailScreen(
                deck = selectedDeck,
                cards = cards,
                onBack = { currentScreen = Screen.DeckList },
                onStartStudy = { mode -> currentScreen = Screen.Study(screen.deckId, mode) },
                onAddCard = { front, back, hint, notes ->
                    coroutineScope.launch {
                        val card = Flashcard(
                            deckId = screen.deckId,
                            front = front,
                            back = back,
                            hint = hint,
                            notes = notes,
                            repetitions = 0L,
                            intervalDays = 1L,
                            easeFactor = 2.5,
                            nextReviewDate = System.currentTimeMillis()
                        )
                        repository.addCard(userId, screen.deckId, card)
                    }
                },
                onDeleteCard = { cardId ->
                    coroutineScope.launch {
                        repository.deleteCard(userId, screen.deckId, cardId)
                    }
                },
                onToggleStar = { card ->
                    coroutineScope.launch {
                        repository.toggleCardStarred(userId, screen.deckId, card)
                    }
                },
                onEditDeck = { currentScreen = Screen.DeckEditor(screen.deckId) },
                onPublishDeck = {
                    coroutineScope.launch {
                        repository.publishDeck(userId, userName, selectedDeck, cards)
                    }
                },
                onDeleteDeck = {
                    coroutineScope.launch {
                        repository.deleteDeck(userId, screen.deckId)
                        currentScreen = Screen.DeckList
                    }
                }
            )
        }

        is Screen.DeckEditor -> {
            val deckToEdit = if (screen.deckId != null) decks.find { it.id == screen.deckId } else null
            DeckEditorScreen(
                initialDeck = deckToEdit,
                onSave = { title, desc, cat, colorHex, isPub ->
                    coroutineScope.launch {
                        if (deckToEdit != null) {
                            val updated = deckToEdit.copy(
                                title = title,
                                description = desc,
                                category = cat,
                                colorHex = colorHex,
                                isPublic = isPub
                            )
                            repository.updateDeck(userId, updated)
                            currentScreen = Screen.DeckDetail(deckToEdit.id)
                        } else {
                            val newDeck = Deck(
                                userId = userId,
                                title = title,
                                description = desc,
                                category = cat,
                                colorHex = colorHex,
                                isPublic = isPub
                            )
                            val res = repository.createDeck(userId, newDeck)
                            if (res.isSuccess) {
                                currentScreen = Screen.DeckDetail(res.getOrThrow())
                            } else {
                                currentScreen = Screen.DeckList
                            }
                        }
                    }
                },
                onDelete = if (deckToEdit != null) {
                    {
                        coroutineScope.launch {
                            repository.deleteDeck(userId, deckToEdit.id)
                            currentScreen = Screen.DeckList
                        }
                    }
                } else null,
                onCancel = {
                    currentScreen = if (deckToEdit != null) Screen.DeckDetail(deckToEdit.id) else Screen.DeckList
                }
            )
        }

        is Screen.Study -> {
            val selectedDeck = decks.find { it.id == screen.deckId } ?: Deck(id = screen.deckId, title = "Deck")
            val cards by repository.observeCards(userId, screen.deckId).collectAsStateWithLifecycle(initialValue = emptyList())

            when (screen.mode) {
                StudyMode.FLASHCARDS -> {
                    StudyFlashcardsScreen(
                        deck = selectedDeck,
                        cards = cards,
                        onBack = { currentScreen = Screen.DeckDetail(screen.deckId) },
                        onToggleStar = { card ->
                            coroutineScope.launch {
                                repository.toggleCardStarred(userId, screen.deckId, card)
                            }
                        }
                    )
                }

                StudyMode.LEARN_SM2 -> {
                    StudyLearnScreen(
                        deck = selectedDeck,
                        cards = cards,
                        onBack = { currentScreen = Screen.DeckDetail(screen.deckId) },
                        onRateCard = { card, rating ->
                            coroutineScope.launch {
                                repository.recordCardReview(userId, screen.deckId, card, rating)
                            }
                        }
                    )
                }

                StudyMode.QUIZ -> {
                    StudyQuizScreen(
                        deck = selectedDeck,
                        cards = cards,
                        onBack = { currentScreen = Screen.DeckDetail(screen.deckId) }
                    )
                }

                StudyMode.MATCH -> {
                    StudyMatchScreen(
                        deck = selectedDeck,
                        cards = cards,
                        onBack = { currentScreen = Screen.DeckDetail(screen.deckId) }
                    )
                }
            }
        }

        is Screen.Explore -> {
            ExploreDecksScreen(
                publicDecks = publicDecks,
                isLoading = false,
                onBack = { currentScreen = Screen.DeckList },
                onImportDeck = { publicDeck ->
                    repository.importPublicDeck(userId, publicDeck)
                }
            )
        }
    }
}
