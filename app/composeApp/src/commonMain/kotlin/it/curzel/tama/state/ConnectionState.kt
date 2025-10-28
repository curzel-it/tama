package it.curzel.tama.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object ConnectionState {
    var hasConnectionError by mutableStateOf(false)
        private set

    private var retryCallback: (() -> Unit)? = null

    fun setConnectionError(hasError: Boolean, onRetry: (() -> Unit)? = null) {
        hasConnectionError = hasError
        if (hasError) {
            retryCallback = onRetry
        }
    }

    fun clearConnectionError() {
        hasConnectionError = false
        retryCallback = null
    }

    fun retry() {
        retryCallback?.invoke()
    }
}
