package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.unit.sp
import com.example.model.Deck
import com.example.model.Flashcard
import kotlinx.coroutines.delay

data class MatchTile(
    val id: String,
    val cardId: String,
    val text: String,
    val isTerm: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMatchScreen(
    deck: Deck,
    cards: List<Flashcard>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    // Pick up to 6 cards for 12 tiles
    val activeCards = remember(cards) { cards.shuffled().take(6) }

    val tiles = remember(activeCards) {
        val list = mutableListOf<MatchTile>()
        activeCards.forEach { card ->
            list.add(MatchTile(id = "${card.id}_term", cardId = card.id, text = card.front, isTerm = true))
            list.add(MatchTile(id = "${card.id}_def", cardId = card.id, text = card.back, isTerm = false))
        }
        list.shuffled()
    }

    val matchedTileIds = remember { mutableStateListOf<String>() }
    var selectedTile by remember { mutableStateOf<MatchTile?>(null) }
    var mismatchPair by remember { mutableStateOf<Pair<String, String>?>(null) }
    var elapsedTimeMs by remember { mutableLongStateOf(0L) }
    var isTimerRunning by remember { mutableStateOf(true) }
    var showVictoryDialog by remember { mutableStateOf(false) }

    // Live timer
    LaunchedEffect(isTimerRunning) {
        val start = System.currentTimeMillis() - elapsedTimeMs
        while (isTimerRunning) {
            elapsedTimeMs = System.currentTimeMillis() - start
            delay(100L)
        }
    }

    fun handleTileClick(tile: MatchTile) {
        if (matchedTileIds.contains(tile.id) || mismatchPair != null) return

        val first = selectedTile
        if (first == null) {
            selectedTile = tile
        } else if (first.id == tile.id) {
            // Deselect
            selectedTile = null
        } else {
            // Check match: same cardId and different tile type
            if (first.cardId == tile.cardId && first.isTerm != tile.isTerm) {
                // Match!
                matchedTileIds.add(first.id)
                matchedTileIds.add(tile.id)
                selectedTile = null

                if (matchedTileIds.size == tiles.size) {
                    isTimerRunning = false
                    showVictoryDialog = true
                }
            } else {
                // Mismatch
                mismatchPair = Pair(first.id, tile.id)
                selectedTile = null
            }
        }
    }

    LaunchedEffect(mismatchPair) {
        if (mismatchPair != null) {
            delay(600L)
            mismatchPair = null
        }
    }

    val secondsFormatted = String.format("%.1fs", elapsedTimeMs / 1000f)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Extension,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = " Match: ${deck.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("match_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = Color(0xFFFEF3C7),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Timer,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = " $secondsFormatted",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB45309)
                            )
                        }
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
                .padding(16.dp)
        ) {
            Text(
                text = "Tap a term, then tap its corresponding definition to match them!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tiles, key = { it.id }) { tile ->
                    val isMatched = matchedTileIds.contains(tile.id)
                    val isSelected = selectedTile?.id == tile.id
                    val isMismatch = mismatchPair?.first == tile.id || mismatchPair?.second == tile.id

                    AnimatedVisibility(
                        visible = !isMatched,
                        exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
                    ) {
                        val (bgColor, borderColor) = when {
                            isMismatch -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                            isSelected -> Pair(Color(0xFFE0E7FF), Color(0xFF4F46E5))
                            else -> Pair(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = BorderStroke(if (isSelected || isMismatch) 2.dp else 1.dp, borderColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .clickable { handleTileClick(tile) }
                                .testTag("match_tile_${tile.id}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tile.text,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    color = if (isMismatch) Color(0xFFB91C1C) else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 4
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVictoryDialog) {
        AlertDialog(
            onDismissRequest = { showVictoryDialog = false },
            title = {
                Text(
                    text = "⚡ Incredible Speed!",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = secondsFormatted,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFD97706)
                    )
                    Text(
                        text = "You cleared all ${activeCards.size} pairs!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        matchedTileIds.clear()
                        selectedTile = null
                        mismatchPair = null
                        elapsedTimeMs = 0L
                        isTimerRunning = true
                        showVictoryDialog = false
                    },
                    modifier = Modifier.testTag("play_match_again_button")
                ) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onBack) {
                    Text("Back to Deck")
                }
            }
        )
    }
}
