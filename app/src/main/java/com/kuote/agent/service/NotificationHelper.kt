package com.kuote.agent.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.kuote.agent.MainActivity
import com.kuote.agent.R
import com.kuote.agent.data.model.Quote

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "kuote_quotes_channel"
        const val CHANNEL_NAME = "Kuote Instant Quotes"
        const val NOTIF_ID = 1001
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new missed-call AI estimates and deposit links"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showQuoteNotification(quote: Quote, companyName: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New Quote: $${quote.estimatedTotal} - $companyName")
            .setContentText("${quote.customerPhone}: ${quote.aiSummary}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${quote.customerPhone}: ${quote.aiSummary}\nDeposit Required: $${quote.requiredDeposit}")
            )

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(quote.id.hashCode(), builder.build())
    }

    fun sendInstantSms(phoneNumber: String, messageText: String) {
        try {
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(phoneNumber, null, messageText, null, null)
            showToast("Instant SMS sent to $phoneNumber")
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("SMS Simulated to $phoneNumber")
        }
    }

    private fun showToast(msg: String) {
        try {
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
