package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.Deck
import com.example.model.Flashcard
import com.example.ui.components.FlipCard
import com.example.ui.components.StudyProgressBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyFlashcardsScreen(
    deck: Deck,
    cards: List<Flashcard>,
    onBack: () -> Unit,
    onToggleStar: (Flashcard) -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    var studyCards by remember(cards) { mutableStateOf(cards) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var starredOnly by remember { mutableStateOf(false) }
    val knownCardIds = remember { mutableStateListOf<String>() }
    val learningCardIds = remember { mutableStateListOf<String>() }
    var showFinishedDialog by remember { mutableStateOf(false) }

    val activeCards = remember(studyCards, starredOnly) {
        if (starredOnly) studyCards.filter { it.isStarred } else studyCards
    }

    val currentCard = activeCards.getOrNull(currentIndex)

    fun nextCard(known: Boolean) {
        if (currentCard != null) {
            if (known) {
                knownCardIds.add(currentCard.id)
            } else {
                learningCardIds.add(currentCard.id)
            }
        }

        if (currentIndex < activeCards.size - 1) {
            currentIndex++
            isFlipped = false
        } else {
            showFinishedDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Flashcards: ${deck.title}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("study_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            studyCards = studyCards.shuffled()
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier.testTag("shuffle_cards_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Shuffle, contentDescription = "Shuffle")
                    }
                    IconButton(
                        onClick = {
                            starredOnly = !starredOnly
                            currentIndex = 0
                            isFlipped = false
                        },
                        modifier = Modifier.testTag("filter_starred_button")
                    ) {
                        Icon(
                            imageVector = if (starredOnly) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = "Starred Filter",
                            tint = if (starredOnly) Color(0xFFEAB308) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Bar
            StudyProgressBar(
                currentIndex = if (activeCards.isEmpty()) 0 else currentIndex + 1,
                totalCards = activeCards.size
            )

            // Center Flashcard
            if (currentCard != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    FlipCard(
                        card = currentCard,
                        isFlipped = isFlipped,
                        onFlip = { isFlipped = !isFlipped },
                        onToggleStar = { onToggleStar(currentCard) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (starredOnly) "No starred cards in this deck" else "No cards available",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Bottom Navigation and Response Buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Swipe Action Buttons (Still Learning vs Know)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { nextCard(known = false) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("still_learning_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(text = " Still Learning", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { nextCard(known = true) },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .testTag("know_card_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(text = " Know It", fontWeight = FontWeight.Bold)
                    }
                }

                // Prev / Next arrow bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentIndex > 0) {
                                currentIndex--
                                isFlipped = false
                            }
                        },
                        enabled = currentIndex > 0,
                        modifier = Modifier.testTag("prev_card_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Card")
                    }

                    Text(
                        text = "Card ${if (activeCards.isEmpty()) 0 else currentIndex + 1} of ${activeCards.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    IconButton(
                        onClick = {
                            if (currentIndex < activeCards.size - 1) {
                                currentIndex++
                                isFlipped = false
                            }
                        },
                        enabled = currentIndex < activeCards.size - 1,
                        modifier = Modifier.testTag("next_card_button")
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Card")
                    }
                }
            }
        }
    }

    if (showFinishedDialog) {
        AlertDialog(
            onDismissRequest = { showFinishedDialog = false },
            title = {
                Text(
                    text = "🎉 Session Complete!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You've studied all ${activeCards.size} cards in this set.",
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Surface(
                            color = Color(0xFFDCFCE7),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "✅ ${knownCardIds.size} Known",
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        Surface(
                            color = Color(0xFFFEE2E2),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "🔄 ${learningCardIds.size} Learning",
                                color = Color(0xFFB91C1C),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentIndex = 0
                        isFlipped = false
                        knownCardIds.clear()
                        learningCardIds.clear()
                        showFinishedDialog = false
                    },
                    modifier = Modifier.testTag("restart_study_button")
                ) {
                    Text("Study Again")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onBack) {
                    Text("Done")
                }
            }
        )
    }
}
