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

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("forward_settings", Context.MODE_PRIVATE) }
    private lateinit var destination: EditText
    private lateinit var phrase: EditText
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        destination.setText(prefs.getString("destination", ""))
        phrase.setText(prefs.getString("phrase", ""))
        updateStatus()
        requestSmsPermissions()
    }

    private fun buildScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 38, 28, 24)
            gravity = Gravity.TOP
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        val title = TextView(this).apply {
            text = "انتقال خودکار پیامک"
            textSize = 23f
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        layout.addView(title, params())
        layout.addView(label("شماره مقصد"))
        destination = EditText(this).apply {
            hint = "مثلاً 09xxxxxxxxx"
            inputType = InputType.TYPE_CLASS_PHONE
            textSize = 17f
        }
        layout.addView(destination, params())
        layout.addView(label("عبارت معرفی‌شده در متن پیامک"))
        phrase = EditText(this).apply {
            hint = "عبارت یا بخشی از متن پیامک را وارد کنید"
            textSize = 17f
            minLines = 2
            gravity = Gravity.TOP or Gravity.RIGHT
        }
        layout.addView(phrase, params())
        val save = Button(this).apply {
            text = "ذخیره و فعال‌سازی"
            setOnClickListener { saveSettings() }
        }
        layout.addView(save, params())
        status = TextView(this).apply {
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 12)
        }
        layout.addView(status, params())
        val note = TextView(this).apply {
            text = "فقط پیامک‌هایی منتقل می‌شوند که متن آن‌ها شامل عبارت ثبت‌شده باشد. متن کامل پیامک همراه شماره فرستنده ارسال می‌شود."
            textSize = 14f
            gravity = Gravity.RIGHT
        }
        layout.addView(note, params())
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    private fun label(value: String) = TextView(this).apply {
        text = value
        textSize = 16f
        setPadding(0, 20, 0, 4)
        setTypeface(null, Typeface.BOLD)
    }

    private fun params() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).apply { bottomMargin = 8 }

    private fun saveSettings() {
        val number = destination.text.toString().trim()
        val key = phrase.text.toString().trim()
        if (number.isBlank() || key.isBlank()) {
            Toast.makeText(this, "شماره مقصد و عبارت پیامک را وارد کنید", Toast.LENGTH_LONG).show()
            return
        }
        if (number.length < 8) {
            Toast.makeText(this, "شماره مقصد معتبر نیست", Toast.LENGTH_LONG).show()
            return
        }
        prefs.edit().putString("destination", number).putString("phrase", key).apply()
        updateStatus()
        Toast.makeText(this, "تنظیمات ذخیره شد", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val active = prefs.getString("destination", "").orEmpty().isNotBlank() &&
            prefs.getString("phrase", "").orEmpty().isNotBlank()
        status.text = if (active) "وضعیت: آماده انتقال پیامک‌های مطابق عبارت"
            else "وضعیت: ابتدا شماره و عبارت را ثبت کنید"
    }

    private fun requestSmsPermissions() {
        val needed = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS)
            .filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (needed.isNotEmpty()) requestPermissions(needed.toTypedArray(), 100)
    }
}
