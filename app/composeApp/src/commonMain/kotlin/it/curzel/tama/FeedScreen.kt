package it.curzel.tama

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import it.curzel.tama.midi.MidiPlayer

@Composable
fun FeedScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Feed")
        Text("Content feed will be displayed here")

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            MidiPlayer.playNote(60, 100, 500)
        }) {
            Text("Play Middle C")
        }
    }
}
