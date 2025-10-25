package it.curzel.tama.feed

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.api.FeedItem
import it.curzel.tama.canvas.AsciiContentWithTv
import it.curzel.tama.canvas.generateTvStatic
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.ShareButton
import it.curzel.tama.theme.TamaButton
import kotlinx.coroutines.delay

@Composable
fun FeedScreen(viewModel: FeedViewModel = remember { FeedViewModel() }) {
    LaunchedEffect(Unit) {
        viewModel.loadFeed()
    }

    LaunchedEffect(viewModel.currentIndex, viewModel.isShowingStatic) {
        if (viewModel.isShowingStatic) {
            viewModel.stopAudio()
        } else {
            delay(250)
            viewModel.playCurrentAudio()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onDispose()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "Tama Feed",
            rightAction = {
                ShareButton(
                    onClick = { viewModel.shareCurrentContent() },
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

                        AsciiContentWithTv(
                            content = if (viewModel.isShowingStatic) staticFrame else item.content.art,
                            fps = if (viewModel.isShowingStatic) 1f else item.content.fps,
                            modifier = Modifier.widthIn(max = 400.dp)
                        )

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
