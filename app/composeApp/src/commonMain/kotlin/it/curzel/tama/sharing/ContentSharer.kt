package it.curzel.tama.sharing

interface ContentSharer {
    fun shareContent(url: String, onCopied: (() -> Unit)? = null)
}

object ContentSharingManager {
    lateinit var sharer: ContentSharer
}
