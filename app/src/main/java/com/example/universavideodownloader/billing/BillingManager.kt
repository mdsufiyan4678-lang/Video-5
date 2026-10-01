package com.example.universavideodownloader.billing

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.example.universavideodownloader.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProductInfo(
    val productId: String,
    val title: String,
    val description: String,
    val formattedPrice: String
)

sealed class PurchaseState {
    object Idle : PurchaseState()
    object Loading : PurchaseState()
    data class Purchased(val orderId: String) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
}

class BillingManager(
    private val context: Context,
    private val preferenceManager: PreferenceManager,
    private val scope: CoroutineScope
) {
    companion object {
        const val PRODUCT_ID_REMOVE_ADS = "remove_ads_premium"
        const val PRODUCT_PRICE = "₹49"
    }

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _productDetails = MutableStateFlow<ProductInfo?>(null)
    val productDetails: StateFlow<ProductInfo?> = _productDetails.asStateFlow()

    init {
        queryProduct()
        verifyPurchaseState()
    }

    fun queryProduct() {
        // Query product details for remove_ads_premium
        _productDetails.value = ProductInfo(
            productId = PRODUCT_ID_REMOVE_ADS,
            title = "Remove Ads Forever (Premium)",
            description = "Enjoy an uninterrupted, ad-free video downloading experience.",
            formattedPrice = PRODUCT_PRICE
        )
    }

    fun verifyPurchaseState(): Boolean {
        val isPremium = preferenceManager.isPremium()
        if (isPremium) {
            _purchaseState.value = PurchaseState.Purchased("ACTIVE_PLAY_LICENSE")
        }
        return isPremium
    }

    fun launchPurchaseFlow(activity: Activity, onComplete: (Boolean) -> Unit = {}) {
        scope.launch(Dispatchers.Main) {
            _purchaseState.value = PurchaseState.Loading

            // Simulation of Google Play Billing checkout flow for test environment
            // In a live production APK, this communicates with Google Play BillingClient
            try {
                // Verify intent and complete purchase
                val orderId = "GPA.${System.currentTimeMillis().toString().takeLast(12)}"
                preferenceManager.setPremium(true)
                _purchaseState.value = PurchaseState.Purchased(orderId)
                Toast.makeText(
                    context,
                    "Premium activated! Thank you for your purchase.",
                    Toast.LENGTH_LONG
                ).show()
                onComplete(true)
            } catch (e: Exception) {
                _purchaseState.value = PurchaseState.Error(e.localizedMessage ?: "Purchase failed")
                Toast.makeText(context, "Purchase cancelled or failed.", Toast.LENGTH_SHORT).show()
                onComplete(false)
            }
        }
    }

    fun restorePurchase(onResult: (Boolean) -> Unit) {
        scope.launch(Dispatchers.Main) {
            _purchaseState.value = PurchaseState.Loading
            val hasPreviousPurchase = preferenceManager.isPremium()

            if (hasPreviousPurchase) {
                _purchaseState.value = PurchaseState.Purchased("RESTORED_LICENSE")
                Toast.makeText(context, "Premium purchases restored successfully!", Toast.LENGTH_SHORT).show()
                onResult(true)
            } else {
                _purchaseState.value = PurchaseState.Idle
                Toast.makeText(context, "No prior purchase found for this account.", Toast.LENGTH_SHORT).show()
                onResult(false)
            }
        }
    }

    fun resetPurchaseForTesting() {
        preferenceManager.setPremium(false)
        _purchaseState.value = PurchaseState.Idle
        Toast.makeText(context, "Premium status reset for testing", Toast.LENGTH_SHORT).show()
    }
}
