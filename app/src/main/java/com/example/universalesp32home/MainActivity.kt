package com.example.universalesp32home

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var deviceName: EditText
    private lateinit var deviceIp: EditText
    private lateinit var list: LinearLayout
    private val labels = arrayOf("Light 1","Light 2","Fan","AC","Oven","Garage Lights","TV","Switch 8")
    private val states = BooleanArray(8)
    private val prefs by lazy { getSharedPreferences("esp32home", Context.MODE_PRIVATE) }
    private val baseUrl get() = "http://${deviceIp.text.toString().trim().ifBlank { "192.168.4.1" }}"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        for (i in labels.indices) labels[i] = prefs.getString("label_$i", labels[i]) ?: labels[i]
        buildUi()
        refreshStatus()
    }

    private fun buildUi() {
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 26, 22, 24)
            setBackgroundColor(0xFFF4F6FA.toInt())
        }
        scroll.addView(root)
        setContentView(scroll)

        root.addView(TextView(this).apply {
            text = "ESP32 HOME"; textSize = 25f; setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF17233B.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Universal Wi-Fi controller • 8 channels"; textSize = 14f
            setTextColor(0xFF65718A.toInt())
        })
        val wifi = Button(this).apply {
            text = "  Open Wi-Fi settings  "
            setOnClickListener { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
        }
        root.addView(wifi)
        status = TextView(this).apply {
            text = "● Not connected — connect your phone to the ESP32 access point"
            textSize = 14f; setPadding(0, 12, 0, 12)
        }
        root.addView(status)

        deviceName = EditText(this).apply {
            hint = "Device name (e.g. Home ESP32)"
            setText(prefs.getString("device_name", "Home ESP32"))
            singleLine = true
        }
        root.addView(deviceName)
        deviceIp = EditText(this).apply {
            hint = "ESP32 IP address"
            setText(prefs.getString("ip", "192.168.4.1"))
            singleLine = true
        }
        root.addView(deviceIp)
        root.addView(Button(this).apply {
            text = "Save device / Check connection"
            setOnClickListener {
                prefs.edit().putString("device_name", deviceName.text.toString())
                    .putString("ip", deviceIp.text.toString()).apply()
                refreshStatus()
            }
        })
        root.addView(TextView(this).apply {
            text = "CONTROLS"; textSize = 18f; setTypeface(null, Typeface.BOLD)
            setTextColor(0xFF17233B.toInt()); setPadding(0, 18, 0, 8)
        })
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(list)
        renderSwitches()
        root.addView(Button(this).apply {
            text = "Refresh status"
            setOnClickListener { refreshStatus() }
        })
        root.addView(TextView(this).apply {
            text = "Connect to the ESP32_HOME Wi-Fi in Android Settings first. Default IP: 192.168.4.1"
            textSize = 12f; setTextColor(0xFF65718A.toInt()); setPadding(0, 16, 0, 0)
        })
    }

    private fun renderSwitches() {
        list.removeAllViews()
        for (i in 0..7) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(12, 7, 8, 7)
                setBackgroundColor(0xFFFFFFFF.toInt())
            }
            val name = TextView(this).apply {
                text = labels[i]; textSize = 16f; setTextColor(0xFF17233B.toInt())
                layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
                setOnLongClickListener {
                    val input = EditText(this@MainActivity).apply { setText(labels[i]) }
                    android.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Rename switch ${i+1}").setView(input)
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Save") { _, _ ->
                            labels[i] = input.text.toString().ifBlank { "Switch ${i+1}" }
                            prefs.edit().putString("label_$i", labels[i]).apply()
                            renderSwitches()
                        }.show()
                    true
                }
            }
            val toggle = Switch(this).apply {
                isChecked = states[i]
                text = if (states[i]) "ON" else "OFF"
                setOnCheckedChangeListener { _, checked ->
                    text = if (checked) "ON" else "OFF"
                    sendSwitch(i + 1, checked)
                }
            }
            row.addView(name)
            row.addView(toggle)
            row.setOnLongClickListener { name.performLongClick() }
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 7 })
        }
    }

    private fun refreshStatus() {
        executor.execute {
            try {
                val conn = (URL("$baseUrl/api/status").openConnection() as HttpURLConnection)
                conn.connectTimeout = 2500; conn.readTimeout = 2500
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val json = JSONObject(body)
                val arr = json.optJSONArray("switches")
                if (arr != null && arr.length() >= 8) {
                    for (i in 0..7) states[i] = arr.optBoolean(i, false)
                }
                runOnUiThread {
                    status.text = "● Connected to ${deviceName.text} (${deviceIp.text})"
                    status.setTextColor(0xFF16834A.toInt())
                    renderSwitches()
                }
            } catch (_: Exception) {
                runOnUiThread {
                    status.text = "● Offline — check Wi-Fi and ESP32 firmware"
                    status.setTextColor(0xFFC0392B.toInt())
                }
            }
        }
    }

    private fun sendSwitch(channel: Int, on: Boolean) {
        executor.execute {
            try {
                val conn = (URL("$baseUrl/api/switch/$channel").openConnection() as HttpURLConnection)
                conn.requestMethod = "POST"
                conn.doOutput = true
                conn.connectTimeout = 2500; conn.readTimeout = 2500
                conn.setRequestProperty("Content-Type", "application/json")
                conn.outputStream.use { it.write(JSONObject().put("state", on).toString().toByteArray()) }
                val code = conn.responseCode
                conn.disconnect()
                if (code !in 200..299) throw Exception("HTTP $code")
                refreshStatus()
            } catch (_: Exception) {
                runOnUiThread { Toast.makeText(this, "Command failed — status not confirmed", Toast.LENGTH_SHORT).show() }
                refreshStatus()
            }
        }
    }

    override fun onResume() { super.onResume(); if (::status.isInitialized) refreshStatus() }
}
