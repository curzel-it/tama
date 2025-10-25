package it.curzel.tama.utils

class PrivacyPolicyOpenerIos : PrivacyPolicyOpener {
    override fun openPrivacyPolicy(onShowWebView: () -> Unit) {
        onShowWebView()
    }
}
