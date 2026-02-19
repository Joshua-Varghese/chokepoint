package com.joshua.chokepoint.data.model

data class Product(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val imageUrl: String = "",
    val stock: Int = 0,
    val type: String = "module", // base | module | accessory
    val visibility: String = "", // featured | part
    val category: String = "",
    val tags: List<String> = emptyList(),
    val badges: List<String> = emptyList(), // New Arrival | Best Seller | Featured | Staff Pick
    val variants: List<Map<String, Any>> = emptyList(),
    val compatibleModules: List<String> = emptyList()
)
