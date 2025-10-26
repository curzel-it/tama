package it.curzel.tama.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import tama.composeapp.generated.resources.Res
import tama.composeapp.generated.resources.icon_back

@Composable
fun MyNavigationBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    rightAction: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left section: back button + title
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_back),
                        contentDescription = "Back",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = if (onBackClick != null) 8.dp else 16.dp)
            )
        }

        // Right button
        Box(
            modifier = Modifier.width(64.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            rightAction?.invoke()
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
