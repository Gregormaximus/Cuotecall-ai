package com.kuote.agent.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Job entity representing field service jobs tracked in QuoteBit.
 * Handles full 2-step Stripe Connect lifecycle:
 * 1. Upfront Deposit ($50 with 4.0% platform fee & off_session card tokenization)
 * 2. Final Balance Settlement (1.5% fee with 5 payment methods: Saved Card, Dynamic QR/Link, Tap to Pay NFC, Manual Keyed, External)
 */
@Entity(tableName = "jobs")
data class Job(
    @PrimaryKey val id: String,
    val customerName: String,
    val customerPhone: String,
    val customerLocation: String,
    val serviceTitle: String,
    val serviceCategory: String, // "ELECTRICAL", "PLUMBING", "TOWING", "LOCKSMITH", etc.
    val status: String = JobStatus.DEPOSIT_PAID,
    val estimatedTotal: Double,
    val depositAmount: Double = 50.00,
    val depositPlatformFee: Double = depositAmount * 0.04, // 4.0% platform fee on deposit
    val depositFeeRate: Double = 0.04,
    val depositPaymentIntentId: String? = "pi_dep_3M98172648172",
    val savedPaymentMethodId: String? = "pm_card_visa_tok99812", // Tokenized card via setup_future_usage = 'off_session'
    val balanceDue: Double = (estimatedTotal - depositAmount).coerceAtLeast(0.0),
    val balancePlatformFee: Double = (estimatedTotal - depositAmount).coerceAtLeast(0.0) * 0.015, // 1.5% fee on final balance
    val balanceFeeRate: Double = 0.015,
    val balancePaymentIntentId: String? = null,
    val finalSettlementMethod: String? = null, // "SAVED_CARD", "DYNAMIC_QR_LINK", "TAP_TO_PAY_NFC", "MANUAL_KEYED_CARD", "EXTERNAL_CASH_VENMO_CHECK"
    val externalPaymentType: String? = null, // "Cash", "Zelle", "Venmo", "Check", "External POS"
    val receiptUrl: String? = null,
    val timestampMillis: Long = System.currentTimeMillis(),
    val scheduledTime: String? = "Today @ 2:30 PM",
    val googleCalendarEventId: String? = "gcal_evt_991827361",
    val calendarSyncStatus: String = "SYNCED",
    val notes: String? = "Customer reported main circuit breaker tripping repeatedly.",
    val gpsLocation: String? = "37.77492, -122.41942",
    val lat: Double? = 37.77492,
    val lng: Double? = -122.41942
)

object JobStatus {
    const val PENDING_DEPOSIT = "PENDING_DEPOSIT"
    const val DEPOSIT_PAID = "DEPOSIT_PAID"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val COMPLETED_PAID_STRIPE = "COMPLETED_PAID_STRIPE"
    const val COMPLETED_PAID_EXTERNALLY = "COMPLETED_PAID_EXTERNALLY"
    const val CANCELLED = "CANCELLED"
}

object SettlementMethod {
    const val SAVED_CARD = "SAVED_CARD"
    const val DYNAMIC_QR_LINK = "DYNAMIC_QR_LINK"
    const val TAP_TO_PAY_NFC = "TAP_TO_PAY_NFC"
    const val MANUAL_KEYED_CARD = "MANUAL_KEYED_CARD"
    const val EXTERNAL_CASH_VENMO_CHECK = "EXTERNAL_CASH_VENMO_CHECK"
}
