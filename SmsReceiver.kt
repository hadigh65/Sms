package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.provider.Telephony

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = context.getSharedPreferences("forward_settings", Context.MODE_PRIVATE)
        val destination = prefs.getString("destination", "").orEmpty()
        val phrase = prefs.getString("phrase", "").orEmpty()
        if (destination.isBlank() || phrase.isBlank()) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (!body.contains(phrase, ignoreCase = true)) return

        try {
            val fullText = "فرستنده: $sender\nمتن پیامک:\n$body"
            val manager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            val parts = manager.divideMessage(fullText)
            manager.sendMultipartTextMessage(destination, null, parts, null, null)
        } catch (_: Exception) {
            // پیام اصلی دست‌نخورده باقی می‌ماند؛ ارسال ناموفق ممکن است به محدودیت دستگاه یا اپراتور مربوط باشد.
        }
    }
}
