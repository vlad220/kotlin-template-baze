package com.template.presentation.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PaymentState(
    val loading: Boolean = false,
    val done: Boolean = false,
)

class DonationViewModel : ViewModel() {

    private val _state = MutableStateFlow(PaymentState())
    val state: StateFlow<PaymentState> = _state.asStateFlow()

    fun paid() {
        _state.value = _state.value.copy(done = true)
    }

    fun reset() {
        _state.value = PaymentState()
    }
}