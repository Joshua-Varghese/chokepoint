package com.joshua.chokepoint.data.model

data class CartItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = ""
)
