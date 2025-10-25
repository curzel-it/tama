package it.curzel.tama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerAndroid
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerAndroid
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageAndroid
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerAndroid

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        MidiPlayer.provider = MidiPlayerAndroid()
        MidiComposer.backend = MidiComposerAndroid()
        ConfigStorage.provider = ConfigStorageAndroid(applicationContext)
        PrivacyPolicyManager.opener = PrivacyPolicyOpenerAndroid()

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