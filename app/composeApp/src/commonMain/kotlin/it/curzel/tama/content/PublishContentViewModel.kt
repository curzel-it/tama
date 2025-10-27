package it.curzel.tama.content

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed class PublishState {
    object Idle : PublishState()
    object ConfirmationNeeded : PublishState()
    object Publishing : PublishState()
    data class Success(val contentId: Long, val channelId: Long) : PublishState()
    data class Error(val message: String) : PublishState()
}

class PublishContentViewModel : ViewModel() {
    var publishState by mutableStateOf<PublishState>(PublishState.Idle)
        private set

    fun showConfirmation() {
        publishState = PublishState.ConfirmationNeeded
    }

    fun publish() {
        publishState = PublishState.Publishing
        viewModelScope.launch {
            val result = PublishContentUseCase.publishContent()
            publishState = if (result.isSuccess) {
                result.getOrNull()?.let { response ->
                    PublishState.Success(response.id, response.channelId)
                } ?: PublishState.Error("Failed to parse response")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                PublishState.Error(error)
            }
        }
    }

    fun dismissDialog() {
        publishState = PublishState.Idle
    }
}
