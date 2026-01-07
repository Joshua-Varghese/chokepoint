package com.joshua.chokepoint.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.joshua.chokepoint.data.model.CartItem
import com.joshua.chokepoint.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.tasks.await

interface CartRepository {
    suspend fun addToCart(product: Product, quantity: Int)
    fun getCartItems(): kotlinx.coroutines.flow.Flow<List<CartItem>>
    suspend fun removeFromCart(productId: String)
    suspend fun updateQuantity(productId: String, quantity: Int)
}

class CartRepositoryImpl : CartRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    override suspend fun addToCart(product: Product, quantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        val productRef = db.collection("users").document(userId).collection("cart").document(product.id)

        try {
            db.runTransaction { transaction ->
                val snapshot = transaction.get(productRef)
                val currentQuantity = snapshot.getLong("quantity")?.toInt() ?: 0
                val newQuantity = if (snapshot.exists()) currentQuantity + quantity else quantity

                val item = CartItem(
                    productId = product.id,
                    name = product.name,
                    price = product.price,
                    quantity = newQuantity,
                    imageUrl = product.imageUrl
                )
                transaction.set(productRef, item)
            }.await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getCartItems(): kotlinx.coroutines.flow.Flow<List<CartItem>> = kotlinx.coroutines.flow.callbackFlow {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            close()
            return@callbackFlow
        }

        val listener = db.collection("users").document(userId).collection("cart")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    close(e)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.documents.mapNotNull { it.toObject(CartItem::class.java) }
                    trySend(items)
                }
            }
        awaitClose { listener.remove() }
    }

    override suspend fun removeFromCart(productId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).collection("cart").document(productId).delete().await()
    }

    override suspend fun updateQuantity(productId: String, quantity: Int) {
        val userId = auth.currentUser?.uid ?: return
        if (quantity < 1) return // Stop at 1, do not delete

        db.collection("users").document(userId).collection("cart").document(productId)
            .update("quantity", quantity).await()
    }
}
