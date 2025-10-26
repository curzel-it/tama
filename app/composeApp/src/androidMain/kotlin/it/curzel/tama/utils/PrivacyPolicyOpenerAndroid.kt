package it.curzel.tama.utils

class PrivacyPolicyOpenerAndroid : PrivacyPolicyOpener {
    override fun openPrivacyPolicy(onShowWebView: () -> Unit) {
        onShowWebView()
    }

    override fun openTermsAndConditions(onShowWebView: () -> Unit) {
        onShowWebView()
    }
}
