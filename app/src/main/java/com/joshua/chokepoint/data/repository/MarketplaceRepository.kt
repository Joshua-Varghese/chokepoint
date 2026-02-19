package com.joshua.chokepoint.data.repository

import com.joshua.chokepoint.data.model.Product
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface MarketplaceRepository {
    fun getProducts(): Flow<List<Product>>
    suspend fun getProduct(productId: String): Product?
}

class MarketplaceRepositoryImpl : MarketplaceRepository {
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
    private val collection = db.collection("products")

    override fun getProducts(): Flow<List<Product>> = kotlinx.coroutines.flow.callbackFlow {
        // We fetch ALL and filter client-side to handle the legacy mix + complex OR logic
        // Ideally, you'd have a specific index for this, but client-side filtering 
        // for < 100 products is instant and safer for legacy data.
        val listener = collection.addSnapshotListener { snapshot, e ->
            if (e != null) {
                close(e)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val products = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(Product::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        null
                    }
                }.filter { p ->
                    // Show if explicitly featured OR (no visibility set AND is a base unit)
                    p.visibility == "featured" || (p.visibility.isEmpty() && p.type == "base")
                }
                trySend(products)
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getProduct(productId: String): Product? {
        return try {
            val doc = collection.document(productId).get().await()
            doc.toObject(Product::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            null
        }
    }
}
