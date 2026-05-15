package com.kaushalyakarnataka.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kaushalyakarnataka.app.data.ChatMessage
import com.kaushalyakarnataka.app.data.KaushalyaRepository
import com.kaushalyakarnataka.app.data.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatDetailUiState(
    val messages: List<ChatMessage> = emptyList(),
    val otherUser: UserProfile? = null,
    val draft: String = "",
    val isSending: Boolean = false,
    val loadingHeader: Boolean = true,
    val error: String? = null,
)

class ChatDetailViewModel(
    private val chatId: String,
    private val selfUserId: String,
    private val repo: KaushalyaRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState: StateFlow<ChatDetailUiState> = _uiState.asStateFlow()

    init {
        observeMessages()
        fetchOtherParticipant()
    }

    private fun observeMessages() {
        viewModelScope.launch {
            try {
                repo.observeMessages(chatId).collect { incoming ->
                    _uiState.update { it.copy(messages = incoming) }
                }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _uiState.update { state -> state.copy(error = "Unable to load messages right now.") }
                }
            }
        }
    }

    private fun fetchOtherParticipant() {
        viewModelScope.launch {
            try {
                val parts = repo.fetchChatParticipantIds(chatId)
                val otherId = parts.firstOrNull { it != selfUserId }
                val other = if (otherId != null) repo.fetchUser(otherId) else null
                _uiState.update { it.copy(otherUser = other, loadingHeader = false) }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _uiState.update { it.copy(loadingHeader = false, error = "Unable to load participant details.") }
                }
            }
        }
    }

    fun onDraftChange(value: String) {
        _uiState.update { it.copy(draft = value) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun sendMessage() {
        val text = _uiState.value.draft.trim()
        if (text.isBlank() || _uiState.value.isSending) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, error = null) }
            try {
                repo.sendMessage(chatId, selfUserId, text)
                _uiState.update { it.copy(draft = "", isSending = false) }
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    _uiState.update { it.copy(isSending = false, error = "Message failed to send. Please retry.") }
                }
            }
        }
    }

    companion object {
        fun factory(
            chatId: String,
            selfUserId: String,
            repo: KaushalyaRepository,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatDetailViewModel(chatId, selfUserId, repo) as T
            }
        }
    }
}
