package it.curzel.tama.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
fun FeedScreen(
    priorityContentId: Long? = null,
    viewModel: FeedViewModel = remember { FeedViewModel() },
    isLandscape: Boolean = false
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var reportReason by remember { mutableStateOf("") }
    var reportError by remember { mutableStateOf<String?>(null) }
    var showReportSuccess by remember { mutableStateOf(false) }
    var isReporting by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(priorityContentId) {
        viewModel.loadFeed(priorityContentId)
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
            if (!isLandscape) {
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
            }

            FeedContent(
                viewModel = viewModel,
                isLandscape = isLandscape,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun FeedContent(
    viewModel: FeedViewModel,
    isLandscape: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        when {
            viewModel.isLoading && viewModel.feedItems.isEmpty() -> {
                FeedLoadingState()
            }

            viewModel.errorMessage != null && viewModel.feedItems.isEmpty() -> {
                FeedLoadingState()
            }

            viewModel.feedItems.isEmpty() && !viewModel.isLoading -> {
                FeedEmptyState()
            }

            else -> {
                FeedItemWithControls(
                    viewModel = viewModel,
                    isLandscape = isLandscape
                )

                FeedLoadingIndicator(
                    loadingServers = viewModel.loadingServers
                )
            }
        }
    }
}

@Composable
fun FeedLoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun FeedEmptyState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text("Nothing to see here, please check back later")
    }
}

@Composable
fun FeedItemWithControls(
    viewModel: FeedViewModel,
    isLandscape: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        viewModel.currentItem?.let { item ->
            val staticFrame = remember { generateTvStatic() }

            if (isLandscape) {
                FeedLandscapeLayout(
                    item = item,
                    staticFrame = staticFrame,
                    viewModel = viewModel,
                    isShowingStatic = viewModel.isShowingStatic
                )
            } else {
                FeedPortraitLayout(
                    item = item,
                    staticFrame = staticFrame,
                    viewModel = viewModel,
                    isShowingStatic = viewModel.isShowingStatic
                )
            }
        }
    }
}

@Composable
fun FeedLoadingIndicator(loadingServers: Set<String>) {
    if (loadingServers.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Loading from ${loadingServers.size} more server(s)...",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun FeedPortraitLayout(
    item: FeedItem,
    staticFrame: String,
    viewModel: FeedViewModel,
    isShowingStatic: Boolean
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val availableWidth = maxWidth.coerceAtMost(400.dp)

        AsciiContentWithTv(
            content = if (isShowingStatic) staticFrame else item.content.art,
            fps = if (isShowingStatic) 1f else item.content.fps,
            showGrid = true,
            availableWidthDp = availableWidth
        )
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TamaButton(
            onClick = { viewModel.previous() },
            enabled = !isShowingStatic
        ) {
            Text("← Previous")
        }

        TamaButton(
            onClick = { viewModel.next() },
            enabled = !isShowingStatic
        ) {
            Text("Next →")
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    FeedItemView(item, isShowingStatic)
}

@Composable
fun FeedLandscapeLayout(
    item: FeedItem,
    staticFrame: String,
    viewModel: FeedViewModel,
    isShowingStatic: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.weight(1f))

        BoxWithConstraints {
            val buttonColumnWidth = 140.dp
            val horizontalSpacing = 32.dp
            val verticalSpacingNeeds = 90.dp

            val tvCharsWide = 36f
            val tvCharsHigh = 14f

            val availableHeight = maxHeight - verticalSpacingNeeds
            val availableWidth = maxWidth - buttonColumnWidth - horizontalSpacing

            val charHeightFromVertical = availableHeight / tvCharsHigh
            val charWidthFromVertical = charHeightFromVertical / 2f
            val tvWidthFromHeight = charWidthFromVertical * tvCharsWide

            val charWidthFromHorizontal = availableWidth / tvCharsWide
            val charHeightFromHorizontal = charWidthFromHorizontal * 2f
            val tvHeightFromWidth = charHeightFromHorizontal * tvCharsHigh

            val tvWidth = if (tvHeightFromWidth <= availableHeight) {
                availableWidth
            } else {
                tvWidthFromHeight
            }

            AsciiContentWithTv(
                content = if (isShowingStatic) staticFrame else item.content.art,
                fps = if (isShowingStatic) 1f else item.content.fps,
                showGrid = true,
                availableWidthDp = tvWidth,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(140.dp)
        ) {
            TamaButton(
                onClick = { viewModel.next() },
                enabled = !isShowingStatic,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Next →")
            }
            TamaButton(
                onClick = { viewModel.previous() },
                enabled = !isShowingStatic,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("← Previous")
            }
        }
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
    val colorScheme = MaterialTheme.colorScheme

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
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(colorScheme.background)
                .border(BorderStroke(1.dp, colorScheme.outline), RoundedCornerShape(2.dp)),
            shape = RoundedCornerShape(2.dp)
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