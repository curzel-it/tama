package it.curzel.tama.pixeleditor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.painterResource
import tama.composeapp.generated.resources.Res
import tama.composeapp.generated.resources.icon_clear

@Composable
fun FrameListView(
    frames: List<PixelFrame>,
    currentFrameIndex: Int,
    onFrameSelect: (Int) -> Unit,
    onFrameDelete: (Int) -> Unit,
    onAddFrame: () -> Unit,
    isLandscape: Boolean,
    canvasWidth: Int,
    canvasHeight: Int,
    fps: Float,
    modifier: Modifier = Modifier
) {
    val (thumbWidth, thumbHeight) = FrameListUseCase.calculateThumbnailSize(
        isLandscape = isLandscape,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        maxSize = 80f
    )

    val baseModifier = modifier.background(MaterialTheme.colorScheme.surface)
    val stackModifier = if (isLandscape) {
        baseModifier.fillMaxHeight()
    } else {
        baseModifier.fillMaxWidth()
    }


    Stack(
        orientation = if (isLandscape) StackOrientation.Vertical else StackOrientation.Horizontal,
        modifier = stackModifier,
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            AnimatedPreviewView(
                frames = frames,
                fps = fps,
                canvasWidth = canvasWidth,
                canvasHeight = canvasHeight,
                isLandscape = isLandscape
            )
        }

        if (isLandscape) {
            LazyColumn(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(frames) { index, frame ->
                    FrameThumbnail(
                        frame = frame,
                        isSelected = index == currentFrameIndex,
                        onSelect = { onFrameSelect(index) },
                        onDelete = if (index == currentFrameIndex && frames.size > 1) {
                            { onFrameDelete(index) }
                        } else null,
                        thumbnailWidth = thumbWidth,
                        thumbnailHeight = thumbHeight
                    )
                }

                item {
                    AddFrameButton(
                        onClick = onAddFrame,
                        width = thumbWidth,
                        height = thumbHeight
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(frames) { index, frame ->
                    FrameThumbnail(
                        frame = frame,
                        isSelected = index == currentFrameIndex,
                        onSelect = { onFrameSelect(index) },
                        onDelete = if (index == currentFrameIndex && frames.size > 1) {
                            { onFrameDelete(index) }
                        } else null,
                        thumbnailWidth = thumbWidth,
                        thumbnailHeight = thumbHeight
                    )
                }

                item {
                    AddFrameButton(
                        onClick = onAddFrame,
                        width = thumbWidth,
                        height = thumbHeight
                    )
                }
            }
        }
    }
}

@Composable
internal fun FrameRenderer(
    frame: PixelFrame,
    modifier: Modifier = Modifier
) {
    val isLightMode = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val pixelColor = if (isLightMode) Color(0xFF081820) else Color(0xFF88C070)
    val backgroundColor = if (isLightMode) Color(0xFFF0FAF0) else Color(0xFF081820)

    Canvas(
        modifier = modifier
    ) {
        drawRect(
            color = backgroundColor,
            size = size
        )

        val cellSize = size.width / frame.width

        for (y in 0 until frame.height) {
            for (x in 0 until frame.width) {
                if (frame.pixels[y][x]) {
                    drawRect(
                        color = pixelColor,
                        topLeft = Offset(x * cellSize, y * cellSize),
                        size = Size(cellSize, cellSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun FrameThumbnail(
    frame: PixelFrame,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: (() -> Unit)?,
    thumbnailWidth: Float,
    thumbnailHeight: Float
) {
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline
    }

    Box(
        modifier = Modifier
            .size(width = thumbnailWidth.dp, height = thumbnailHeight.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(4.dp)
                )
                .clip(RoundedCornerShape(4.dp))
                .clickable { onSelect() }
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            FrameRenderer(
                frame = frame,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.icon_clear),
                    tint = Color.White,
                    contentDescription = "Delete",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun AddFrameButton(
    onClick: () -> Unit,
    width: Float,
    height: Float
) {
    Box(
        modifier = Modifier
            .size(width = width.dp, height = height.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp),
            )
            .clip(RoundedCornerShape(4.dp))
            .clickable { onClick() }
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
