package it.curzel.tama.screens

import java.awt.Desktop
import java.net.URI

class UrlOpenerJvm : UrlOpener {
    override fun openUrl(url: String) {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
