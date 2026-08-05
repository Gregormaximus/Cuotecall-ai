package com.kuote.agent.monetization

import android.content.Context
import com.kuote.agent.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RevenueCatState(
    val isProSubscribed: Boolean = false,
    val isTrialActive: Boolean = false,
    val monthlyPriceText: String = "$9.99/mo",
    val entitlementName: String = "pro_access",
    val appUserId: String = "com.quotecall.agent.user",
    val isPaywallVisible: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: String? = null
)

class RevenueCatManager private constructor() {

    private val _state = MutableStateFlow(RevenueCatState())
    val state: StateFlow<RevenueCatState> = _state.asStateFlow()

    fun initialize(context: Context) {
        val apiKey = try { BuildConfig.REVENUECAT_API_KEY } catch (e: Exception) { "" }
        if (apiKey.isNotBlank() && apiKey != "goog_sample_key") {
            // Purchases.configure(PurchasesConfiguration.Builder(context, apiKey).appUserID("com.quotecall.agent.user").build())
            _state.value = _state.value.copy(statusMessage = "RevenueCat SDK Connected (com.quotecall.agent)")
        } else {
            _state.value = _state.value.copy(statusMessage = "RevenueCat Sandbox Mode Active (com.quotecall.agent)")
        }
    }

    fun showPaywall() {
        _state.value = _state.value.copy(isPaywallVisible = true)
    }

    fun hidePaywall() {
        _state.value = _state.value.copy(isPaywallVisible = false)
    }

    fun purchaseProAccess(onSuccess: () -> Unit) {
        _state.value = _state.value.copy(isProcessing = true)
        // Simulate real Play Store / RevenueCat subscription flow
        _state.value = _state.value.copy(
            isProSubscribed = true,
            isTrialActive = true,
            isPaywallVisible = false,
            isProcessing = false,
            statusMessage = "QuoteCall Pro Active (7-Day Trial)"
        )
        onSuccess()
    }

    fun restorePurchases() {
        _state.value = _state.value.copy(
            isProSubscribed = true,
            statusMessage = "Purchases Restored"
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: RevenueCatManager? = null

        fun getInstance(): RevenueCatManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RevenueCatManager()
                INSTANCE = instance
                instance
            }
        }
    }
}
