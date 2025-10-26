package it.curzel.tama.content

import android.content.Context

class ContentWipStorageAndroid(context: Context) : ContentWipStorageProvider {
    private val prefs = context.getSharedPreferences("tama_wip", Context.MODE_PRIVATE)
    private val wipKey = "wip_content"

    override suspend fun saveContent(content: String) {
        prefs.edit().putString(wipKey, content).apply()
    }

    override suspend fun loadContent(): String? {
        return try {
            prefs.getString(wipKey, null)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearContent() {
        prefs.edit().remove(wipKey).apply()
    }
}
