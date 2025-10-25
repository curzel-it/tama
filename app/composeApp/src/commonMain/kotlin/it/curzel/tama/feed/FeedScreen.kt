package it.curzel.tama.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import it.curzel.tama.api.FeedItem
import it.curzel.tama.canvas.AsciiContentWithTv
import it.curzel.tama.canvas.generateTvStatic
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun FeedScreen(viewModel: FeedViewModel = remember { FeedViewModel() }) {
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportError by remember { mutableStateOf<String?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    LaunchedEffect(viewModel.currentIndex, viewModel.isShowingStatic, viewModel.currentItem) {
        if (viewModel.isShowingStatic) {
            viewModel.stopAudio()
        } else {
            delay(250)
            viewModel.playCurrentAudio()
        }
    }

    LaunchedEffect(showReportSuccess) {
        if (showReportSuccess) {
            snackbarHostState.showSnackbar(
                message = "Thank you for your report. We will review this content.",
                duration = SnackbarDuration.Short
            )
            showReportSuccess = false
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }

    if (showReportDialog) {
        ReportContentDialog(
            reason = reportReason,
            onReasonChange = { reportReason = it },
            onDismiss = {
                showReportDialog = false
                reportReason = ""
                reportError = null
            },
            onConfirm = {
                isReporting = true
                reportError = null
                viewModel.reportCurrentContent(
                    reason = reportReason,
                    onSuccess = {
                        isReporting = false
                        showReportDialog = false
                        reportReason = ""
                        showReportSuccess = true
                    },
                    onError = { error ->
                        isReporting = false
                        reportError = error
                    }
                )
            },
            isLoading = isReporting,
            errorMessage = reportError
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "Tama Feed",
            rightAction = {
                ContentKebabMenu(
                    onShareClick = {
                        viewModel.shareCurrentContent {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = "Link copied to clipboard",
                                    duration = SnackbarDuration.Short
                                )
                            }
                        }
                    },
                    onReportClick = { showReportDialog = true },
                    enabled = viewModel.currentItem != null && !viewModel.isShowingStatic
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
        when {
            viewModel.isLoading && viewModel.feedItems.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            viewModel.errorMessage != null && viewModel.feedItems.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Error: ${viewModel.errorMessage}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            viewModel.feedItems.isEmpty() && !viewModel.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nothing to see here, please check back later")
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    viewModel.currentItem?.let { item ->
                        val staticFrame = remember { generateTvStatic() }

                        BoxWithConstraints(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            AsciiContentWithTv(
                                content = if (viewModel.isShowingStatic) staticFrame else item.content.art,
                                fps = if (viewModel.isShowingStatic) 1f else item.content.fps,
                                availableWidthDp = maxWidth.coerceAtMost(400.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TamaButton(
                                onClick = { viewModel.previous() },
                                enabled = !viewModel.isShowingStatic
                            ) {
                                Text("← Previous")
                            }

                            TamaButton(
                                onClick = { viewModel.next() },
                                enabled = !viewModel.isShowingStatic
                            ) {
                                Text("Next →")
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FeedItemView(item, viewModel.isShowingStatic)
                    }

                    if (viewModel.loadingServers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Loading from ${viewModel.loadingServers.size} more server(s)...",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        }
    }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FeedItemView(item: FeedItem, isShowingStatic: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Channel: ${item.channel.name}",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ContentKebabMenu(
    onShareClick: () -> Unit,
    onReportClick: () -> Unit,
    enabled: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            enabled = enabled
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    expanded = false
                    onShareClick()
                }
            )
            DropdownMenuItem(
                text = { Text("Report") },
                onClick = {
                    expanded = false
                    onReportClick()
                }
            )
        }
    }
}

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
                    placeholder = { Text("Please describe the issue...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
