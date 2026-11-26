package com.example.smsforwarder

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.pm.PackageManager
import android.view.Gravity
import android.view.View
import android.widget.*
import android.graphics.Typeface
import android.text.InputType
import org.json.JSONArray
import org.json.JSONObject

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("forward_settings", Context.MODE_PRIVATE) }
    private lateinit var destination: EditText
    private lateinit var phrase: EditText
    private lateinit var status: TextView
    private lateinit var rulesContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        migrateOldRule()
        buildScreen()
        renderRules()
        updateStatus()
        requestSmsPermissions()
    }

    private fun buildScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 30, 28, 24)
            gravity = Gravity.TOP
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        layout.addView(TextView(this).apply {
            text = "انتقال خودکار پیامک"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }, params())
        layout.addView(label("عبارت یا نمونه‌ای از متن پیامک"))
        phrase = EditText(this).apply {
            hint = "مثلاً کد تأیید یا بخشی از متن پیامک"
            textSize = 16f
            minLines = 2
            gravity = Gravity.TOP or Gravity.RIGHT
        }
        layout.addView(phrase, params())
        layout.addView(label("شماره مقصد این پیامک"))
        destination = EditText(this).apply {
            hint = "مثلاً 09xxxxxxxxx"
            inputType = InputType.TYPE_CLASS_PHONE
            textSize = 17f
        }
        layout.addView(destination, params())
        layout.addView(Button(this).apply {
            text = "معرفی و ذخیره پیامک"
            setOnClickListener { addRule() }
        }, params())
        status = TextView(this).apply {
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, 18, 0, 12)
        }
        layout.addView(status, params())
        layout.addView(label("فهرست پیامک‌های معرفی‌شده"))
        rulesContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        layout.addView(rulesContainer, params())
        layout.addView(TextView(this).apply {
            text = "هر پیامک دریافتی که شامل یکی از عبارت‌های ثبت‌شده باشد، متن کامل آن همراه شماره فرستنده به مقصد همان مورد ارسال می‌شود. در صورت تطبیق با چند مورد، فقط اولین مورد در فهرست انتخاب می‌شود."
            textSize = 13f
            setPadding(0, 18, 0, 8)
            gravity = Gravity.RIGHT
        }, params())
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 16f
        setPadding(0, 18, 0, 5)
        setTypeface(null, Typeface.BOLD)
    }

    private fun params() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    private fun getRules(): JSONArray = try {
        JSONArray(prefs.getString("rules", "[]"))
    } catch (_: Exception) { JSONArray() }

    private fun saveRules(rules: JSONArray) {
        prefs.edit().putString("rules", rules.toString()).apply()
    }

    private fun migrateOldRule() {
        if (prefs.contains("rules")) return
        val oldPhrase = prefs.getString("phrase", "").orEmpty()
        val oldDestination = prefs.getString("destination", "").orEmpty()
        if (oldPhrase.isNotBlank() && oldDestination.isNotBlank()) {
            val rules = JSONArray()
            rules.put(JSONObject().put("phrase", oldPhrase).put("destination", oldDestination))
            saveRules(rules)
        }
    }

    private fun addRule() {
        val key = phrase.text.toString().trim()
        val number = destination.text.toString().trim()
        if (key.isBlank() || number.isBlank()) {
            Toast.makeText(this, "نمونه پیامک و شماره مقصد را وارد کنید", Toast.LENGTH_LONG).show()
            return
        }
        if (number.length < 8) {
            Toast.makeText(this, "شماره مقصد معتبر نیست", Toast.LENGTH_LONG).show()
            return
        }
        val rules = getRules()
        for (i in 0 until rules.length()) {
            val item = rules.optJSONObject(i) ?: continue
            if (item.optString("phrase").equals(key, true)) {
                Toast.makeText(this, "این عبارت قبلاً ثبت شده است؛ ابتدا مورد قبلی را حذف کنید", Toast.LENGTH_LONG).show()
                return
            }
        }
        rules.put(JSONObject().put("phrase", key).put("destination", number))
        saveRules(rules)
        phrase.text.clear()
        destination.text.clear()
        renderRules()
        updateStatus()
        Toast.makeText(this, "نمونه پیامک ذخیره شد", Toast.LENGTH_SHORT).show()
    }

    private fun deleteRule(index: Int) {
        val old = getRules()
        val updated = JSONArray()
        for (i in 0 until old.length()) if (i != index) updated.put(old.getJSONObject(i))
        saveRules(updated)
        renderRules()
        updateStatus()
    }

    private fun renderRules() {
        rulesContainer.removeAllViews()
        val rules = getRules()
        if (rules.length() == 0) {
            rulesContainer.addView(TextView(this).apply {
                text = "هنوز نمونه‌ای ذخیره نشده است."
                textSize = 14f
                setPadding(4, 12, 4, 12)
            })
            return
        }
        for (i in 0 until rules.length()) {
            val item = rules.optJSONObject(i) ?: continue
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(10, 8, 10, 8)
                layoutDirection = View.LAYOUT_DIRECTION_RTL
                setBackgroundResource(android.R.drawable.dialog_holo_light_frame)
            }
            val details = TextView(this).apply {
                text = "${i + 1}. ${item.optString("phrase")}\nمقصد: ${item.optString("destination")}"
                textSize = 14f
                setPadding(4, 4, 4, 4)
            }
            row.addView(details, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            row.addView(Button(this).apply {
                text = "حذف"
                setOnClickListener { deleteRule(i) }
            })
            val lp = params()
            lp.bottomMargin = 10
            rulesContainer.addView(row, lp)
        }
    }

    private fun updateStatus() {
        val count = getRules().length()
        status.text = if (count > 0) "وضعیت: فعال — $count نمونه پیامک ذخیره شده"
            else "وضعیت: هنوز نمونه پیامکی ثبت نشده است"
    }

    private fun requestSmsPermissions() {
        val needed = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 100)
    }
}
