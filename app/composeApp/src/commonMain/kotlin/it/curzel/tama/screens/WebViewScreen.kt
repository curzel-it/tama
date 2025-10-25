package it.curzel.tama.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.multiplatform.webview.web.WebView
import com.multiplatform.webview.web.rememberWebViewState
import it.curzel.tama.theme.MyNavigationBar

@Composable
fun WebViewScreen(
    url: String,
    title: String,
    onBack: () -> Unit
) {
    val state = rememberWebViewState(url)

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        MyNavigationBar(
            title = title,
            onBackClick = onBack
        )

        WebView(
            state = state,
            modifier = Modifier.fillMaxSize()
        )
    }
}
