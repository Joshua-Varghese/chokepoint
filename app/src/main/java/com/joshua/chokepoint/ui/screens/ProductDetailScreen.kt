package com.joshua.chokepoint.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.joshua.chokepoint.data.model.Product
import com.joshua.chokepoint.data.repository.CartRepository
import com.joshua.chokepoint.data.repository.CheckoutHelper
import com.joshua.chokepoint.data.repository.MarketplaceRepository
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val repository: MarketplaceRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    var product by mutableStateOf<Product?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun loadProduct(productId: String) {
        viewModelScope.launch {
            loading = true
            product = repository.getProduct(productId)
            if (product == null) {
                error = "Product not found"
            }
            loading = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String,
    repository: MarketplaceRepository,
    cartRepository: CartRepository,
    onBackClick: () -> Unit,
    onCartClick: () -> Unit
) {
    val viewModel: ProductDetailViewModel = viewModel {
        ProductDetailViewModel(repository, cartRepository)
    }

    LaunchedEffect(productId) {
        viewModel.loadProduct(productId)
    }

    val context = LocalContext.current
    val activity = context as? Activity
    val checkoutHelper = remember { activity?.let { CheckoutHelper(it) } }

    var selectedVariant by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Auto-select first variant if available and nothing selected
    LaunchedEffect(viewModel.product) {
        if (selectedVariant == null && !viewModel.product?.variants.isNullOrEmpty()) {
            selectedVariant = viewModel.product!!.variants[0]
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(viewModel.product?.name ?: "Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            viewModel.product?.let { product ->
                val basePrice = product.price
                val priceMod = (selectedVariant?.get("priceMod") as? Number)?.toDouble() ?: 0.0
                val totalPrice = basePrice + priceMod

                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Total",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "₹${totalPrice.toInt()}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Button(
                            onClick = {
                                if (checkoutHelper != null) {
                                    val variantName = selectedVariant?.get("name") as? String ?: "Standard"
                                    checkoutHelper.startPayment(
                                        product = product,
                                        variantName = variantName,
                                        totalPrice = totalPrice,
                                        onSuccess = { orderId ->
                                            Toast.makeText(context, "Order Placed! ID: $orderId", Toast.LENGTH_LONG).show()
                                        },
                                        onError = { msg ->
                                            Toast.makeText(context, "Error: $msg", Toast.LENGTH_LONG).show()
                                        }
                                    )
                                } else {
                                    Toast.makeText(context, "Checkout not available", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Buy Now")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            if (viewModel.loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (viewModel.product != null) {
                val product = viewModel.product!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    AsyncImage(
                        model = product.imageUrl,
                        contentDescription = product.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp),
                        contentScale = ContentScale.Crop
                    )

                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = product.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Tags
                        if (product.tags.isNotEmpty()) {
                            Row(modifier = Modifier.padding(vertical = 8.dp)) {
                                product.tags.forEach { tag ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = RoundedCornerShape(4.dp),
                                        modifier = Modifier.padding(end = 8.dp)
                                    ) {
                                        Text(
                                            text = tag.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Variants section
                        if (product.variants.isNotEmpty()) {
                            Text(
                                "Select Edition",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            product.variants.forEach { variant ->
                                val vName = variant["name"] as? String ?: ""
                                val vDesc = variant["description"] as? String ?: ""
                                val vPriceMod = (variant["priceMod"] as? Number)?.toDouble() ?: 0.0
                                val isSelected = selectedVariant == variant

                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { selectedVariant = variant }
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                            shape = RoundedCornerShape(8.dp)
                                        ),
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha=0.1f) else MaterialTheme.colorScheme.surface
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedVariant = variant }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(vName, fontWeight = FontWeight.Bold)
                                            if (vDesc.isNotEmpty()) {
                                                Text(vDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                        if (vPriceMod > 0) {
                                            Text("+₹${vPriceMod.toInt()}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Compatible Modules List (ReadOnly)
                        if (product.compatibleModules.isNotEmpty()) {
                            Text(
                                "Compatible Modules",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            Text(
                                "This platform supports ${product.compatibleModules.size} specific sensor/accessory modules.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = viewModel.error ?: "Unknown error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}
