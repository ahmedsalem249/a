package com.asdev.aclrenew

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class Account(
    val id: String,
    var name: String,
    var email: String,
    var serviceId: String,
    var status: String = "ACTIVE",
    var expiry: Long? = null,
    var lastCheck: Long? = null,
    var lastError: String? = null
)

class MainActivity : Activity() {

    private val accounts = mutableListOf<Account>()
    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("aclrenew", MODE_PRIVATE) }

    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var title: TextView
    private lateinit var stats: LinearLayout

    private val BG = Color.rgb(10, 14, 22)
    private val CARD = Color.rgb(20, 27, 39)
    private val CARD2 = Color.rgb(27, 35, 50)
    private val BLUE = Color.rgb(65, 145, 255)
    private val GREEN = Color.rgb(45, 190, 110)
    private val ORANGE = Color.rgb(245, 165, 55)
    private val RED = Color.rgb(235, 75, 85)
    private val WHITE = Color.WHITE
    private val MUTED = Color.rgb(155, 166, 185)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadAccounts()
        buildShell()
        showScreen("dashboard")
    }

    private fun buildShell() {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
        }

        val header = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(20.dp(), 18.dp(), 20.dp(), 12.dp())
        }

        title = TextView(this).apply {
            text = "ACLClouds Auto Renew"
            textSize = 20f
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, WRAP, 1f)
        }

        val add = textButton("+ Account", BLUE, WHITE).apply {
            setOnClickListener { editAccount(null) }
        }

        header.addView(title)
        header.addView(add)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16.dp(), 4.dp(), 16.dp(), 10.dp())
        }

        val scroll = ScrollView(this).apply {
            addView(content, ViewGroup.LayoutParams(MATCH, WRAP))
        }

        val nav = LinearLayout(this).apply {
