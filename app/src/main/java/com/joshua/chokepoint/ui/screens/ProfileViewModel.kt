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

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        _error.value = null
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (currentUser == null) {
             _error.value = "Not Logged In"
             return
        }
        val uid = currentUser.uid
        
        _isLoading.value = true
        firestoreRepository.getUser(uid,
            onSuccess = { user ->
                if (user != null) {
                    _userProfile.value = user
                    _isLoading.value = false
                } else {
                    // Profile missing! Attempt self-healing
                    val email = currentUser.email ?: ""
                    val name = currentUser.displayName ?: "User"
                    
                    firestoreRepository.syncUserProfile(uid, email, name,
                        onSuccess = {
                            // Retry loading after creation
                            loadProfile()
                        },
                        onFailure = { e ->
                            _isLoading.value = false
                            _error.value = "Failed to create profile: ${e.message}"
                        }
                    )
                }
            },
            onFailure = { e ->
                _isLoading.value = false
                _error.value = "Failed to load profile: ${e.message}"
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
