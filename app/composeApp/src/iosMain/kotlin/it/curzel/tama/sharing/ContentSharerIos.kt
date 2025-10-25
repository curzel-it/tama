package it.curzel.tama.sharing

import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import kotlinx.cinterop.convert

class ContentSharerIos : ContentSharer {
    override fun shareContent(url: String, onCopied: (() -> Unit)?) {
        try {
            val activityItems = listOf<Any?>(url)
            val activityViewController = UIActivityViewController(
                activityItems = activityItems,
                applicationActivities = null
            )

            val rootViewController = UIApplication.sharedApplication.keyWindow?.rootViewController
            rootViewController?.presentViewController(
                activityViewController,
                animated = true,
                completion = null
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
