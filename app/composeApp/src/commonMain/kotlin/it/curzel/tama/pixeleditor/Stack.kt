package it.curzel.tama.pixeleditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StackOrientation {
    Horizontal,
    Vertical
}

@Composable
fun Stack(
    orientation: StackOrientation,
    modifier: Modifier = Modifier,
    spacing: Dp = 0.dp,
    content: LazyListScope.() -> Unit
) {
    when (orientation) {
        StackOrientation.Horizontal -> {
            LazyRow(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
        StackOrientation.Vertical -> {
            LazyColumn(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = content
            )
        }
    }
}
