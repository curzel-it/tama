package it.curzel.tama.pixeleditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
    content: @Composable () -> Unit,
) {
    when (orientation) {
        StackOrientation.Horizontal -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(spacing),
                verticalAlignment = Alignment.CenterVertically,
                content = { content() }
            )
        }
        StackOrientation.Vertical -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(spacing),
                content = { content() }
            )
        }
    }
}
