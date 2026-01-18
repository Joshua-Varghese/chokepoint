package com.joshua.chokepoint.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.joshua.chokepoint.data.model.Product
import com.joshua.chokepoint.data.repository.MarketplaceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class MarketplaceUiState {
    object Loading : MarketplaceUiState()
    data class Success(val products: List<Product>) : MarketplaceUiState()
    data class Error(val message: String) : MarketplaceUiState()
}

class MarketplaceViewModel(
    private val repository: MarketplaceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MarketplaceUiState>(MarketplaceUiState.Loading)
    val uiState: StateFlow<MarketplaceUiState> = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = MarketplaceUiState.Loading
            try {
                repository.getProducts().collect { products ->
                    _uiState.value = MarketplaceUiState.Success(products)
                }
            } catch (e: Exception) {
                _uiState.value = MarketplaceUiState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
