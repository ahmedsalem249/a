package com.asdev.aclrenew

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

data class Account(
    val id: String,
    val name: String,
    val email: String,
    val serviceId: String,
    val status: String,
    val expiry: Long?,
    val lastCheck: Long?,
    val lastError: String?
)

class MainActivity : android.app.Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()

    private lateinit var container: LinearLayout
    private lateinit var subtitle: TextView

    private val prefs by lazy {
        getSharedPreferences("acl", Context.MODE_PRIVATE)
    }

    private var accounts: List<Account> = emptyList()

    private val ticker = object : Runnable {
        override fun run() {
            redrawCountdownsOnly()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(buildUi())

        handler.post(ticker)
        loadAccounts()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun buildUi(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(245, 247, 251))
            setPadding(18.dp(), 18.dp(), 18.dp(), 18.dp())
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val title = TextView(this).apply {
            text = "ACLClouds Auto Renew"
            textSize = 22f
            setTextColor(Color.rgb(24, 32, 51))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        subtitle = TextView(this).apply {
            text = "5-account mobile dashboard"
            textSize = 13f
            setTextColor(Color.rgb(105, 115, 134))
        }

        titleBox.addView(title)
        titleBox.addView(subtitle)

        val refresh = Button(this).apply {
            text = "Refresh"
            setOnClickListener {
                loadAccounts()
            }
        }

        val settings = Button(this).apply {
            text = "Settings"
            setOnClickListener {
                showSettings()
            }
        }

        header.addView(titleBox)
        header.addView(refresh)
        header.addView(settings)

        root.addView(header)

        val scroll = ScrollView(this)

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 12.dp(), 0, 30.dp())
        }

        scroll.addView(container)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        return root
    }

    private fun showSettings() {

        val input = EditText(this).apply {
            hint = "https://your-backend.example.com"
            setText(
                prefs.getString(
                    "baseUrl",
                    "http://10.0.2.2:3000"
                )
            )
            selectAll()
        }

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp(), 4.dp(), 24.dp(), 0)
            addView(input)
        }

        AlertDialog.Builder(this)
            .setTitle("Backend URL")
            .setMessage(
                "The Android app talks to your secure backend. " +
                        "Keep ACLClouds passwords on the backend, not in the phone app."
            )
            .setView(box)
            .setPositiveButton("Save") { _, _ ->

                prefs.edit()
                    .putString(
                        "baseUrl",
                        input.text.toString().trim().trimEnd('/')
                    )
                    .apply()

                loadAccounts()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadAccounts() {

        renderMessage("Loading accounts...")

        val base = prefs.getString(
            "baseUrl",
            "http://10.0.2.2:3000"
        )!!.trimEnd('/')

        executor.execute {

            try {

                val json = httpGet("$base/api/accounts")
                val parsed = parseAccounts(json)

                runOnUiThread {

                    accounts = parsed

                    subtitle.text =
                        "${parsed.size} accounts • Backend connected"

                    renderAll()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    subtitle.text = "Backend connection failed"

                    renderMessage(
                        "Could not load accounts.\n${e.message}"
                    )
                }
            }
        }
    }

    private fun manualCheck(id: String) {

        val base = prefs.getString(
            "baseUrl",
            "http://10.0.2.2:3000"
        )!!.trimEnd('/')

        Toast.makeText(
            this,
            "Checking account $id...",
            Toast.LENGTH_SHORT
        ).show()

        executor.execute {

            try {

                httpPost("$base/api/accounts/$id/check")

                runOnUiThread {
                    loadAccounts()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    Toast.makeText(
                        this,
                        e.message ?: "Check failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun renderAll() {

        container.removeAllViews()

        if (accounts.isEmpty()) {

            renderMessage(
                "No ACLClouds accounts configured yet.\n" +
                        "Add ACL_1..ACL_5 in the backend .env file."
            )

            return
        }

        accounts.forEach { account ->
            container.addView(accountCard(account))
        }
    }

    private fun renderMessage(msg: String) {

        if (!::container.isInitialized) return

        container.removeAllViews()

        val t = TextView(this).apply {
            text = msg
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(
                16.dp(),
                24.dp(),
                16.dp(),
                24.dp()
            )
        }

        container.addView(t)
    }

    private fun accountCard(a: Account): View {

        val card = LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setBackgroundColor(Color.WHITE)

            setPadding(
                18.dp(),
                18.dp(),
                18.dp(),
                18.dp()
            )

            elevation = 4f
        }

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {

            setMargins(
                0,
                0,
                0,
                16.dp()
            )
        }

        card.layoutParams = lp

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val title = TextView(this).apply {

            text = a.name
            textSize = 19f

            setTextColor(
                Color.rgb(24, 32, 51)
            )

            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
        }

        head.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val status = TextView(this).apply {

            text = a.status
            textSize = 12f

            setPadding(
                10.dp(),
                6.dp(),
                10.dp(),
                6.dp()
            )

            setTextColor(
                if (a.status.contains("Error", true))
                    Color.rgb(180, 35, 24)
                else
                    Color.rgb(0, 120, 70)
            )

            setBackgroundColor(
                Color.rgb(238, 242, 247)
            )
        }

        head.addView(status)

        card.addView(head)

        card.addView(label(a.email))

        card.addView(
            label(
                "Service ID: ${
                    a.serviceId.ifBlank { "—" }
                }"
            )
        )

        val countdown = TextView(this).apply {

            tag = "countdown"

            text = expiryText(a.expiry)

            textSize = 27f

            setTextColor(
                Color.rgb(20, 45, 99)
            )

            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )

            setPadding(
                0,
                14.dp(),
                0,
                10.dp()
            )
        }

        card.addView(countdown)

        card.addView(
            label(
                "Expiry: ${formatDate(a.expiry)}"
            )
        )

        card.addView(
            label(
                "Last check: ${formatDate(a.lastCheck)}"
            )
        )

        if (!a.lastError.isNullOrBlank()) {

            val err = label(
                "⚠ ${a.lastError}"
            ).apply {

                setTextColor(
                    Color.rgb(180, 35, 24)
                )
            }

            card.addView(err)
        }

        val check = Button(this).apply {

            text = "Check now"

            setOnClickListener {
                manualCheck(a.id)
            }
        }

        card.addView(check)

        return card
    }

    private fun redrawCountdownsOnly() {

        if (!::container.isInitialized) return

        for (i in 0 until container.childCount) {

            val card =
                container.getChildAt(i) as? LinearLayout
                    ?: continue

            val countdown =
                card.findViewWithTag<TextView>("countdown")
                    ?: continue

            val a =
                accounts.getOrNull(i)
                    ?: continue

            countdown.text = expiryText(a.expiry)
        }
    }

    private fun expiryText(expiry: Long?): String {

        if (expiry == null) {
            return "⏱ —"
        }

        val diff =
            expiry - System.currentTimeMillis()

        if (diff <= 0) {
            return "⏱ Expired"
        }

        val totalSec = diff / 1000

        val days = totalSec / 86400

        val hrs =
            (totalSec % 86400) / 3600

        val min =
            (totalSec % 3600) / 60

        val sec =
            totalSec % 60

        return "⏱ ${days}d ${hrs}h ${min}m ${sec}s"
    }

    private fun formatDate(time: Long?): String {

        if (time == null) {
            return "—"
        }

        return try {

            val formatter =
                SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    Locale.getDefault()
                )

            formatter.format(Date(time))

        } catch (e: Exception) {

            "—"
        }
    }

    private fun parseAccounts(
        json: String
    ): List<Account> {

        val arr = JSONArray(json)

        val out =
            ArrayList<Account>()

        for (i in 0 until arr.length()) {

            val o =
                arr.getJSONObject(i)

            fun longOrNull(
                name: String
            ): Long? {

                val s =
                    o.optString(name, "")

                return s.toLongOrNull()
            }

            out.add(
                Account(

                    id = o.optString("id"),

                    name = o.optString("name"),

                    email = o.optString("email"),

                    serviceId =
                        o.optString("serviceId"),

                    status =
                        o.optString("status"),

                    expiry =
                        longOrNull("expiry"),

                    lastCheck =
                        longOrNull("lastCheck"),

                    lastError =
                        o.optString("lastError")
                            .ifBlank { null }
                )
            )
        }

        return out
    }

    private fun httpGet(
        urlString: String
    ): String {

        val conn =
            URL(urlString)
                .openConnection() as HttpURLConnection

        return try {

            conn.requestMethod = "GET"

            conn.connectTimeout = 20000
            conn.readTimeout = 20000

            readResponse(conn)

        } finally {

            conn.disconnect()
        }
    }

    private fun httpPost(
        urlString: String
    ): String {

        val conn =
            URL(urlString)
                .openConnection() as HttpURLConnection

        return try {

            conn.requestMethod = "POST"

            conn.connectTimeout = 20000
            conn.readTimeout = 20000

            conn.doOutput = true

            conn.outputStream.use {
                // Empty POST body
            }

            readResponse(conn)

        } finally {

            conn.disconnect()
        }
    }

    private fun readResponse(
        conn: HttpURLConnection
    ): String {

        val code = conn.responseCode

        val stream =
            if (code in 200..299)
                conn.inputStream
            else
                conn.errorStream

        val body =
            stream.bufferedReader().use {
                it.readText()
            }

        if (code !in 200..299) {
            error("HTTP $code: $body")
        }

        return body
    }

    private fun label(
        text: String
    ): TextView = TextView(this).apply {

        this.text = text

        textSize = 13f

        setTextColor(
            Color.rgb(105, 115, 134)
        )

        setPadding(
            0,
            5.dp(),
            0,
            0
        )
    }

    private fun Int.dp(): Int =
        (this * resources.displayMetrics.density).toInt()
}
