package it.curzel.tama.utils

import java.awt.Desktop
import java.net.URI

class PrivacyPolicyOpenerJvm : PrivacyPolicyOpener {
    override fun openPrivacyPolicy(onShowWebView: () -> Unit) {
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI(PrivacyPolicyManager.PRIVACY_POLICY_URL))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
