package it.curzel.tama.content

import platform.Foundation.NSUserDefaults

class ContentWipStorageIos : ContentWipStorageProvider {
    private val userDefaults = NSUserDefaults.standardUserDefaults
    private val wipKey = "tama_wip_content"

    override suspend fun saveContent(content: String) {
        userDefaults.setObject(content, wipKey)
        userDefaults.synchronize()
    }

    override suspend fun loadContent(): String? {
        return try {
            userDefaults.stringForKey(wipKey)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearContent() {
        userDefaults.removeObjectForKey(wipKey)
        userDefaults.synchronize()
    }
}
