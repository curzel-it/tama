package it.curzel.tama.pixeleditor

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun AnimatedPreviewView(
    frames: List<PixelFrame>,
    fps: Float,
    canvasWidth: Int,
    canvasHeight: Int,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    if (frames.isEmpty()) return

    var currentFrameIndex by remember { mutableStateOf(0) }

    LaunchedEffect(frames, fps) {
        if (frames.size <= 1) {
            currentFrameIndex = 0
            return@LaunchedEffect
        }

        while (true) {
            delay((1000 / fps).toLong())
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
    }

    val (thumbWidth, thumbHeight) = FrameListUseCase.calculateThumbnailSize(
        isLandscape = isLandscape,
        canvasWidth = canvasWidth,
        canvasHeight = canvasHeight,
        maxSize = 80f
    )
    Box(
        modifier = Modifier
            .size(width = thumbWidth.dp, height = thumbHeight.dp)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(4.dp)
            )
            .clip(RoundedCornerShape(4.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        FrameRenderer(
            frame = frames[currentFrameIndex],
            modifier = Modifier.fillMaxSize()
        )
    }
}
