package it.curzel.tama.screenshot

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.singleWindowApplication
import java.awt.Dimension
import javax.swing.SwingUtilities
import it.curzel.tama.App
import it.curzel.tama.api.ApiManager
import it.curzel.tama.content.ContentWipStorageJvm
import it.curzel.tama.content.ContentWipUseCase
import it.curzel.tama.midi.MidiComposer
import it.curzel.tama.midi.MidiComposerJvm
import it.curzel.tama.midi.MidiPlayer
import it.curzel.tama.midi.MidiPlayerJvm
import it.curzel.tama.screens.UrlOpenerHolder
import it.curzel.tama.screens.UrlOpenerJvm
import it.curzel.tama.storage.ConfigStorage
import it.curzel.tama.storage.ConfigStorageJvm
import it.curzel.tama.storage.ReportedContentStorage
import it.curzel.tama.storage.ReportedContentStorageJvm
import it.curzel.tama.utils.PrivacyPolicyManager
import it.curzel.tama.utils.PrivacyPolicyOpenerJvm
import it.curzel.tama.sharing.ContentSharingManager
import it.curzel.tama.sharing.ContentSharerJvm
import it.curzel.tama.utils.OrientationStubs
import it.curzel.tama.version.Platform
import it.curzel.tama.version.VersionUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomepageScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        MidiPlayer.provider = MidiPlayerJvm()
        MidiComposer.backend = MidiComposerJvm()
        ConfigStorage.provider = ConfigStorageJvm()
        ReportedContentStorage.provider = ReportedContentStorageJvm()
        PrivacyPolicyManager.opener = PrivacyPolicyOpenerJvm()
        ContentSharingManager.sharer = ContentSharerJvm()
        ContentWipUseCase.storageProvider = ContentWipStorageJvm()
        UrlOpenerHolder.urlOpener = UrlOpenerJvm()

        OrientationStubs.isForcedPortrait = true

        val config = runBlocking { ConfigStorage.loadConfig() }
        val serverUrl = config?.server_url ?: "https://tama.curzel.it"
        VersionUseCase.apiClient = ApiManager.getClient(serverUrl)
        VersionUseCase.currentPlatform = Platform.JVM
    }

    @Test
    fun captureHomepageLight() {
        captureHomepageWithTheme(darkTheme = false, themeName = "light")
    }

    @Test
    fun captureHomepageDark() {
        captureHomepageWithTheme(darkTheme = true, themeName = "dark")
    }

    private fun captureHomepageWithTheme(darkTheme: Boolean, themeName: String) {
        val width = 400
        val height = 1000

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = 1f,
                    fontScale = 1f
                )
            ) {
                App(
                    darkThemeOverride = darkTheme,
                    modifier = Modifier.width(width.dp).height(height.dp)
                )
            }
        }

        composeTestRule.waitForIdle()
        Thread.sleep(5000)

        composeTestRule.onRoot().captureScreenshot(
            label = "homepage",
            width = width,
            height = height,
            theme = themeName
        )
    }
}
