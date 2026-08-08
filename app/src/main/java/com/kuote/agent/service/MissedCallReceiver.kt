package com.kuote.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CallLog
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.kuote.agent.ai.MultimodalIntakeEngine
import com.kuote.agent.data.repository.KuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MissedCallReceiver : BroadcastReceiver() {

    companion object {
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var lastIncomingNumber = ""
        private var isIncomingRinging = false
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER) ?: ""
            android.util.Log.d("MissedCallReceiver", "Phone state changed: $stateStr, number: $incomingNumber")

            when (stateStr) {
                TelephonyManager.EXTRA_STATE_RINGING -> {
                    isIncomingRinging = true
                    if (incomingNumber.isNotBlank()) lastIncomingNumber = incomingNumber
                }
                TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                    // Call was answered
                    isIncomingRinging = false
                }
                TelephonyManager.EXTRA_STATE_IDLE -> {
                    // Call ended. If it was ringing and never went offhook -> MISSED/REJECTED CALL!
                    android.util.Log.d("MissedCallReceiver", "IDLE: isIncomingRinging: $isIncomingRinging, lastIncomingNumber: $lastIncomingNumber")
                    if (isIncomingRinging) {
                        isIncomingRinging = false
                        val missedNumber = when {
                            lastIncomingNumber.isNotBlank() -> lastIncomingNumber
                            incomingNumber.isNotBlank() -> incomingNumber
                            else -> ""
                        }

                        android.util.Log.d("MissedCallReceiver", "Triggering async missed call handler for: '$missedNumber'")
                        processMissedCall(context.applicationContext, missedNumber)
                    }
                }
            }
            lastState = stateStr ?: TelephonyManager.EXTRA_STATE_IDLE
        }
    }

    private fun processMissedCall(context: Context, initialPhoneNumber: String) {
        val repository = KuoteRepository(context)
        val notifHelper = NotificationHelper(context)
        val intakeEngine = MultimodalIntakeEngine()
        com.kuote.agent.util.ContactCache.init(context)

        CoroutineScope(Dispatchers.IO).launch {
            val company = repository.getCompanyProfileDirect()
            if (!company.isAgentActive) {
                android.util.Log.d("MissedCallReceiver", "Agent is inactive in settings. Skipping auto-reply.")
                return@launch
            }

            var targetNumber = initialPhoneNumber

            // Polling CallLog if initialPhoneNumber was withheld/empty by Android OS
            if (targetNumber.isBlank()) {
                val delays = listOf(500L, 1000L, 1500L, 2000L)
                for (d in delays) {
                    delay(d)
                    val logNumber = fetchRecentCallLogNumber(context)
                    if (logNumber.isNotBlank()) {
                        targetNumber = logNumber
                        android.util.Log.d("MissedCallReceiver", "Retrieved real caller number from CallLog: $targetNumber")
                        break
                    }
                }
            }

            val cleanNumber = targetNumber.replace(Regex("[^0-9+]"), "")

            if (cleanNumber.length < 6) {
                android.util.Log.w("MissedCallReceiver", "Unable to determine caller number from intent or CallLog. Skipping auto-reply.")
                return@launch
            }

            // Filter out personal contacts saved in address book!
            if (com.kuote.agent.util.ContactCache.isContact(cleanNumber)) {
                android.util.Log.d("MissedCallReceiver", "Number $cleanNumber is in Contacts. Skipping AI auto-reply.")
                return@launch
            }

            val services = repository.getServicesDirect()
            val rawSlug = company.name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val slug = if (rawSlug.isBlank()) "my-business" else rawSlug
            val smsMessage = "${company.autoSmsTemplate} https://quotebit.app/?slug=$slug"

            android.util.Log.d("MissedCallReceiver", "Sending instant auto-SMS to $cleanNumber")

            // 1. Generate AI Quote for missed call
            val quote = intakeEngine.analyzeCustomerRequest(
                customerPhone = cleanNumber,
                location = "Redwood City",
                textInput = "Incoming missed call response requested for $cleanNumber",
                companyProfile = company,
                services = services
            )

            // 2. Send Instant Auto-SMS linked to quote ID
            notifHelper.sendInstantSms(
                phoneNumber = cleanNumber,
                messageText = smsMessage,
                triggerType = "MISSED_CALL_AUTO",
                relatedQuoteId = quote.id
            )

            // 3. Save Quote to Room & Trigger Notification
            repository.saveQuote(quote)
            notifHelper.showQuoteNotification(quote, company.name)
        }
    }

    private fun fetchRecentCallLogNumber(context: Context): String {
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            try {
                // Query calls made in the last 60 seconds
                val minTime = System.currentTimeMillis() - 60_000
                val cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DATE),
                    "${CallLog.Calls.DATE} >= ?",
                    arrayOf(minTime.toString()),
                    "${CallLog.Calls.DATE} DESC"
                )
                cursor?.use {
                    if (it.moveToFirst()) {
                        val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                        if (numberIndex >= 0) {
                            val num = it.getString(numberIndex)
                            if (!num.isNullOrBlank()) return num
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MissedCallReceiver", "Failed to query recent CallLog", e)
            }
        }
        return ""
    }
}
