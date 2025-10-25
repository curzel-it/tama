package it.curzel.tama.pixeleditor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.curzel.tama.canvas.AsciiContentView
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.delay

@Composable
fun AnimatedPreviewPanel(
    frames: List<PixelFrame>,
    fps: Float,
    charWidth: Int,
    charHeight: Int,
    visible: Boolean,
    onClose: () -> Unit
) {
    if (!visible || frames.isEmpty()) {
        return
    }

    var currentFrameIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(frames, fps) {
        if (frames.size > 1) {
            while (true) {
                delay((1000 / fps).toLong())
                currentFrameIndex = (currentFrameIndex + 1) % frames.size
            }
        }
    }

    val brailleContent = remember(frames, currentFrameIndex) {
        if (frames.isNotEmpty()) {
            val frame = frames.getOrNull(currentFrameIndex) ?: frames.first()
            BrailleConverter.pixelsToBraille(frame.pixels)
        } else {
            ""
        }
    }

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .background(
                    color = MaterialTheme.colorScheme.background,
                    shape = RoundedCornerShape(8.dp)
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Animation Preview",
                    style = MaterialTheme.typography.titleMedium
                )

                TamaButton(
                    onClick = onClose,
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        text = "×",
                        fontSize = 20.sp
                    )
                }
            }

            if (brailleContent.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    AsciiContentView(
                        content = brailleContent,
                        fps = fps,
                        showTvFrame = false
                    )
                }
            }

            Text(
                text = "Frame ${currentFrameIndex + 1} / ${frames.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
