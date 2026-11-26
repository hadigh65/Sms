package com.example.smsforwarder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.provider.Telephony
import org.json.JSONArray

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages.firstOrNull()?.originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        if (body.isBlank()) return

        val prefs = context.getSharedPreferences("forward_settings", Context.MODE_PRIVATE)
        val rules = try { JSONArray(prefs.getString("rules", "[]")) } catch (_: Exception) { JSONArray() }
        var destination = ""
        for (i in 0 until rules.length()) {
            val rule = rules.optJSONObject(i) ?: continue
            val phrase = rule.optString("phrase")
            val target = rule.optString("destination")
            if (phrase.isNotBlank() && target.isNotBlank() && body.contains(phrase, ignoreCase = true)) {
                destination = target
                break
            }
        }
        if (destination.isBlank()) return
        try {
            val fullText = "فرستنده: $sender\nمتن پیامک:\n$body"
            val manager = if (android.os.Build.VERSION.SDK_INT >= 31) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            manager.sendMultipartTextMessage(destination, null, manager.divideMessage(fullText), null, null)
        } catch (_: Exception) {
            // Keep the received message untouched if forwarding fails.
        }
    }
}
