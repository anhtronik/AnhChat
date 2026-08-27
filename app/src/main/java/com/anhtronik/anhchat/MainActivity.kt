package com.anhtronik.anhchat

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {

    private lateinit var content: LinearLayout

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = TextView(this).apply {
            text = "AnhChat"
            textSize = 24f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(18), dp(10), dp(18), dp(10))
            setBackgroundColor(Color.rgb(7, 94, 84))
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(64)
            )
        )

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.rgb(7, 94, 84))
        }

        val chatTab = makeTab("CHAT")
        val statusTab = makeTab("STATUS")
        val callTab = makeTab("PANGGILAN")

        tabs.addView(chatTab)
        tabs.addView(statusTab)
        tabs.addView(callTab)

        root.addView(
            tabs,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            )
        )

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(8), dp(12), dp(20))
        }

        val scroll = ScrollView(this).apply {
            isFillViewport = true
        }

        scroll.addView(content)

        root.addView(
            scroll,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        )

        chatTab.setOnClickListener { showChats() }
        statusTab.setOnClickListener { showStatus() }
        callTab.setOnClickListener { showCalls() }

        setContentView(root)
        showChats()
    }

    private fun makeTab(title: String): TextView {
        return TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f
            )
        }
    }

    private fun showChats() {
        content.removeAllViews()

        val info = TextView(this).apply {
            text = "Chat menggunakan nomor HP"
            textSize = 13f
            setTextColor(Color.GRAY)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        content.addView(info)

        addChat(
            "Admin Anhtronik",
            "+628xxxxxxxxxx",
            "Selamat datang di AnhChat 👋"
        )

        val newChat = Button(this).apply {
            text = "＋ CHAT NOMOR BARU"
            setOnClickListener {
                showNumberDialog()
            }
        }

        content.addView(newChat)
    }

    private fun addChat(
        name: String,
        phone: String,
        message: String
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            isClickable = true
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = name
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val number = TextView(this).apply {
            text = phone
            textSize = 13f
            setTextColor(Color.GRAY)
        }

        val msg = TextView(this).apply {
            text = message
            textSize = 15f
            setTextColor(Color.DKGRAY)
        }

        item.addView(title)
        item.addView(number)
        item.addView(msg)

        item.setOnClickListener {
            openChat(phone)
        }

        content.addView(item)

        content.addView(
            View(this).apply {
                setBackgroundColor(Color.LTGRAY)
            },
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            )
        )
    }

    private fun showNumberDialog() {
        val input = EditText(this).apply {
            hint = "08xxxxxxxxxx"
            inputType = InputType.TYPE_CLASS_PHONE
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val wrapper = LinearLayout(this).apply {
            setPadding(dp(20), dp(5), dp(20), 0)
            addView(
                input,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Mulai chat")
            .setMessage("Masukkan nomor HP pengguna AnhChat")
            .setView(wrapper)
            .setNegativeButton("Batal", null)
            .setPositiveButton("Cari", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val phone = normalizePhone(
                        input.text.toString()
                    )

                    if (phone == null) {
                        input.error = "Nomor HP tidak valid"
                        return@setOnClickListener
                    }

                    dialog.dismiss()
                    openChat(phone)
                }
        }

        dialog.show()
    }

    private fun normalizePhone(value: String): String? {
        var phone = value
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        phone = when {
            phone.startsWith("+62") -> phone

            phone.startsWith("62") ->
                "+$phone"

            phone.startsWith("0") ->
                "+62${phone.substring(1)}"

            else -> return null
        }

        if (phone.length < 11 || phone.length > 16) {
            return null
        }

        return phone
    }

    private fun openChat(phone: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(239, 234, 226))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
            setBackgroundColor(Color.rgb(7, 94, 84))
        }

        val back = TextView(this).apply {
            text = "‹"
            textSize = 38f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(56)
            )

            setOnClickListener {
                showHome()
            }
        }

        val title = TextView(this).apply {
            text = "$phone\nonline"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(8), 0, dp(5), 0)
        }

        val video = TextView(this).apply {
            text = "▣"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(56)
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Video call akan disambungkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val call = TextView(this).apply {
            text = "☎"
            textSize = 23f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(56)
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Panggilan akan disambungkan",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        top.addView(back)

        top.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                dp(56),
                1f
            )
        )

        top.addView(video)
        top.addView(call)

        root.addView(top)

        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }

        val scroll = ScrollView(this).apply {
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

        val hello = TextView(this).apply {
            text = "Chat dengan\n$phone"
            textSize = 16f
            setTextColor(Color.BLACK)
            setPadding(dp(14), dp(10), dp(14), dp(10))
            setBackgroundColor(Color.WHITE)
        }

        messages.addView(hello)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(10)
            )
            setBackgroundColor(Color.WHITE)
        }

        val attach = TextView(this).apply {
            text = "+"
            textSize = 28f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(52)
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Kirim foto/video/file",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val input = EditText(this).apply {
            hint = "Ketik pesan"
            textSize = 16f
            setSingleLine(false)
            maxLines = 4
            minHeight = dp(48)
            setPadding(
                dp(12),
                dp(8),
                dp(12),
                dp(8)
            )
        }

        val mic = TextView(this).apply {
            text = "🎤"
            textSize = 22f
            gravity = Gravity.CENTER

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(52)
            )

            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Voice note",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val send = TextView(this).apply {
            text = "➤"
            textSize = 25f
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(7, 94, 84))

            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(52)
            )

            setOnClickListener {
                val text = input.text
                    .toString()
                    .trim()

                if (text.isNotEmpty()) {

                    val bubble = TextView(
                        this@MainActivity
                    ).apply {
                        this.text = text
                        textSize = 16f
                        setTextColor(Color.BLACK)

                        setPadding(
                            dp(14),
                            dp(10),
                            dp(14),
                            dp(10)
                        )

                        setBackgroundColor(
                            Color.rgb(
                                220,
                                248,
                                198
                            )
                        )
                    }

                    val params =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply {
                            gravity = Gravity.END
                            setMargins(
                                dp(50),
                                dp(5),
                                0,
                                dp(5)
                            )
                        }

                    messages.addView(
                        bubble,
                        params
                    )

                    input.setText("")

                    scroll.post {
                        scroll.fullScroll(
                            View.FOCUS_DOWN
                        )
                    }
                }
            }
        }

        bottom.addView(attach)

        bottom.addView(
            input,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        bottom.addView(mic)
        bottom.addView(send)

        root.addView(bottom)

        View.setOnApplyWindowInsetsListener(bottom) { view, insets ->

            view.setPadding(
                dp(6),
                dp(6),
                dp(6),
                dp(10) + insets.systemWindowInsetBottom
            )

            insets
        }

        setContentView(root)

        bottom.post {
            input.requestFocus()
        }
    }

    private fun showStatus() {
        content.removeAllViews()

        val title = TextView(this).apply {
            text = "Status Saya"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(dp(15), dp(20), dp(15), dp(10))
        }

        val description = TextView(this).apply {
            text =
                "Status foto, video dan teks 24 jam akan ditampilkan di sini."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(dp(15), dp(5), dp(15), dp(20))
        }

        content.addView(title)
        content.addView(description)
    }

    private fun showCalls() {
        content.removeAllViews()

        val title = TextView(this).apply {
            text = "Panggilan AnhChat"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(dp(15), dp(20), dp(15), dp(10))
        }

        val description = TextView(this).apply {
            text =
                "Riwayat panggilan suara dan video akan tampil di sini."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(dp(15), dp(5), dp(15), dp(20))
        }

        content.addView(title)
        content.addView(description)
    }
}
