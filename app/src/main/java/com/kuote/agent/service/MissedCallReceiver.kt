package com.kuote.agent.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.kuote.agent.ai.MultimodalIntakeEngine
import com.kuote.agent.data.repository.KuoteRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
                    // Call ended. If it was ringing and never went offhook -> MISSED CALL!
                    if (isIncomingRinging) {
                        isIncomingRinging = false
                        val missedNumber = if (lastIncomingNumber.isNotBlank()) lastIncomingNumber else "+1 (555) 012-3456"
                        processMissedCall(context, missedNumber)
                    }
                }
            }
            lastState = stateStr ?: TelephonyManager.EXTRA_STATE_IDLE
        }
    }

    private fun processMissedCall(context: Context, phoneNumber: String) {
        val repository = KuoteRepository(context.applicationContext)
        val notifHelper = NotificationHelper(context.applicationContext)
        val intakeEngine = MultimodalIntakeEngine()

        CoroutineScope(Dispatchers.IO).launch {
            val company = repository.getCompanyProfileDirect()
            if (!company.isAgentActive) return@launch

            // Filter out personal contacts saved in phonebook!
            if (com.kuote.agent.util.ContactCache.isContact(phoneNumber)) {
                android.util.Log.d("MissedCallReceiver", "Number $phoneNumber is in Contacts. Skipping AI auto-reply.")
                return@launch
            }

            val services = repository.getServicesDirect()
            val webConfig = repository.webConfigFlow

            val rawSlug = company.name.lowercase().trim().replace(Regex("[^a-z0-9]+"), "-").trim('-')
            val slug = if (rawSlug.isBlank()) "apex-electric-pros" else rawSlug
            val smsMessage = "${company.autoSmsTemplate} https://ais-dev-evjq6zhfgibldhq4rn7mw2-55455507008.us-west2.run.app/?slug=$slug"
            
            // 1. Send Instant Auto-SMS
            notifHelper.sendInstantSms(phoneNumber, smsMessage)

            // 2. Generate AI Quote for missed call
            val quote = intakeEngine.analyzeCustomerRequest(
                customerPhone = phoneNumber,
                location = "Redwood City",
                textInput = "Incoming missed call response requested for $phoneNumber",
                companyProfile = company,
                services = services
            )

            // 3. Save Quote to Room / Firestore & Trigger Notification
            repository.saveQuote(quote)
            notifHelper.showQuoteNotification(quote, company.name)
        }
    }
}
