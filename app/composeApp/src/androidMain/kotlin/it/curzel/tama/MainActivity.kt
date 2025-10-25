package it.curzel.tama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerAndroid
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MidiPlayer.provider = MidiPlayerAndroid()
        ConfigStorage.provider = ConfigStorageAndroid(applicationContext)

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}