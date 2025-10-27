package it.curzel.tama.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.curzel.tama.theme.TamaButton

@Composable
fun ReportContentDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    isLoading: Boolean,
    errorMessage: String?
) {
    // Use MaterialTheme colors which respect the app's theme preference
    val colorScheme = MaterialTheme.colorScheme

    // Match style.css modal colors
    // Light: modal-bg=#F0FAF0, border=#081820
    // Dark: modal-bg=#081820, border=#88C070
    val modalBg = colorScheme.background
    val modalBorder = colorScheme.outline
    val closeColor = colorScheme.outline
    val borderColor = colorScheme.outline

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(4.dp),
            color = modalBg,
            border = BorderStroke(1.dp, modalBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = "Report Content",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(
                        onClick = { if (!isLoading) onDismiss() },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Text(
                            text = "×",
                            fontSize = 28.sp,
                            color = closeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text("Why are you reporting this content?")

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = reason,
                    onValueChange = onReasonChange,
                    placeholder = {
                        Text(
                            "Please describe the issue...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 5,
                    enabled = !isLoading,
                    isError = errorMessage != null,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = borderColor,
                        unfocusedBorderColor = borderColor
                    )
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                TamaButton(
                    onClick = onConfirm,
                    enabled = reason.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = borderColor
                        )
                    } else {
                        Text("Submit Report")
                    }
                }
            }
        }
    }
}
