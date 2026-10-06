package com.asdev.aclrenew

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
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
    private lateinit var accountsCount: TextView
    private lateinit var activeCount: TextView
    private lateinit var expiringCount: TextView

    private val prefs by lazy {
        getSharedPreferences("acl", Context.MODE_PRIVATE)
    }

    private var accounts: List<Account> = emptyList()

    companion object {
        private const val BG = 0xFF0B1020.toInt()
        private const val CARD = 0xFF141B2E.toInt()
        private const val CARD_2 = 0xFF192238.toInt()
        private const val WHITE = 0xFFF5F7FF.toInt()
        private const val MUTED = 0xFF8F9BB3.toInt()
        private const val BLUE = 0xFF5B8CFF.toInt()
        private const val GREEN = 0xFF38D996.toInt()
        private const val ORANGE = 0xFFFFB454.toInt()
        private const val RED = 0xFFFF5C6C.toInt()
    }

    private val ticker = object : Runnable {
        override fun run() {
            redrawCountdownsOnly()
            updateStats()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = BG
        window.navigationBarColor = BG

        setContentView(buildUi())

        handler.post(ticker)
        loadAccounts()
    }

    override fun onDestroy() {
        handler.removeCallbacks(ticker)
        executor.shutdownNow()
        super.onDestroy()
    }

    // =========================================================
    // MAIN UI
    // =========================================================

    private fun buildUi(): View {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG)
            setPadding(
                18.dp(),
                18.dp(),
                18.dp(),
                0
            )
        }

        // ---------------- HEADER ----------------

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
            text = "ACLClouds"
            textSize = 27f
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        subtitle = TextView(this).apply {
            text = "Auto Renew Dashboard"
            textSize = 13f
            setTextColor(MUTED)
            setPadding(0, 3.dp(), 0, 0)
        }

        titleBox.addView(title)
        titleBox.addView(subtitle)

        val refresh = iconButton("↻")

        refresh.setOnClickListener {
            loadAccounts()
        }

        header.addView(titleBox)
        header.addView(refresh)

        root.addView(header)

        // ---------------- WELCOME ----------------

        val welcome = TextView(this).apply {
            text = "Your services at a glance"
            textSize = 15f
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD

            setPadding(
                0,
                25.dp(),
                0,
                12.dp()
            )
        }

        root.addView(welcome)

        // ---------------- STATS ----------------

        val stats = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        accountsCount = statCard(
            stats,
            "ACCOUNTS",
            "0"
        )

        activeCount = statCard(
            stats,
            "ACTIVE",
            "0"
        )

        expiringCount = statCard(
            stats,
            "EXPIRING",
            "0"
        )

        root.addView(stats)

        // ---------------- SECTION TITLE ----------------

        val servicesTitle = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                0,
                24.dp(),
                0,
                10.dp()
            )
        }

        val services = TextView(this).apply {
            text = "YOUR SERVICES"
            textSize = 13f
            setTextColor(MUTED)
            typeface = Typeface.DEFAULT_BOLD

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val refreshText = TextView(this).apply {
            text = "Refresh"
            textSize = 12f
            setTextColor(BLUE)

            setOnClickListener {
                loadAccounts()
            }
        }

        servicesTitle.addView(services)
        servicesTitle.addView(refreshText)

        root.addView(servicesTitle)

        // ---------------- SCROLL ----------------

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                0,
                0,
                0,
                25.dp()
            )
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

        // ---------------- BOTTOM BAR ----------------

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(
                0,
                8.dp(),
                0,
                10.dp()
            )

            background = rounded(
                CARD,
                20.dp()
            )
        }

        bottom.addView(
            bottomItem("⌂", "Dashboard", true)
        )

        bottom.addView(
            bottomItem("☷", "Accounts", false)
        )

        bottom.addView(
            bottomItem("⚙", "Settings", false)
        )

        root.addView(
            bottom,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                65.dp()
            )
        )

        return root
    }

    // =========================================================
    // STATS
    // =========================================================

    private fun statCard(
        parent: LinearLayout,
        label: String,
        value: String
    ): TextView {

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL

            setPadding(
                13.dp(),
                12.dp(),
                13.dp(),
                12.dp()
            )

            background = rounded(
                CARD,
                16.dp()
            )
        }

        val lp = LinearLayout.LayoutParams(
            0,
            78.dp(),
            1f
        )

        lp.setMargins(
            0,
            0,
            7.dp(),
            0
        )

        parent.addView(box, lp)

        val number = TextView(this).apply {
            text = value
            textSize = 22f
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val text = TextView(this).apply {
            text = label
            textSize = 9f
            setTextColor(MUTED)
            setPadding(0, 2.dp(), 0, 0)
        }

        box.addView(number)
        box.addView(text)

        return number
    }

    // =========================================================
    // ACCOUNT CARD
    // =========================================================

    private fun accountCard(a: Account): View {

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                17.dp(),
                17.dp(),
                17.dp(),
                17.dp()
            )

            background = rounded(
                CARD,
                20.dp()
            )

            elevation = 2f
        }

        val lp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        lp.setMargins(
            0,
            0,
            0,
            13.dp()
        )

        card.layoutParams = lp

        // ---------------- TOP ----------------

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val avatar = TextView(this).apply {
            text = accountNumber(a)
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD

            background = rounded(
                BLUE,
                12.dp()
            )

            layoutParams = LinearLayout.LayoutParams(
                42.dp(),
                42.dp()
            )
        }

        top.addView(avatar)

        val identity = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL

            setPadding(
                12.dp(),
                0,
                8.dp(),
                0
            )

            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val name = TextView(this).apply {
            text = a.name.ifBlank {
                "Account ${accountNumber(a)}"
            }

            textSize = 16f
            setTextColor(WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }

        val email = TextView(this).apply {
            text = a.email.ifBlank {
                "No email"
            }

            textSize = 11f
            setTextColor(MUTED)

            setPadding(
                0,
                3.dp(),
                0,
                0
            )
        }

        identity.addView(name)
        identity.addView(email)

        top.addView(identity)

        val status = statusBadge(a)

        top.addView(status)

        card.addView(top)

        // ---------------- SERVICE ----------------

        val service = TextView(this).apply {
            text = "SERVICE  •  ${
                a.serviceId.ifBlank { "—" }
            }"

            textSize = 10f
            setTextColor(MUTED)

            setPadding(
                0,
                14.dp(),
                0,
                0
            )
        }

        card.addView(service)

        // ---------------- COUNTDOWN ----------------

        val countdown = TextView(this).apply {

            tag = "countdown"

            text = expiryText(a.expiry)

            textSize = 25f

            setTextColor(
                countdownColor(a.expiry)
            )

            typeface = Typeface.DEFAULT_BOLD

            setPadding(
                0,
                10.dp(),
                0,
                0
            )
        }

        card.addView(countdown)

        val remainingLabel = TextView(this).apply {
            text = "TIME REMAINING"
            textSize = 9f
            setTextColor(MUTED)
        }

        card.addView(remainingLabel)

        // ---------------- INFO ----------------

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL

            setPadding(
                0,
                13.dp(),
                0,
                13.dp()
            )
        }

        val expiry = smallInfo(
            "EXPIRY",
            formatDate(a.expiry)
        )

        val check = smallInfo(
            "LAST CHECK",
            formatDate(a.lastCheck)
        )

        info.addView(
            expiry,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        info.addView(
            check,
            LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        card.addView(info)

        // ---------------- ERROR ----------------

        if (!a.lastError.isNullOrBlank()) {

            val error = TextView(this).apply {
                text = "⚠ ${a.lastError}"
                textSize = 11f
                setTextColor(RED)

                setPadding(
                    10.dp(),
                    8.dp(),
                    10.dp(),
                    8.dp()
                )

                background = rounded(
                    0x22FF5C6C,
                    10.dp()
                )
            }

            card.addView(error)

            space(card, 10)
        }

        // ---------------- BUTTONS ----------------

        val actions = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val checkButton = actionButton(
            "CHECK NOW",
            BLUE
        )

        checkButton.setOnClickListener {
            manualCheck(a.id)
        }

        val renewButton = actionButton(
            "RENEW",
            GREEN
        )

        renewButton.setOnClickListener {
            Toast.makeText(
                this,
                "Renew integration will be connected after the ACLClouds Renew flow is confirmed.",
                Toast.LENGTH_LONG
            ).show()
        }

        actions.addView(
            checkButton,
            LinearLayout.LayoutParams(
                0,
                45.dp(),
                1f
            ).apply {
                setMargins(
                    0,
                    0,
                    5.dp(),
                    0
                )
            }
        )

        actions.addView(
            renewButton,
            LinearLayout.LayoutParams(
                0,
                45.dp(),
                1f
            ).apply {
                setMargins(
                    5.dp(),
                    0,
                    0,
                    0
                )
            }
        )

        card.addView(actions)

        return card
    }

    // =========================================================
    // STATUS
    // =========================================================

    private fun statusBadge(a: Account): TextView {

        val statusText =
            when {
                !a.lastError.isNullOrBlank() ->
                    "ERROR"

                a.expiry != null &&
                        a.expiry - System.currentTimeMillis()
                        <= 2 * 24 * 60 * 60 * 1000L ->
                    "EXPIRING"

                else ->
                    "ACTIVE"
            }

        val color =
            when (statusText) {
                "ERROR" -> RED
                "EXPIRING" -> ORANGE
                else -> GREEN
            }

        return TextView(this).apply {
            text = statusText
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(color)
            typeface = Typeface.DEFAULT_BOLD

            setPadding(
                9.dp(),
                6.dp(),
                9.dp(),
                6.dp()
            )

            background = rounded(
                color and 0x00FFFFFF or 0x22000000,
                20.dp()
            )
        }
    }

    // =========================================================
    // SMALL INFO
    // =========================================================

    private fun smallInfo(
        title: String,
        value: String
    ): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            val t = TextView(this@MainActivity).apply {
                text = title
                textSize = 8f
                setTextColor(MUTED)
            }

            val v = TextView(this@MainActivity).apply {
                text = value
                textSize = 11f
                setTextColor(WHITE)
                typeface = Typeface.DEFAULT_BOLD

                setPadding(
                    0,
                    3.dp(),
                    0,
                    0
                )
            }

            addView(t)
            addView(v)
        }
    }

    // =========================================================
    // BOTTOM NAV
    // =========================================================

    private fun bottomItem(
        icon: String,
        label: String,
        selected: Boolean
    ): LinearLayout {

        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setPadding(
                20.dp(),
                5.dp(),
                20.dp(),
                5.dp()
            )
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 21f
            gravity = Gravity.CENTER

            setTextColor(
                if (selected) BLUE else MUTED
            )
        }

        val text = TextView(this).apply {
            text = label
            textSize = 9f
            gravity = Gravity.CENTER

            setTextColor(
                if (selected) WHITE else MUTED
            )

            setPadding(
                0,
                2.dp(),
                0,
                0
            )
        }

        item.addView(iconView)
        item.addView(text)

        return item
    }

    // =========================================================
    // BUTTONS
    // =========================================================

    private fun actionButton(
        text: String,
        color: Int
    ): TextView {

        return TextView(this).apply {

            this.text = text

            gravity = Gravity.CENTER

            textSize = 11f

            setTextColor(
                if (color == GREEN)
                    BG
                else
                    WHITE
            )

            typeface = Typeface.DEFAULT_BOLD

            background = rounded(
                color,
                13.dp()
            )
        }
    }

    private fun iconButton(
        icon: String
    ): TextView {

        return TextView(this).apply {

            text = icon

            textSize = 25f

            gravity = Gravity.CENTER

            setTextColor(WHITE)

            background = rounded(
                CARD,
                14.dp()
            )

            layoutParams = LinearLayout.LayoutParams(
                48.dp(),
                48.dp()
            )
        }
    }

    // =========================================================
    // DATA
    // =========================================================

    private fun loadAccounts() {

        renderMessage("Loading services...")

        val base = prefs.getString(
            "baseUrl",
            "http://10.0.2.2:3000"
        )!!.trimEnd('/')

        executor.execute {

            try {

                val json =
                    httpGet("$base/api/accounts")

                val parsed =
                    parseAccounts(json)

                runOnUiThread {

                    accounts = parsed

                    subtitle.text =
                        "${parsed.size} services connected"

                    renderAll()
                    updateStats()
                }

            } catch (e: Exception) {

                runOnUiThread {

                    subtitle.text =
                        "Connection unavailable"

                    renderMessage(
                        "Unable to load services.\n\n${e.message}"
                    )

                    updateStats()
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
            "Checking account...",
            Toast.LENGTH_SHORT
        ).show()

        executor.execute {

            try {

                httpPost(
                    "$base/api/accounts/$id/check"
                )

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

    // =========================================================
    // RENDER
    // =========================================================

    private fun renderAll() {

        container.removeAllViews()

        if (accounts.isEmpty()) {

            renderMessage(
                "No services found.\n\nConfigure your ACLClouds accounts first."
            )

            return
        }

        accounts.forEach {
            container.addView(
                accountCard(it)
            )
        }
    }

    private fun renderMessage(
        msg: String
    ) {

        if (!::container.isInitialized)
            return

        container.removeAllViews()

        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER

            setPadding(
                25.dp(),
                70.dp(),
                25.dp(),
                70.dp()
            )

            background = rounded(
                CARD,
                20.dp()
            )
        }

        val icon = TextView(this).apply {
            text = "☁"
            textSize = 42f
            gravity = Gravity.CENTER
            setTextColor(BLUE)
        }

        val message = TextView(this).apply {
            text = msg
            textSize = 14f
            gravity = Gravity.CENTER
            setTextColor(MUTED)
            setPadding(
                0,
                12.dp(),
                0,
                0
            )
        }

        box.addView(icon)
        box.addView(message)

        container.addView(box)
    }

    // =========================================================
    // COUNTDOWN
    // =========================================================

    private fun redrawCountdownsOnly() {

        if (!::container.isInitialized)
            return

        for (i in 0 until container.childCount) {

            val card =
                container.getChildAt(i)
                    as? LinearLayout
                    ?: continue

            val countdown =
                card.findViewWithTag<TextView>(
                    "countdown"
                ) ?: continue

            val account =
                accounts.getOrNull(i)
                    ?: continue

            countdown.text =
                expiryText(account.expiry)

            countdown.setTextColor(
                countdownColor(account.expiry)
            )
        }
    }

    private fun expiryText(
        expiry: Long?
    ): String {

        if (expiry == null)
            return "—"

        val diff =
            expiry - System.currentTimeMillis()

        if (diff <= 0)
            return "EXPIRED"

        val totalSec =
            diff / 1000

        val days =
            totalSec / 86400

        val hours =
            (totalSec % 86400) / 3600

        val minutes =
            (totalSec % 3600) / 60

        val seconds =
            totalSec % 60

        return "${days}d ${hours}h ${minutes}m ${seconds}s"
    }

    private fun countdownColor(
        expiry: Long?
    ): Int {

        if (expiry == null)
            return MUTED

        val diff =
            expiry - System.currentTimeMillis()

        return when {

            diff <= 0 ->
                RED

            diff <= 24 * 60 * 60 * 1000L ->
                RED

            diff <= 2 * 24 * 60 * 60 * 1000L ->
                ORANGE

            else ->
                GREEN
        }
    }

    // =========================================================
    // STATS
    // =========================================================

    private fun updateStats() {

        if (!::accountsCount.isInitialized)
            return

        val total =
            accounts.size

        val active =
            accounts.count {
                it.lastError.isNullOrBlank() &&
                        (
                                it.expiry == null ||
                                        it.expiry >
                                        System.currentTimeMillis()
                                )
            }

        val expiring =
            accounts.count {

                val expiry =
                    it.expiry ?: return@count false

                val diff =
                    expiry -
                            System.currentTimeMillis()

                diff > 0 &&
                        diff <=
                        2 * 24 * 60 * 60 * 1000L
            }

        accountsCount.text =
            total.toString()

        activeCount.text =
            active.toString()

        expiringCount.text =
            expiring.toString()
    }

    // =========================================================
    // HELPERS
    // =========================================================

    private fun accountNumber(
        a: Account
    ): String {

        val number =
            a.id.filter {
                it.isDigit()
            }.toIntOrNull()

        return if (number != null)
            number.toString()
        else
            "01"
    }

    private fun formatDate(
        time: Long?
    ): String {

        if (time == null)
            return "—"

        return try {

            val formatter =
                SimpleDateFormat(
                    "dd MMM, HH:mm",
                    Locale.getDefault()
                )

            formatter.format(
                Date(time)
            )

        } catch (_: Exception) {
            "—"
        }
    }

    private fun parseAccounts(
        json: String
    ): List<Account> {

        val arr =
            JSONArray(json)

        val out =
            ArrayList<Account>()

        for (i in 0 until arr.length()) {

            val o =
                arr.getJSONObject(i)

            fun longOrNull(
                name: String
            ): Long? {

                val s =
                    o.optString(
                        name,
                        ""
                    )

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

    // =========================================================
    // HTTP
    // =========================================================

    private fun httpGet(
        urlString: String
    ): String {

        val conn =
            URL(urlString)
                .openConnection()
                    as HttpURLConnection

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
                .openConnection()
                    as HttpURLConnection

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

        val code =
            conn.responseCode

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
            error(
                "HTTP $code: $body"
            )
        }

        return body
    }

    // =========================================================
    // DRAWABLES
    // =========================================================

    private fun rounded(
        color: Int,
        radius: Int
    ): GradientDrawable {

        return GradientDrawable().apply {
            setColor(color)
            cornerRadius =
                radius.toFloat()
        }
    }

    private fun space(
        parent: LinearLayout,
        dp: Int
    ) {

        val view = View(this)

        parent.addView(
            view,
            LinearLayout.LayoutParams(
                1,
                dp.dp()
            )
        )
    }

    private fun Int.dp(): Int =
        (
                this *
                        resources.displayMetrics.density
                ).toInt()
}
