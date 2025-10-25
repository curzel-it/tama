package it.curzel.tama.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun MyNavigationBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left button (back button)
            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (onBackClick != null) {
                    TextButton(onClick = onBackClick) {
                        Text("← Back")
                    }
                }
            }

            // Title (center, flexible)
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Right button
            Box(
                modifier = Modifier.width(64.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                rightAction?.invoke()
            }
        }
    }
}

@Composable
fun ShareButton(
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        enabled = enabled
    ) {
        Text("Share")
    }
}
