package com.template.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.template.presentation.screens.ProfileScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel() : ViewModel() {
    private val _state = MutableStateFlow(ProfileScreenState())
    val state: StateFlow<ProfileScreenState> = _state

    fun toggleEdit() {
        _state.value = _state.value.copy(isEditing = !_state.value.isEditing)
    }

    fun updateName(name: String) {
        _state.value = _state.value.copy(name = name)
    }

    fun updatePhone(phone: String) {
        _state.value = _state.value.copy(phone = phone)
    }

    fun updateRole(role: String) {
        _state.value = _state.value.copy(role = role)
    }

    fun updatePosition(position: String) {
        _state.value = _state.value.copy(position = position)
    }

    fun updateSkills(skills: String) {
        _state.value = _state.value.copy(skills = skills)
    }

    fun saveProfile() {
        // Здесь должна быть логика сохранения профиля
        viewModelScope.launch {
            // Симуляция асинхронного сохранения
            _state.value = _state.value.copy(isEditing = false, saved = true)
        }
    }

    fun clearSaved() {
        _state.value = _state.value.copy(saved = false)
    }
}
