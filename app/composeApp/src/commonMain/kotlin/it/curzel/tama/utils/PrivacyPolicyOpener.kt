package it.curzel.tama.utils

interface PrivacyPolicyOpener {
    fun openPrivacyPolicy(
        onShowWebView: () -> Unit
    )
}

object PrivacyPolicyManager {
    lateinit var opener: PrivacyPolicyOpener

    const val PRIVACY_POLICY_URL = "https://tama.curzel.it/privacy-policy.html"
}
