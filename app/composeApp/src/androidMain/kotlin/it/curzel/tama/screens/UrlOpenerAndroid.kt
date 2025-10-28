package it.curzel.tama.screens

import android.content.Context
import android.content.Intent
import android.net.Uri

class UrlOpenerAndroid(private val context: Context) : UrlOpener {
    override fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
