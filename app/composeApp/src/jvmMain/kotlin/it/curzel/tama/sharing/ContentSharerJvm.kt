package it.curzel.tama.sharing

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class ContentSharerJvm : ContentSharer {
    override fun shareContent(url: String, onCopied: (() -> Unit)?) {
        try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            val stringSelection = StringSelection(url)
            clipboard.setContents(stringSelection, null)
            onCopied?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
