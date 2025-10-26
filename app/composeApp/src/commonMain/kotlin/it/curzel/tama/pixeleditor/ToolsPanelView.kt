package it.curzel.tama.pixeleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val baseModifier = modifier.background(MaterialTheme.colorScheme.surface)

    if (isLandscape) {
        LazyColumn(
            modifier = baseModifier
                .fillMaxHeight()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canUndo) {
                item {
                    ToolButton(
                        icon = Res.drawable.icon_undo,
                        contentDescription = "Undo",
                        onClick = onUndo
                    )
                }
            }

            if (canRedo) {
                item {
                    ToolButton(
                        icon = Res.drawable.icon_redo,
                        contentDescription = "Redo",
                        onClick = onRedo
                    )
                }
            }

            if (canUndo || canRedo) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_pencil,
                    contentDescription = "Pencil",
                    isActive = currentTool == ToolType.PENCIL,
                    onClick = { onSelectTool(ToolType.PENCIL) }
                )
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_eraser,
                    contentDescription = "Eraser",
                    isActive = currentTool == ToolType.ERASER,
                    onClick = { onSelectTool(ToolType.ERASER) }
                )
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_move,
                    contentDescription = "Move",
                    isActive = currentTool == ToolType.MOVE,
                    onClick = { onSelectTool(ToolType.MOVE) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            item {
                ToolButton(icon = Res.drawable.icon_clear, contentDescription = "Clear", onClick = onClearCanvas)
            }

            item {
                ToolButton(icon = Res.drawable.icon_fill, contentDescription = "Fill", onClick = onFillCanvas)
            }

            item {
                ToolButton(icon = Res.drawable.icon_settings, contentDescription = "Settings", onClick = onOpenCanvasSettings)
            }
        }
    } else {
        LazyRow(
            modifier = baseModifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canUndo) {
                item {
                    ToolButton(
                        icon = Res.drawable.icon_undo,
                        contentDescription = "Undo",
                        onClick = onUndo
                    )
                }
            }

            if (canRedo) {
                item {
                    ToolButton(
                        icon = Res.drawable.icon_redo,
                        contentDescription = "Redo",
                        onClick = onRedo
                    )
                }
            }

            if (canUndo || canRedo) {
                item {
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_pencil,
                    contentDescription = "Pencil",
                    isActive = currentTool == ToolType.PENCIL,
                    onClick = { onSelectTool(ToolType.PENCIL) }
                )
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_eraser,
                    contentDescription = "Eraser",
                    isActive = currentTool == ToolType.ERASER,
                    onClick = { onSelectTool(ToolType.ERASER) }
                )
            }

            item {
                ToolButton(
                    icon = Res.drawable.icon_move,
                    contentDescription = "Move",
                    isActive = currentTool == ToolType.MOVE,
                    onClick = { onSelectTool(ToolType.MOVE) }
                )
            }

            item {
                Spacer(modifier = Modifier.width(8.dp))
            }

            item {
                ToolButton(icon = Res.drawable.icon_clear, contentDescription = "Clear", onClick = onClearCanvas)
            }

            item {
                ToolButton(icon = Res.drawable.icon_fill, contentDescription = "Fill", onClick = onFillCanvas)
            }

            item {
                ToolButton(icon = Res.drawable.icon_settings, contentDescription = "Settings", onClick = onOpenCanvasSettings)
            }
        }
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
