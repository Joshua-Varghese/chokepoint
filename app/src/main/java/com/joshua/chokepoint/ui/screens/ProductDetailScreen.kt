package com.joshua.chokepoint.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.joshua.chokepoint.data.model.Product
import com.joshua.chokepoint.data.repository.CartRepository
import com.joshua.chokepoint.data.repository.MarketplaceRepository
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    private val productId: String,
    private val repository: MarketplaceRepository,
    private val cartRepository: CartRepository
) : ViewModel() {
    
    var product by mutableStateOf<Product?>(null)
    var isLoading by mutableStateOf(true)
    var isAdding by mutableStateOf(false)

    init {
        loadProduct()
    }

    private fun loadProduct() {
        viewModelScope.launch {
            isLoading = true
            product = repository.getProduct(productId)
            isLoading = false
        }
    }

    fun addToCart(onSuccess: () -> Unit) {
        val p = product ?: return
        viewModelScope.launch {
            isAdding = true
            cartRepository.addToCart(p, 1)
            isAdding = false
            onSuccess()
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
    val viewModel = remember { ProductDetailViewModel(productId, repository, cartRepository) }
    val context = LocalContext.current
    val product = viewModel.product

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product?.name ?: "Loading...") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onCartClick) {
                         Icon(Icons.Default.ShoppingCart, contentDescription = "Cart")
                    }
                }
            )
        },
        bottomBar = {
             if (product != null) {
                 Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = { 
                                viewModel.addToCart {
                                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isAdding
                        ) {
                             Icon(Icons.Default.ShoppingCart, contentDescription = null)
                             Spacer(modifier = Modifier.width(8.dp))
                             Text("Add to Cart")
                        }
                        Button(
                            onClick = { 
                                 viewModel.addToCart {
                                    onCartClick()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !viewModel.isAdding
                        ) {
                            Text("Buy Now")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (product != null) {
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
                            .height(300.dp),
                        contentScale = ContentScale.Crop
                    )
                    
                    Column(modifier = Modifier.padding(24.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                             Text(
                                text = product.name,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "₹${product.price}",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                       
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        if (product.inStock) {
                            Surface(color = Color(0xFFE8F5E9), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "In Stock",
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        } else {
                             Surface(color = Color(0xFFFFEBEE), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    "Out of Stock",
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text("Description", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                 Text("Product not found", modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
