package it.curzel.tama.screenshot

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
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

class ScreenshotTest {

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
    fun takeScreenshots() {
        listOf(IPHONE_6_5, IPAD_13, ANDROID_FHD).forEach { size ->
            listOf(DARK_MODE, LIGHT_MODE).forEach { darkTheme ->
                // Homepage (Feed)
                capture("homepage", darkTheme, size) {
                    Thread.sleep(1000)
                }

                // Content Editor
                capture("content-editor", darkTheme, size) { rule ->
                    rule.onNodeWithTag("tab-Create").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                }

                // Pixel Art Editor
                capture("pixel-editor", darkTheme, size) { rule ->
                    rule.onNodeWithTag("tab-Create").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                    rule.onNodeWithTag("edit-animation-button").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                }

                // MIDI Composer
                capture("midi-composer", darkTheme, size) { rule ->
                    rule.onNodeWithTag("tab-Create").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                    rule.onNodeWithTag("edit-audio-button").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                }

                // Settings
                capture("settings", darkTheme, size) { rule ->
                    rule.onNodeWithTag("tab-Settings").performClick()
                    rule.waitForIdle()
                    Thread.sleep(1000)
                }
            }
        }
    }

    private fun capture(
        label: String,
        darkTheme: Boolean,
        originalSize: IntSize,
        navigate: (ComposeTestRule) -> Unit
    ) {
        val scaleFactor = 4
        val size = originalSize.scale(1 / scaleFactor.toFloat())

        composeTestRule.setContent {
            val density = Density(
                density = 1f,
                fontScale = 1f
            )
            CompositionLocalProvider(
                LocalDensity provides density
            ) {
                val widthDp = with(density) { size.width.toDp() }
                val heightDp = with(density) { size.height.toDp() }
                App(
                    darkThemeOverride = darkTheme,
                    modifier = Modifier.width(widthDp).height(heightDp)
                )
            }
        }

        composeTestRule.waitForIdle()
        Thread.sleep(1000)
        navigate(composeTestRule)
        Thread.sleep(3000)

        composeTestRule.onRoot().captureScreenshot(
            label = label,
            originalSize = originalSize,
            theme = if (darkTheme) "dark" else "light",
        )
    }
}

private const val DARK_MODE = true
private const val LIGHT_MODE = false

private val IPHONE_6_5 = IntSize(1242, 2688)
private val IPAD_13 = IntSize(2064, 2752)
private val ANDROID_FHD = IntSize(1080, 1920)

private fun IntSize.scale(v: Float): IntSize {
    return IntSize((width * v).toInt(), (height * v).toInt())
}
