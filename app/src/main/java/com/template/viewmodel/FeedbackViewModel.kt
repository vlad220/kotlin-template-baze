package com.template.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.template.data.repository.WarehouseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class FeedbackUiState(
    val text: String = "",
    val isSending: Boolean = false,
    val result: String? = null
)

class FeedbackViewModel : ViewModel() {

    private val repository = WarehouseRepository()

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    fun updateText(text: String) { _state.value = _state.value.copy(text = text) }

    fun submit() {
        val s = _state.value
        if (s.text.isBlank()) return
        _state.value = s.copy(isSending = true, result = null)

        viewModelScope.launch {
            val res = repository.sendFeedback("feedback", s.text)
            _state.value = _state.value.copy(
                isSending = false,
                result = if (res.isSuccess) "Спасибо! Сообщение отправлено" else "Ошибка отправки. Попробуйте позже"
            )
        }
    }

    fun clearResult() { _state.value = _state.value.copy(result = null) }
}
