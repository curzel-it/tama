package it.curzel.tama.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

object OrientationStubs {
    var isForcedLandscape = false
    var isForcedPortrait = false
}

@Composable
fun isLandscape(): Boolean {
    if (OrientationStubs.isForcedLandscape) return true
    if (OrientationStubs.isForcedPortrait) return false

    var isLandscapeMode by remember { mutableStateOf(false) }

    BoxWithConstraints {
        val currentIsLandscape = maxWidth > maxHeight
        if (isLandscapeMode != currentIsLandscape) {
            isLandscapeMode = currentIsLandscape
        }
    }

    return isLandscapeMode
}
