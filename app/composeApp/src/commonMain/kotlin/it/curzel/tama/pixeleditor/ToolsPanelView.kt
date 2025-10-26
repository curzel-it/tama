package it.curzel.tama.pixeleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import tama.composeapp.generated.resources.*

@Composable
fun ToolsPanelView(
    isLandscape: Boolean,
    currentTool: ToolType,
    onSelectTool: (ToolType) -> Unit,
    onClearCanvas: () -> Unit,
    onFillCanvas: () -> Unit,
    onOpenCanvasSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseModifier = modifier.background(MaterialTheme.colorScheme.surface)
    val stackModifier = if (isLandscape) {
        baseModifier
            .fillMaxHeight()
            .padding(8.dp)
    } else {
        baseModifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    }

    Stack(
        orientation = if (isLandscape) StackOrientation.Vertical else StackOrientation.Horizontal,
        modifier = stackModifier,
        spacing = 8.dp
    ) {
        ToolButton(
            icon = Res.drawable.icon_pencil,
            contentDescription = "Pencil",
            isActive = currentTool == ToolType.PENCIL,
            onClick = { onSelectTool(ToolType.PENCIL) }
        )

        ToolButton(
            icon = Res.drawable.icon_eraser,
            contentDescription = "Eraser",
            isActive = currentTool == ToolType.ERASER,
            onClick = { onSelectTool(ToolType.ERASER) }
        )

        ToolButton(
            icon = Res.drawable.icon_move,
            contentDescription = "Move",
            isActive = currentTool == ToolType.MOVE,
            onClick = { onSelectTool(ToolType.MOVE) }
        )

        Spacer(modifier = Modifier.width(8.dp).height(8.dp))

        ToolButton(icon = Res.drawable.icon_clear, contentDescription = "Clear", onClick = onClearCanvas)

        ToolButton(icon = Res.drawable.icon_fill, contentDescription = "Fill", onClick = onFillCanvas)

        ToolButton(icon = Res.drawable.icon_settings, contentDescription = "Settings", onClick = onOpenCanvasSettings)
    }
}

@Composable
private fun ToolButton(
    icon: DrawableResource,
    contentDescription: String,
    onClick: () -> Unit,
    isActive: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(32.dp)
        )
    }
}
