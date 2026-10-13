package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.ui.components.StudyProgressBar

data class QuizQuestion(
    val card: Flashcard,
    val options: List<String>,
    val correctAnswerIndex: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyQuizScreen(
    deck: Deck,
    cards: List<Flashcard>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val questions = remember(cards) {
        cards.shuffled().map { card ->
            // Pick up to 3 distractors from other cards
            val otherBacks = cards.filter { it.id != card.id }.map { it.back }.shuffled().take(3)
            val allOptions = (otherBacks + card.back).shuffled()
            QuizQuestion(
                card = card,
                options = allOptions,
                correctAnswerIndex = allOptions.indexOf(card.back)
            )
        }
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var showResultsDialog by remember { mutableStateOf(false) }

    val currentQuestion = questions.getOrNull(currentIndex)

    fun handleOptionSelect(index: Int) {
        if (selectedOptionIndex != null || currentQuestion == null) return
        selectedOptionIndex = index
        if (index == currentQuestion.correctAnswerIndex) {
            score++
        }
    }

    fun nextQuestion() {
        if (currentIndex < questions.size - 1) {
            currentIndex++
            selectedOptionIndex = null
        } else {
            showResultsDialog = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = " Practice Quiz: ${deck.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("quiz_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(end = 16.dp)
                    ) {
                        Text(
                            text = "Score: $score",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Progress Bar
            StudyProgressBar(
                currentIndex = currentIndex + 1,
                totalCards = questions.size
            )

            // Question Card
            if (currentQuestion != null) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "QUESTION ${currentIndex + 1}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentQuestion.card.front,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Options List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    currentQuestion.options.forEachIndexed { index, option ->
                        val isSelected = selectedOptionIndex == index
                        val isCorrect = index == currentQuestion.correctAnswerIndex
                        val isAnswered = selectedOptionIndex != null

                        val (bgColor, borderColor, textColor) = when {
                            !isAnswered -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.colorScheme.onSurface)
                            isCorrect -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), Color(0xFF15803D))
                            isSelected -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), Color(0xFFB91C1C))
                            else -> Triple(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = bgColor,
                            border = BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAnswered) { handleOptionSelect(index) }
                                .testTag("quiz_option_$index")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = textColor,
                                    fontWeight = if (isSelected || isCorrect) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isAnswered) {
                                    if (isCorrect) {
                                        Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Correct", tint = Color(0xFF16A34A))
                                    } else if (isSelected) {
                                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Incorrect", tint = Color(0xFFDC2626))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Next Question Action Button
            if (selectedOptionIndex != null) {
                Button(
                    onClick = { nextQuestion() },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("quiz_next_button")
                ) {
                    Text(
                        text = if (currentIndex < questions.size - 1) "Next Question" else "See Results",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Box(modifier = Modifier.height(56.dp))
            }
        }
    }

    if (showResultsDialog) {
        val percentage = if (questions.isNotEmpty()) (score * 100) / questions.size else 0
        AlertDialog(
            onDismissRequest = { showResultsDialog = false },
            title = {
                Text(
                    text = if (percentage >= 80) "🏆 Excellent Job!" else "📝 Quiz Finished!",
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
                        text = "Score: $score / ${questions.size} ($percentage%)",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (percentage >= 70) Color(0xFF10B981) else Color(0xFFF59E0B)
                    )
                    Text(
                        text = if (percentage >= 80) "You've demonstrated solid recall of this set." else "Keep studying with SM-2 Spaced Repetition to master these cards!",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        currentIndex = 0
                        selectedOptionIndex = null
                        score = 0
                        showResultsDialog = false
                    },
                    modifier = Modifier.testTag("retake_quiz_button")
                ) {
                    Text("Retake Quiz")
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
