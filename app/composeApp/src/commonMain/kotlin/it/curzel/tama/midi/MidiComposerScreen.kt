package it.curzel.tama.midi

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.theme.MyNavigationBar
import it.curzel.tama.theme.TamaButton

@Composable
fun MidiComposerScreen(
    viewModel: MidiComposerViewModel = remember { MidiComposerViewModel() },
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = "MIDI Composer",
            onBackClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Composition",
                style = MaterialTheme.typography.titleLarge
            )

            OutlinedTextField(
                value = viewModel.composition,
                onValueChange = { viewModel.updateComposition(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                placeholder = {
                    Text("Enter MIDI notes (e.g., 4c 4e 4g 2c5)")
                },
                enabled = !viewModel.isPlaying
            )

            if (viewModel.errorMessage != null) {
                Text(
                    text = viewModel.errorMessage ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Playback",
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TamaButton(
                    onClick = { viewModel.play() },
                    enabled = !viewModel.isPlaying,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Play")
                }

                TamaButton(
                    onClick = { viewModel.stop() },
                    enabled = viewModel.isPlaying,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }

            if (viewModel.isPlaying) {
                Text(
                    text = "Playing on loop...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Help",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = "Format: [duration][#][note][octave][waveform][.volume]",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Duration: 1=whole, 2=half, 4=quarter, 8=eighth, 16=sixteenth, 32=32nd",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Note: c, d, e, f, g, a, b (add # for sharp, - for rest)",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Octave: 1-8 (default: 4)",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Waveform: q=square, t=triangle, s=sawtooth, p=pulse",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Volume: .0 to .9 (e.g., 4c.5 = 50% volume)",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Flags: --bpm [tempo], --volume [0.0-1.0], --adsr, --vibrato",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Multi-channel: --channel [notes] --channel [notes]",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Examples:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Basic: 4c 4e 4g 2c5",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Waveforms: 4ct 4et 4gt (triangle wave)",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Effects: --adsr --vibrato 2c 2e 2g",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                text = "Multi-ch: --channel 4c 4e --channel 4e 4g",
                style = MaterialTheme.typography.bodySmall
            )
        }
        }
    }
}
