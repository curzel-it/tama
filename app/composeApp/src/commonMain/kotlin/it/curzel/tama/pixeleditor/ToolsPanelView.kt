package it.curzel.tama.pixeleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ToolsPanelView(
    isLandscape: Boolean,
    onClearCanvas: () -> Unit,
    onFillCanvas: () -> Unit,
    onOpenCanvasSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseModifier = modifier.background(MaterialTheme.colorScheme.surface)
    val stackModifier = if (isLandscape) {
        baseModifier
            .width(100.dp)
            .fillMaxHeight()
            .padding(8.dp)
    } else {
        baseModifier
            .fillMaxWidth()
            .height(60.dp)
            .padding(horizontal = 8.dp)
    }

    Stack(
        orientation = if (isLandscape) StackOrientation.Vertical else StackOrientation.Horizontal,
        modifier = stackModifier,
        spacing = 8.dp
    ) {
        item {
            ToolButton("Clear", onClick = onClearCanvas)
        }
        item {
            ToolButton("Fill", onClick = onFillCanvas)
        }
        item {
            ToolButton("Settings", onClick = onOpenCanvasSettings)
        }
    }
}

@Composable
private fun ToolButton(
    text: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 70.dp, height = 50.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isActive) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            maxLines = 2
        )
    }
}
