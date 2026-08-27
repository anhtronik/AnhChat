package com.anhtronik.anhchat

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*

class MainActivity : Activity() {

    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showHome()
    }

    private fun showHome() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val header = TextView(this).apply {
            text = "AnhChat"
            textSize = 25f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(45, 25, 20, 25)
            setBackgroundColor(Color.rgb(7, 94, 84))
        }

        root.addView(
            header,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                160
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

        root.addView(tabs)

        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 15, 20, 20)
        }

        val scroll = ScrollView(this)
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
                120,
                1f
            )
        }
    }

    private fun showChats() {
        content.removeAllViews()

        addChat(
            "Admin Anhtronik",
            "Selamat datang di AnhChat 👋",
            "15:40"
        )

        addChat(
            "Contoh Pengguna",
            "Halo, ini pesan percobaan",
            "14:21"
        )

        addChat(
            "Grup AnhChat",
            "Fitur grup akan tersedia",
            "Kemarin"
        )

        val newChat = Button(this).apply {
            text = "＋ MULAI CHAT BARU"
            setOnClickListener {
                openChat("Chat Baru")
            }
        }

        content.addView(newChat)
    }

    private fun addChat(
        name: String,
        message: String,
        time: String
    ) {
        val item = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 30, 25, 30)
            isClickable = true
            setBackgroundColor(Color.WHITE)
        }

        val title = TextView(this).apply {
            text = name
            textSize = 18f
            setTextColor(Color.BLACK)
            setTypeface(null, Typeface.BOLD)
        }

        val msg = TextView(this).apply {
            text = "$message   •   $time"
            textSize = 14f
            setTextColor(Color.DKGRAY)
        }

        item.addView(title)
        item.addView(msg)

        item.setOnClickListener {
            openChat(name)
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

    private fun openChat(name: String) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(239, 234, 226))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(10, 10, 10, 10)
            setBackgroundColor(Color.rgb(7, 94, 84))
        }

        val back = Button(this).apply {
            text = "‹"
            setOnClickListener {
                showHome()
            }
        }

        val title = TextView(this).apply {
            text = "$name\nonline"
            textSize = 18f
            setTextColor(Color.WHITE)
            setPadding(20, 0, 0, 0)
        }

        val call = Button(this).apply {
            text = "☎"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Panggilan suara tahap berikutnya",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val video = Button(this).apply {
            text = "▣"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Video call tahap berikutnya",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        top.addView(back)
        top.addView(
            title,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )
        top.addView(video)
        top.addView(call)

        root.addView(top)

        val messages = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(25, 25, 25, 25)
        }

        val scroll = ScrollView(this)
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
            text = "Halo 👋\nIni ruang chat AnhChat."
            textSize = 17f
            setTextColor(Color.BLACK)
            setPadding(25, 20, 25, 20)
            setBackgroundColor(Color.WHITE)
        }

        messages.addView(hello)

        val bottom = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(10, 10, 10, 10)
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.WHITE)
        }

        val input = EditText(this).apply {
            hint = "Pesan"
            setSingleLine(false)
        }

        val attach = Button(this).apply {
            text = "＋"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Foto / video / file tahap berikutnya",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val mic = Button(this).apply {
            text = "🎤"
            setOnClickListener {
                Toast.makeText(
                    this@MainActivity,
                    "Voice note tahap berikutnya",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        val send = Button(this).apply {
            text = "➤"

            setOnClickListener {
                val text = input.text.toString().trim()

                if (text.isNotEmpty()) {
                    val bubble = TextView(this@MainActivity).apply {
                        this.text = text
                        textSize = 17f
                        setTextColor(Color.BLACK)
                        setPadding(25, 20, 25, 20)
                        setBackgroundColor(Color.rgb(220, 248, 198))
                    }

                    messages.addView(bubble)
                    input.setText("")
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

        setContentView(root)
    }

    private fun showStatus() {
        content.removeAllViews()

        val title = TextView(this).apply {
            text = "Status Saya"
            textSize = 21f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.BLACK)
            setPadding(20, 30, 20, 15)
        }

        val description = TextView(this).apply {
            text = "Ketuk untuk menambahkan status foto, video atau teks.\n\nStatus 24 jam akan dibuat pada tahap backend."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(20, 10, 20, 30)
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
            setPadding(20, 30, 20, 15)
        }

        val description = TextView(this).apply {
            text = "Belum ada panggilan.\n\nVoice call dan video call akan disambungkan ke server WebRTC."
            textSize = 16f
            setTextColor(Color.DKGRAY)
            setPadding(20, 10, 20, 30)
        }

        content.addView(title)
        content.addView(description)
    }
}
