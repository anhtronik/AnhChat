package com.anhtronik.anhchat

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : Activity() {

    private val api =
        "https://anhchat-api.fahmiherman05.workers.dev"

    private val handler =
        Handler(Looper.getMainLooper())

    private var pollRunnable: Runnable? = null
    private var currentChatPhone: String? = null
    private var lastMessages = ""

    private val prefs by lazy {
        getSharedPreferences(
            "anhchat_account",
            MODE_PRIVATE
        )
    }

    private fun dp(v: Int): Int {
        return (
            v * resources.displayMetrics.density
        ).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val phone = prefs.getString(
            "phone",
            null
        )

        if (phone.isNullOrEmpty()) {
            showLogin()
        } else {
            showHome()
        }
    }

    private fun normalizePhone(
        value: String
    ): String? {

        var phone = value
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        phone = when {
            phone.startsWith("+62") ->
                phone

            phone.startsWith("62") ->
                "+$phone"

            phone.startsWith("0") ->
                "+62${phone.substring(1)}"

            else ->
                return null
        }

        if (
            phone.length < 11 ||
            phone.length > 16
        ) {
            return null
        }

        if (!phone.substring(1).all {
                it.isDigit()
            }) {
            return null
        }

        return phone
    }

    private fun postJson(
        path: String,
        body: JSONObject
    ): JSONObject {

        val connection =
            URL("$api$path")
                .openConnection()
                    as HttpURLConnection

        connection.requestMethod = "POST"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        connection.doOutput = true
        connection.setRequestProperty(
            "Content-Type",
            "application/json"
        )

        connection.outputStream.use {
            it.write(
                body.toString()
                    .toByteArray()
            )
        }

        val stream =
            if (
                connection.responseCode
                    in 200..299
            ) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val text =
            stream?.bufferedReader()
                ?.use { it.readText() }
                ?: "{}"

        connection.disconnect()

        return JSONObject(text)
    }

    private fun getJson(
        path: String
    ): JSONObject {

        val connection =
            URL("$api$path")
                .openConnection()
                    as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        val stream =
            if (
                connection.responseCode
                    in 200..299
            ) {
                connection.inputStream
            } else {
                connection.errorStream
            }

        val text =
            stream?.bufferedReader()
                ?.use { it.readText() }
                ?: "{}"

        connection.disconnect()

        return JSONObject(text)
    }

    private fun background(
        work: () -> Unit
    ) {
        Thread {
            try {
                work()
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this,
                        "Koneksi gagal: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun showLogin() {
        stopPolling()

        val root = LinearLayout(this).apply {
            orientation =
                LinearLayout.VERTICAL

            gravity = Gravity.CENTER

            setPadding(
                dp(28),
                dp(40),
                dp(28),
                dp(40)
            )

            setBackgroundColor(
                Color.WHITE
            )
        }

        val logo = TextView(this).apply {
            text = "💬"
            textSize = 72f
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = "Selamat datang di AnhChat"
            textSize = 26f

            setTextColor(Color.BLACK)

            setTypeface(
                null,
                Typeface.BOLD
            )

            gravity = Gravity.CENTER

            setPadding(
                0,
                dp(15),
                0,
                dp(10)
            )
        }

        val description =
            TextView(this).apply {

                text =
                    "Masukkan nomor HP untuk menggunakan AnhChat"

                textSize = 15f
                setTextColor(Color.DKGRAY)
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    0,
                    0,
                    dp(25)
                )
            }

        val phoneInput =
            EditText(this).apply {

                hint = "08xxxxxxxxxx"

                inputType =
                    InputType.TYPE_CLASS_PHONE

                textSize = 18f

                setPadding(
                    dp(14),
                    dp(12),
                    dp(14),
                    dp(12)
                )
            }

        val nameInput =
            EditText(this).apply {

                hint = "Nama Anda"

                inputType =
                    InputType.TYPE_CLASS_TEXT
            }

        val button =
            Button(this).apply {

                text = "LANJUTKAN"

                setOnClickListener {

                    val phone =
                        normalizePhone(
                            phoneInput.text
                                .toString()
                        )

                    val name =
                        nameInput.text
                            .toString()
                            .trim()

                    if (phone == null) {
                        phoneInput.error =
                            "Nomor HP tidak valid"
                        return@setOnClickListener
                    }

                    if (name.length < 2) {
                        nameInput.error =
                            "Masukkan nama"
                        return@setOnClickListener
                    }

                    isEnabled = false
                    text = "Memproses..."

                    background {

                        val response =
                            postJson(
                                "/users/register",
                                JSONObject().apply {
                                    put(
                                        "phone",
                                        phone
                                    )
                                    put(
                                        "name",
                                        name
                                    )
                                }
                            )

                        runOnUiThread {

                            isEnabled = true
                            text = "LANJUTKAN"

                            if (
                                response.optBoolean(
                                    "ok"
                                )
                            ) {

                                prefs.edit()
                                    .putString(
                                        "phone",
                                        phone
                                    )
                                    .putString(
                                        "name",
                                        name
                                    )
                                    .apply()

                                showHome()

                            } else {

                                Toast.makeText(
                                    this@MainActivity,
                                    response.optString(
                                        "message",
                                        "Pendaftaran gagal"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }

        root.addView(logo)

        root.addView(
            title,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(description)

        root.addView(
            phoneInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            nameInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(
            button,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            ).apply {
                topMargin = dp(20)
            }
        )

        val notice =
            TextView(this).apply {

                text =
                    "Versi pengujian AnhChat"

                textSize = 12f
                setTextColor(Color.GRAY)
                gravity = Gravity.CENTER

                setPadding(
                    0,
                    dp(18),
                    0,
                    0
                )
            }

        root.addView(notice)

        setContentView(root)
    }

    private fun showHome() {
        stopPolling()

        currentChatPhone = null

        val myPhone =
            prefs.getString(
                "phone",
                ""
            ) ?: ""

        val myName =
            prefs.getString(
                "name",
                "AnhChat"
            ) ?: "AnhChat"

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val header =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(18),
                    dp(10),
                    dp(10),
                    dp(10)
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        94,
                        84
                    )
                )
            }

        val title =
            TextView(this).apply {

                text = "AnhChat"
                textSize = 25f

                setTextColor(
                    Color.WHITE
                )

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        val account =
            TextView(this).apply {

                text = "⋮"
                textSize = 30f

                gravity = Gravity.CENTER

                setTextColor(
                    Color.WHITE
                )

                setOnClickListener {
                    showAccountMenu()
                }
            }

        header.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(58),
                1f
            )
        )

        header.addView(
            account,
            LinearLayout.LayoutParams(
                dp(55),
                dp(58)
            )
        )

        root.addView(header)

        val tabs =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                setBackgroundColor(
                    Color.rgb(
                        7,
                        94,
                        84
                    )
                )
            }

        val chatTab =
            makeTab("CHAT")

        val statusTab =
            makeTab("STATUS")

        val callsTab =
            makeTab("PANGGILAN")

        tabs.addView(chatTab)
        tabs.addView(statusTab)
        tabs.addView(callsTab)

        root.addView(
            tabs,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        val body =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(15),
                    dp(15),
                    dp(15),
                    dp(20)
                )
            }

        root.addView(
            body,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        fun showChats() {
            body.removeAllViews()

            val me =
                TextView(this).apply {

                    text =
                        "$myName\n$myPhone"

                    textSize = 16f

                    setTextColor(
                        Color.DKGRAY
                    )

                    setPadding(
                        dp(10),
                        dp(8),
                        dp(10),
                        dp(20)
                    )
                }

            body.addView(me)

            val info =
                TextView(this).apply {

                    text =
                        "Cari nomor pengguna AnhChat untuk mulai mengirim pesan."

                    textSize = 16f

                    setTextColor(
                        Color.DKGRAY
                    )

                    setPadding(
                        dp(10),
                        dp(20),
                        dp(10),
                        dp(20)
                    )
                }

            body.addView(info)

            val newChat =
                Button(this).apply {

                    text =
                        "＋ CHAT NOMOR BARU"

                    setOnClickListener {
                        showNumberDialog()
                    }
                }

            body.addView(newChat)
        }

        chatTab.setOnClickListener {
            showChats()
        }

        statusTab.setOnClickListener {

            body.removeAllViews()

            body.addView(
                TextView(this).apply {
                    text =
                        "STATUS\n\nStatus foto, video dan teks 24 jam akan ditambahkan pada tahap berikutnya."

                    textSize = 18f

                    setPadding(
                        dp(15),
                        dp(25),
                        dp(15),
                        dp(15)
                    )
                }
            )
        }

        callsTab.setOnClickListener {

            body.removeAllViews()

            body.addView(
                TextView(this).apply {

                    text =
                        "PANGGILAN\n\nRiwayat voice call dan video call akan ditambahkan setelah sistem chat stabil."

                    textSize = 18f

                    setPadding(
                        dp(15),
                        dp(25),
                        dp(15),
                        dp(15)
                    )
                }
            )
        }

        setContentView(root)

        showChats()
    }

    private fun makeTab(
        title: String
    ): TextView {

        return TextView(this).apply {

            text = title
            textSize = 14f

            gravity = Gravity.CENTER

            setTextColor(
                Color.WHITE
            )

            setTypeface(
                null,
                Typeface.BOLD
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1f
                )
        }
    }

    private fun showAccountMenu() {

        val phone =
            prefs.getString(
                "phone",
                ""
            ) ?: ""

        val name =
            prefs.getString(
                "name",
                ""
            ) ?: ""

        AlertDialog.Builder(this)
            .setTitle(name)
            .setMessage(phone)
            .setPositiveButton(
                "Tutup",
                null
            )
            .setNegativeButton(
                "Ganti nomor / Keluar"
            ) { _, _ ->

                prefs.edit()
                    .clear()
                    .apply()

                showLogin()
            }
            .show()
    }

    private fun showNumberDialog() {

        val input =
            EditText(this).apply {

                hint =
                    "08xxxxxxxxxx"

                inputType =
                    InputType.TYPE_CLASS_PHONE
            }

        val wrap =
            LinearLayout(this).apply {

                setPadding(
                    dp(20),
                    dp(5),
                    dp(20),
                    0
                )

                addView(
                    input,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                )
            }

        val dialog =
            AlertDialog.Builder(this)
                .setTitle(
                    "Chat Baru"
                )
                .setMessage(
                    "Masukkan nomor pengguna AnhChat"
                )
                .setView(wrap)
                .setNegativeButton(
                    "Batal",
                    null
                )
                .setPositiveButton(
                    "Cari",
                    null
                )
                .create()

        dialog.setOnShowListener {

            dialog
                .getButton(
                    AlertDialog.BUTTON_POSITIVE
                )
                .setOnClickListener {

                    val phone =
                        normalizePhone(
                            input.text
                                .toString()
                        )

                    if (phone == null) {
                        input.error =
                            "Nomor tidak valid"
                        return@setOnClickListener
                    }

                    val myPhone =
                        prefs.getString(
                            "phone",
                            ""
                        )

                    if (phone == myPhone) {
                        input.error =
                            "Itu nomor Anda sendiri"
                        return@setOnClickListener
                    }

                    dialog.getButton(
                        AlertDialog.BUTTON_POSITIVE
                    ).isEnabled = false

                    background {

                        val encoded =
                            URLEncoder.encode(
                                phone,
                                "UTF-8"
                            )

                        val response =
                            getJson(
                                "/users/find?phone=$encoded"
                            )

                        runOnUiThread {

                            if (
                                response.optBoolean(
                                    "registered"
                                )
                            ) {

                                dialog.dismiss()

                                val user =
                                    response.optJSONObject(
                                        "user"
                                    )

                                val name =
                                    user?.optString(
                                        "name"
                                    ) ?: phone

                                openChat(
                                    phone,
                                    name
                                )

                            } else {

                                dialog.getButton(
                                    AlertDialog.BUTTON_POSITIVE
                                ).isEnabled = true

                                Toast.makeText(
                                    this@MainActivity,
                                    "Nomor belum menggunakan AnhChat",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
        }

        dialog.show()
    }

    private fun openChat(
        phone: String,
        name: String
    ) {
        stopPolling()

        currentChatPhone = phone
        lastMessages = ""

        val root =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setBackgroundColor(
                    Color.rgb(
                        239,
                        234,
                        226
                    )
                )
            }

        val top =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(5),
                    dp(5),
                    dp(5),
                    dp(5)
                )

                setBackgroundColor(
                    Color.rgb(
                        7,
                        94,
                        84
                    )
                )
            }

        val back =
            TextView(this).apply {

                text = "‹"
                textSize = 38f
                gravity = Gravity.CENTER

                setTextColor(
                    Color.WHITE
                )

                setOnClickListener {
                    showHome()
                }
            }

        val title =
            TextView(this).apply {

                text =
                    "$name\n$phone"

                textSize = 16f

                setTextColor(
                    Color.WHITE
                )

                gravity =
                    Gravity.CENTER_VERTICAL

                setTypeface(
                    null,
                    Typeface.BOLD
                )
            }

        val video =
            TextView(this).apply {

                text = "▣"
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)

                setOnClickListener {

                    Toast.makeText(
                        this@MainActivity,
                        "Video call akan ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val call =
            TextView(this).apply {

                text = "☎"
                textSize = 22f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)

                setOnClickListener {

                    Toast.makeText(
                        this@MainActivity,
                        "Voice call akan ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        top.addView(
            back,
            LinearLayout.LayoutParams(
                dp(50),
                dp(60)
            )
        )

        top.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(60),
                1f
            )
        )

        top.addView(
            video,
            LinearLayout.LayoutParams(
                dp(50),
                dp(60)
            )
        )

        top.addView(
            call,
            LinearLayout.LayoutParams(
                dp(50),
                dp(60)
            )
        )

        root.addView(top)

        val messages =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.VERTICAL

                setPadding(
                    dp(10),
                    dp(10),
                    dp(10),
                    dp(10)
                )
            }

        val scroll =
            ScrollView(this).apply {
                isFillViewport = true
            }

        scroll.addView(messages)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        val bottom =
            LinearLayout(this).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    Gravity.CENTER_VERTICAL

                setPadding(
                    dp(5),
                    dp(5),
                    dp(5),
                    dp(8)
                )

                setBackgroundColor(
                    Color.WHITE
                )
            }

        val attach =
            TextView(this).apply {

                text = "+"
                textSize = 28f
                gravity = Gravity.CENTER

                setOnClickListener {
                    Toast.makeText(
                        this@MainActivity,
                        "Foto/video/file akan ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val input =
            EditText(this).apply {

                hint = "Pesan"
                textSize = 16f
                maxLines = 4

                setPadding(
                    dp(12),
                    dp(8),
                    dp(12),
                    dp(8)
                )
            }

        val mic =
            TextView(this).apply {

                text = "🎤"
                textSize = 21f
                gravity = Gravity.CENTER

                setOnClickListener {
                    Toast.makeText(
                        this@MainActivity,
                        "Voice note akan ditambahkan",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        val send =
            TextView(this).apply {

                text = "➤"
                textSize = 27f
                gravity = Gravity.CENTER

                setTextColor(
                    Color.rgb(
                        7,
                        94,
                        84
                    )
                )

                setOnClickListener {

                    val text =
                        input.text
                            .toString()
                            .trim()

                    if (text.isEmpty()) {
                        return@setOnClickListener
                    }

                    val myPhone =
                        prefs.getString(
                            "phone",
                            ""
                        ) ?: ""

                    input.setText("")

                    background {

                        val response =
                            postJson(
                                "/messages/send",
                                JSONObject().apply {

                                    put(
                                        "fromPhone",
                                        myPhone
                                    )

                                    put(
                                        "toPhone",
                                        phone
                                    )

                                    put(
                                        "message",
                                        text
                                    )
                                }
                            )

                        runOnUiThread {

                            if (
                                response.optBoolean(
                                    "ok"
                                )
                            ) {

                                lastMessages = ""

                                loadMessages(
                                    phone,
                                    messages,
                                    scroll
                                )

                            } else {

                                Toast.makeText(
                                    this@MainActivity,
                                    response.optString(
                                        "message",
                                        "Pesan gagal dikirim"
                                    ),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }

        bottom.addView(
            attach,
            LinearLayout.LayoutParams(
                dp(45),
                dp(52)
            )
        )

        bottom.addView(
            input,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        bottom.addView(
            mic,
            LinearLayout.LayoutParams(
                dp(45),
                dp(52)
            )
        )

        bottom.addView(
            send,
            LinearLayout.LayoutParams(
                dp(50),
                dp(52)
            )
        )

        bottom.setOnApplyWindowInsetsListener {
                view,
                insets ->

            view.setPadding(
                dp(5),
                dp(5),
                dp(5),
                dp(8) +
                    insets.systemWindowInsetBottom
            )

            insets
        }

        root.addView(bottom)

        setContentView(root)

        startPolling(
            phone,
            messages,
            scroll
        )
    }

    private fun loadMessages(
        phone: String,
        container: LinearLayout,
        scroll: ScrollView
    ) {

        val myPhone =
            prefs.getString(
                "phone",
                ""
            ) ?: ""

        background {

            val meEncoded =
                URLEncoder.encode(
                    myPhone,
                    "UTF-8"
                )

            val otherEncoded =
                URLEncoder.encode(
                    phone,
                    "UTF-8"
                )

            val response =
                getJson(
                    "/messages?me=$meEncoded&with=$otherEncoded"
                )

            if (
                !response.optBoolean(
                    "ok"
                )
            ) {
                return@background
            }

            val array =
                response.optJSONArray(
                    "messages"
                ) ?: JSONArray()

            val snapshot =
                array.toString()

            if (snapshot == lastMessages) {
                return@background
            }

            lastMessages = snapshot

            runOnUiThread {

                if (
                    currentChatPhone != phone
                ) {
                    return@runOnUiThread
                }

                container.removeAllViews()

                if (array.length() == 0) {

                    container.addView(
                        TextView(
                            this@MainActivity
                        ).apply {

                            text =
                                "Belum ada pesan.\nMulai percakapan dengan $phone"

                            textSize = 15f
                            setTextColor(
                                Color.GRAY
                            )

                            gravity =
                                Gravity.CENTER

                            setPadding(
                                dp(15),
                                dp(30),
                                dp(15),
                                dp(30)
                            )
                        }
                    )
                }

                for (
                    i in 0
                    until array.length()
                ) {

                    val message =
                        array.getJSONObject(i)

                    val sender =
                        message.optString(
                            "sender_phone"
                        )

                    val body =
                        message.optString(
                            "body"
                        )

                    val time =
                        message.optString(
                            "created_at"
                        )

                    val mine =
                        sender == myPhone

                    val bubble =
                        TextView(
                            this@MainActivity
                        ).apply {

                            text =
                                "$body\n${shortTime(time)}"

                            textSize = 16f

                            setTextColor(
                                Color.BLACK
                            )

                            setPadding(
                                dp(13),
                                dp(9),
                                dp(13),
                                dp(7)
                            )

                            setBackgroundColor(
                                if (mine) {
                                    Color.rgb(
                                        220,
                                        248,
                                        198
                                    )
                                } else {
                                    Color.WHITE
                                }
                            )
                        }

                    val params =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {

                            gravity =
                                if (mine) {
                                    Gravity.END
                                } else {
                                    Gravity.START
                                }

                            if (mine) {
                                setMargins(
                                    dp(55),
                                    dp(4),
                                    0,
                                    dp(4)
                                )
                            } else {
                                setMargins(
                                    0,
                                    dp(4),
                                    dp(55),
                                    dp(4)
                                )
                            }
                        }

                    container.addView(
                        bubble,
                        params
                    )
                }

                scroll.post {
                    scroll.fullScroll(
                        View.FOCUS_DOWN
                    )
                }
            }
        }
    }

    private fun shortTime(
        value: String
    ): String {

        if (
            value.length >= 16
        ) {
            return value
                .substring(11, 16)
        }

        return ""
    }

    private fun startPolling(
        phone: String,
        container: LinearLayout,
        scroll: ScrollView
    ) {

        stopPolling()

        val runnable =
            object : Runnable {

                override fun run() {

                    if (
                        currentChatPhone
                            != phone
                    ) {
                        return
                    }

                    loadMessages(
                        phone,
                        container,
                        scroll
                    )

                    handler.postDelayed(
                        this,
                        2500
                    )
                }
            }

        pollRunnable = runnable
        handler.post(runnable)
    }

    private fun stopPolling() {

        pollRunnable?.let {
            handler.removeCallbacks(it)
        }

        pollRunnable = null
    }

    @Deprecated(
        "Deprecated in Java"
    )
    override fun onBackPressed() {

        if (
            currentChatPhone != null
        ) {
            showHome()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        stopPolling()
        super.onDestroy()
    }
}
