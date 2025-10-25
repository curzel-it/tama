package it.curzel.tama.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TamaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    // Use MaterialTheme colors which respect the app's theme preference
    // Light: background=#F0FAF0, onBackground=#081820, primary=#88C070, outline=#081820
    // Dark: background=#081820, onBackground=#88C070, primary=#88C070, outline=#88C070
    val colorScheme = MaterialTheme.colorScheme

    // Match style.css button styling
    // Light mode: bg=#88C070, text=#081820, border=#081820
    // Dark mode: bg=#081820, text=white, border=#88C070
    val containerColor = if (colorScheme.background == Color(0xFFF0FAF0)) {
        colorScheme.primary // #88C070 in light mode
    } else {
        colorScheme.background // #081820 in dark mode
    }

    val contentColor = if (colorScheme.background == Color(0xFFF0FAF0)) {
        colorScheme.onBackground // #081820 in light mode
    } else {
        Color.White // white in dark mode
    }

    val borderColor = colorScheme.outline

    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.3f),
            disabledContentColor = contentColor.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, borderColor),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)
    ) {
        content()
    }
}
