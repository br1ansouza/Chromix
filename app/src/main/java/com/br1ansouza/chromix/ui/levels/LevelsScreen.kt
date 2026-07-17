package com.br1ansouza.chromix.ui.levels

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private const val LOOKAHEAD_LEVELS = 60

/**
 * Grade de níveis: concluídos com check, atual destacado, futuros bloqueados
 * (progressão linear). A lista cresce sob demanda conforme o recorde avança —
 * as fases são procedurais, então só o número importa.
 */
@Composable
fun LevelsScreen(
    currentLevel: Int,
    bestLevelReached: Int,
    onLevelSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    val levels = remember(bestLevelReached) {
        (1..bestLevelReached + LOOKAHEAD_LEVELS).toList()
    }
    val gridState = rememberLazyGridState(
        initialFirstVisibleItemIndex = (currentLevel - 1).coerceAtLeast(0)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar ao jogo",
                    tint = Color.White,
                )
            }
            Text(
                text = "Níveis",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 68.dp),
            state = gridState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(levels, key = { it }) { level ->
                LevelCell(
                    level = level,
                    isCompleted = level < bestLevelReached,
                    isCurrent = level == currentLevel,
                    isUnlocked = level <= bestLevelReached,
                    onClick = { onLevelSelected(level) },
                )
            }
        }
    }
}

@Composable
private fun LevelCell(
    level: Int,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit,
) {
    val primary = MaterialTheme.colorScheme.primary
    val background = when {
        isCompleted -> primary.copy(alpha = 0.22f)
        isUnlocked -> Color.White.copy(alpha = 0.08f)
        else -> Color.White.copy(alpha = 0.03f)
    }
    val borderColor = if (isCurrent) primary else Color.Transparent
    val contentAlpha = if (isUnlocked) 1f else 0.35f

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(background, RoundedCornerShape(14.dp))
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .then(if (isUnlocked) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$level",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = contentAlpha),
            )
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Concluído",
                    tint = primary,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
