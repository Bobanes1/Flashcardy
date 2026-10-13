package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.algorithm.Sm2Algorithm
import com.example.model.Flashcard
import com.example.ui.theme.MasteryLearning
import com.example.ui.theme.MasteryMastered
import com.example.ui.theme.MasteryNew
import com.example.ui.theme.MasteryReviewing
import com.example.ui.theme.RatingAgain
import com.example.ui.theme.RatingEasy
import com.example.ui.theme.RatingGood
import com.example.ui.theme.RatingHard

@Composable
fun MasteryBadge(
    masteryLevel: Long,
    modifier: Modifier = Modifier
) {
    val (color, label) = when (masteryLevel) {
        3L -> Pair(MasteryMastered, "Mastered")
        2L -> Pair(MasteryReviewing, "Reviewing")
        1L -> Pair(MasteryLearning, "Learning")
        else -> Pair(MasteryNew, "New")
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun Sm2RatingRow(
    card: Flashcard,
    onRate: (Sm2Algorithm.ReviewRating) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val ratings = listOf(
            Triple(Sm2Algorithm.ReviewRating.AGAIN, RatingAgain, "again_button"),
            Triple(Sm2Algorithm.ReviewRating.HARD, RatingHard, "hard_button"),
            Triple(Sm2Algorithm.ReviewRating.GOOD, RatingGood, "good_button"),
            Triple(Sm2Algorithm.ReviewRating.EASY, RatingEasy, "easy_button")
        )

        ratings.forEach { (rating, color, tag) ->
            val previewInterval = Sm2Algorithm.getPreviewIntervalLabel(card, rating)
            Button(
                onClick = { onRate(rating) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = color,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag(tag)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = rating.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = previewInterval,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}

@Composable
fun StudyProgressBar(
    currentIndex: Int,
    totalCards: Int,
    modifier: Modifier = Modifier
) {
    val progress = if (totalCards > 0) currentIndex.toFloat() / totalCards else 0f

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$currentIndex / $totalCards cards",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
