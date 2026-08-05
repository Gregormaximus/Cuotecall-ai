package com.kuote.agent.data.repository

import android.content.Context
import com.kuote.agent.data.model.Job
import com.kuote.agent.data.model.JobStatus
import com.kuote.agent.data.model.SettlementMethod
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Stripe Connect Repository executing 2-Step Payment Lifecycle for Field Service Contractors:
 *
 * STEP 1: Upfront Deposit ($50.00)
 * - Platform Fee: 4.0% ($2.00 on $50) via `application_fee_amount`
 * - Card Tokenization: Pass `setup_future_usage = 'off_session'` to store payment_method_id securely for final balance charges.
 *
 * STEP 2: Job Close & Final Balance Settlement
 * 1. Saved Card (Stripe Connect): Off-session charge on saved card, 1.5% incentive platform fee.
 * 2. Dynamic QR Code / SMS Payment Link: Stripe Checkout link & QR payload.
 * 3. Tap to Pay / NFC Contactless: Stripe Terminal NFC flow.
 * 4. Manual Card Entry: Keyed-in card PaymentIntent.
 * 5. Mark Paid Externally: Cash, Zelle, Venmo, Check, External POS + digital PDF/SMS receipt generation.
 */
class StripeRepository(private val context: Context) {

    data class StripeDepositResult(
        val success: Boolean,
        val paymentIntentId: String?,
        val paymentMethodId: String?,
        val message: String
    )

    data class StripeSettlementResult(
        val success: Boolean,
        val paymentIntentId: String?,
        val receiptUrl: String?,
        val status: String,
        val message: String
    )

    /**
     * Step 1: Upfront Deposit Capture (4.0% Platform Fee & Off-Session Tokenization)
     */
    suspend fun createUpfrontDepositIntent(
        jobId: String,
        amount: Double = 50.00,
        stripeAccountId: String
    ): StripeDepositResult = withContext(Dispatchers.IO) {
        delay(800) // Simulate Stripe API latency
        val platformFee = amount * 0.04 // 4.0% Fee
        val piId = "pi_dep_${System.currentTimeMillis().toString().takeLast(8)}"
        val pmId = "pm_card_saved_${System.currentTimeMillis().toString().takeLast(6)}"

        StripeDepositResult(
            success = true,
            paymentIntentId = piId,
            paymentMethodId = pmId,
            message = "Deposit of $$amount captured ($${String.format("%.2f", platformFee)} platform fee). Card saved off-session for balance settlement."
        )
    }

    /**
     * Settlement Method 1: Charge Saved Card (Off-Session via Stripe Connect)
     * Discounted 1.5% Platform Fee
     */
    suspend fun settleWithSavedCard(
        job: Job,
        stripeAccountId: String
    ): StripeSettlementResult = withContext(Dispatchers.IO) {
        delay(1000)
        val pmId = job.savedPaymentMethodId ?: "pm_card_saved_default"
        val balance = job.balanceDue
        val platformFee = balance * 0.015 // 1.5% Fee
        val piId = "pi_bal_${System.currentTimeMillis().toString().takeLast(8)}"
        val receipt = "https://pay.stripe.com/receipts/quotecall_$piId"

        StripeSettlementResult(
            success = true,
            paymentIntentId = piId,
            receiptUrl = receipt,
            status = JobStatus.COMPLETED_PAID_STRIPE,
            message = "Charged saved card ($pmId) $$balance with 1.5% fee ($${String.format("%.2f", platformFee)})."
        )
    }

    /**
     * Settlement Method 2: Dynamic QR Code / SMS Payment Link Generator
     */
    suspend fun generateDynamicPaymentLink(
        job: Job,
        stripeAccountId: String
    ): String = withContext(Dispatchers.IO) {
        val slug = job.id.takeLast(6)
        "https://quotecall.ai/pay/checkout_$slug?amount=${job.balanceDue}&fee=1.5"
    }

    /**
     * Settlement Method 3: Tap to Pay / NFC Contactless Flow
     */
    suspend fun initiateTapToPayNfc(
        job: Job,
        stripeAccountId: String
    ): StripeSettlementResult = withContext(Dispatchers.IO) {
        delay(1200) // Simulate NFC contact & card read
        val piId = "pi_nfc_${System.currentTimeMillis().toString().takeLast(8)}"
        val receipt = "https://pay.stripe.com/receipts/quotecall_$piId"

        StripeSettlementResult(
            success = true,
            paymentIntentId = piId,
            receiptUrl = receipt,
            status = JobStatus.COMPLETED_PAID_STRIPE,
            message = "NFC Tap to Pay successful ($${job.balanceDue})."
        )
    }

    /**
     * Settlement Method 4: Manual Keyed-In Card Entry
     */
    suspend fun processManualKeyedCard(
        job: Job,
        cardNumber: String,
        expMonth: Int,
        expYear: Int,
        cvc: String,
        stripeAccountId: String
    ): StripeSettlementResult = withContext(Dispatchers.IO) {
        delay(1100)
        val piId = "pi_keyed_${System.currentTimeMillis().toString().takeLast(8)}"
        val receipt = "https://pay.stripe.com/receipts/quotecall_$piId"

        StripeSettlementResult(
            success = true,
            paymentIntentId = piId,
            receiptUrl = receipt,
            status = JobStatus.COMPLETED_PAID_STRIPE,
            message = "Manual card charge processed successfully."
        )
    }

    /**
     * Settlement Method 5: Mark Paid Externally (Cash, Zelle, Venmo, Check, External POS)
     * Bypasses Stripe charge processing, generates digital receipt info.
     */
    suspend fun settlePaidExternally(
        job: Job,
        externalMethod: String
    ): StripeSettlementResult = withContext(Dispatchers.IO) {
        delay(500)
        val receipt = "https://quotecall.ai/receipts/ext_${job.id}"

        StripeSettlementResult(
            success = true,
            paymentIntentId = null,
            receiptUrl = receipt,
            status = JobStatus.COMPLETED_PAID_EXTERNALLY,
            message = "Job marked paid via $externalMethod ($${job.balanceDue}). Receipt generated."
        )
    }
}
