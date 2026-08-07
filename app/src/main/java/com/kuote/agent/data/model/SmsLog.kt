package com.kuote.agent.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sms_logs")
data class SmsLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val recipientPhone: String,
    val messageText: String,
    val timestampMillis: Long = System.currentTimeMillis(),
    val status: String = "DELIVERED", // "DELIVERED", "PENDING", "FAILED", "SKIPPED"
    val triggerType: String = "MISSED_CALL_AUTO", // "MISSED_CALL_AUTO", "TEST_SMS", "MANUAL"
    val relatedQuoteId: String? = null,
    val errorDetails: String? = null
)
