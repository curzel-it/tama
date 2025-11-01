package it.curzel.tama.content

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.theme.TamaButton
import it.curzel.tama.storage.ConfigStorage

@Composable
fun PublishContentDialog(
    publishState: PublishState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onNavigateToContent: (Long) -> Unit,
    onShowToast: (String) -> Unit = {}
) {
    when (publishState) {
        is PublishState.ConfirmationNeeded -> {
            ConfirmationDialog(
                onDismiss = onDismiss,
                onConfirm = onConfirm
            )
        }

        is PublishState.Publishing -> {
            PublishingDialog()
        }

        is PublishState.Success -> {
            SuccessDialog(
                contentId = publishState.contentId,
                onDismiss = onDismiss,
                onNavigateToContent = onNavigateToContent,
                onShowToast = onShowToast
            )
        }

        is PublishState.Error -> {
            ErrorDialog(
                message = publishState.message,
                onDismiss = onDismiss
            )
        }

        else -> {}
    }
}

@Composable
private fun ConfirmationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val modalBg = colorScheme.background
    val modalBorder = colorScheme.outline

    Dialog(onDismissRequest = onDismiss) {
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Publish Content?",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Your content will be published to your channel and visible to everyone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    TamaButton(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Publish")
                    }
                }
            }
        }
    }
}

@Composable
private fun PublishingDialog() {
    val colorScheme = MaterialTheme.colorScheme
    val modalBg = colorScheme.background
    val modalBorder = colorScheme.outline

    Dialog(onDismissRequest = {}) {
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
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Publishing...",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun SuccessDialog(
    contentId: Long,
    onDismiss: () -> Unit,
    onNavigateToContent: (Long) -> Unit,
    onShowToast: (String) -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val modalBg = colorScheme.background
    val modalBorder = colorScheme.outline
    var serverUrl by remember { mutableStateOf("https://tama.curzel.it") }

    LaunchedEffect(Unit) {
        val config = ConfigStorage.loadConfig()
        serverUrl = config?.server_url ?: "https://tama.curzel.it"
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(4.dp),
            color = modalBg,
            border = BorderStroke(1.dp, modalBorder)
        ) {
            Box {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Success!",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Your content is now live",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TamaButton(
                            onClick = { onNavigateToContent(contentId) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("View Content")
                        }
                        TamaButton(
                            onClick = {
                                val shareUrl = "$serverUrl/view/content/$contentId"
                                ContentSharingManager.sharer.shareContent(shareUrl) {
                                    onShowToast("Link copied to clipboard!")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Share")
                        }
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val modalBg = colorScheme.background
    val modalBorder = colorScheme.outline

    Dialog(onDismissRequest = onDismiss) {
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
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Publishing Failed",
                    style = MaterialTheme.typography.titleLarge,
                    color = colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                TamaButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}
