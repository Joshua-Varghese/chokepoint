package com.joshua.chokepoint.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val inStock: Boolean = true,
    val quirks: List<String> = emptyList()
)
