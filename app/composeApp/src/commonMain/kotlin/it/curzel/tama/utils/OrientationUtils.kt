package it.curzel.tama.utils

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun isLandscape(): Boolean {
    var landscape by remember { mutableStateOf(false) }
    BoxWithConstraints {
        landscape = maxWidth > maxHeight
    }
    return landscape
}
