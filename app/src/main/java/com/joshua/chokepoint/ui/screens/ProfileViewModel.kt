package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.firestore.FirestoreRepository
import com.joshua.chokepoint.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth

class ProfileViewModel(
    private val firestoreRepository: FirestoreRepository
) : ViewModel() {

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        firestoreRepository.getUser(uid,
            onSuccess = { user ->
                _userProfile.value = user
                _isLoading.value = false
            },
            onFailure = {
                _isLoading.value = false
            }
        )
    }

    fun updateName(newName: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _isLoading.value = true
        firestoreRepository.updateName(uid, newName,
            onSuccess = {
                loadProfile() // Reload to reflect changes
            },
            onFailure = {
                _isLoading.value = false
            }
        )
    }
}
