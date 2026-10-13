package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.algorithm.Sm2Algorithm
import com.example.model.Deck
import com.example.model.Flashcard
import com.example.ui.components.AddCardDialog
import com.example.ui.components.MasteryBadge

enum class StudyMode {
    FLASHCARDS,
    LEARN_SM2,
    QUIZ,
    MATCH
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeckDetailScreen(
    deck: Deck,
    cards: List<Flashcard>,
    onBack: () -> Unit,
    onStartStudy: (StudyMode) -> Unit,
    onAddCard: (front: String, back: String, hint: String, notes: String) -> Unit,
    onDeleteCard: (String) -> Unit,
    onToggleStar: (Flashcard) -> Unit,
    onEditDeck: () -> Unit,
    onPublishDeck: () -> Unit,
    onDeleteDeck: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    var showAddDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    val dueCardsCount = remember(cards) {
        cards.count { Sm2Algorithm.isDue(it) }
    }

    val masteredCount = remember(cards) {
        cards.count { it.masteryLevel >= 3L }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = deck.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("deck_detail_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEditDeck,
                        modifier = Modifier.testTag("edit_deck_top_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit Deck")
                    }

                    IconButton(
                        onClick = { shareDeckContent(context, deck, cards) },
                        modifier = Modifier.testTag("share_deck_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share Deck")
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Deck") },
                                onClick = {
                                    showMenu = false
                                    onEditDeck()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Publish to Community") },
                                onClick = {
                                    showMenu = false
                                    onPublishDeck()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Deck", color = Color(0xFFDC2626)) },
                                onClick = {
                                    showMenu = false
                                    onDeleteDeck()
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("add_card_fab")
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Flashcard")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Deck stats header
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = deck.category.ifBlank { "General" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            if (dueCardsCount > 0) {
                                Surface(
                                    color = Color(0xFFFEF2F2),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Alarm,
                                            contentDescription = null,
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = " $dueCardsCount due today",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }
                                }
                            }
                        }

                        if (deck.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = deck.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stats Summary Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            StatColumn(title = "Total", value = "${cards.size}")
                            StatColumn(title = "Due", value = "$dueCardsCount", highlightColor = if (dueCardsCount > 0) Color(0xFFDC2626) else null)
                            StatColumn(title = "Mastered", value = "$masteredCount", highlightColor = Color(0xFF10B981))
                        }
                    }
                }
            }

            // Study Modes Section
            item {
                Text(
                    text = "Study Modes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudyModeCard(
                        title = "Flashcards",
                        subtitle = "Flip & review",
                        icon = Icons.Filled.Style,
                        color = Color(0xFF4F46E5),
                        enabled = cards.isNotEmpty(),
                        modifier = Modifier.weight(1f),
                        testTag = "study_flashcards_mode",
                        onClick = { onStartStudy(StudyMode.FLASHCARDS) }
                    )
                    StudyModeCard(
                        title = "Learn (SM-2)",
                        subtitle = if (dueCardsCount > 0) "$dueCardsCount due now" else "Spaced repetition",
                        icon = Icons.Filled.Psychology,
                        color = Color(0xFF0284C7),
                        enabled = cards.isNotEmpty(),
                        badgeText = if (dueCardsCount > 0) "$dueCardsCount" else null,
                        modifier = Modifier.weight(1f),
                        testTag = "study_learn_mode",
                        onClick = { onStartStudy(StudyMode.LEARN_SM2) }
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StudyModeCard(
                        title = "Practice Quiz",
                        subtitle = "Test recall",
                        icon = Icons.Filled.AutoAwesome,
                        color = Color(0xFF0D9488),
                        enabled = cards.size >= 2,
                        modifier = Modifier.weight(1f),
                        testTag = "study_quiz_mode",
                        onClick = { onStartStudy(StudyMode.QUIZ) }
                    )
                    StudyModeCard(
                        title = "Match Game",
                        subtitle = "Speed pairs",
                        icon = Icons.Filled.Extension,
                        color = Color(0xFFD97706),
                        enabled = cards.size >= 3,
                        modifier = Modifier.weight(1f),
                        testTag = "study_match_mode",
                        onClick = { onStartStudy(StudyMode.MATCH) }
                    )
                }
            }

            // Cards in deck header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cards (${cards.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Button(
                        onClick = { showAddDialog = true },
                        modifier = Modifier.testTag("add_card_inline_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(text = " Add Card")
                    }
                }
            }

            // Cards List
            if (cards.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "This deck is currently empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Add flashcards to start studying!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(cards, key = { it.id }) { card ->
                    CardItem(
                        card = card,
                        onToggleStar = { onToggleStar(card) },
                        onDelete = { onDeleteCard(card.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddCardDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { front, back, hint, notes ->
                onAddCard(front, back, hint, notes)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun StatColumn(title: String, value: String, highlightColor: Color? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StudyModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    enabled: Boolean,
    badgeText: String? = null,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .testTag(testTag)
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (enabled) 2.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = color.copy(alpha = if (enabled) 0.15f else 0.05f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (enabled) color else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (badgeText != null && enabled) {
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = CircleShape
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun CardItem(
    card: Flashcard,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val isDue = Sm2Algorithm.isDue(card)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MasteryBadge(masteryLevel = card.masteryLevel)
                    if (isDue) {
                        Surface(
                            color = Color(0xFFFEF2F2),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Due for review",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFDC2626),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = card.front,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = card.back,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (card.hint.isNotBlank()) {
                    Text(
                        text = "Hint: ${card.hint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleStar) {
                    Icon(
                        imageVector = if (card.isStarred) Icons.Filled.Star else Icons.Outlined.StarOutline,
                        contentDescription = "Star",
                        tint = if (card.isStarred) Color(0xFFEAB308) else MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

private fun shareDeckContent(context: Context, deck: Deck, cards: List<Flashcard>) {
    val textBuilder = StringBuilder()
    textBuilder.append("📚 Flashcard Deck: ${deck.title}\n")
    if (deck.description.isNotBlank()) textBuilder.append("${deck.description}\n")
    textBuilder.append("----------------------------\n")
    cards.forEachIndexed { i, card ->
        textBuilder.append("${i + 1}. ${card.front} -> ${card.back}\n")
    }
    textBuilder.append("\nCreated with StudyPulse!")

    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, textBuilder.toString())
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share Deck: ${deck.title}")
    context.startActivity(shareIntent)
}
