package it.curzel.tama.screens

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class UrlOpenerIos : UrlOpener {
    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url)
        if (nsUrl != null && UIApplication.sharedApplication.canOpenURL(nsUrl)) {
            UIApplication.sharedApplication.openURL(nsUrl)
        }
    }
}
