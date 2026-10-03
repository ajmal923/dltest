package com.example.dltest

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.*
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.*

class MainActivity : Activity() {
    private val h = Handler(Looper.getMainLooper())
    private lateinit var etLink: EditText
    private lateinit var tv: TextView
    private lateinit var tvInfo: TextView
    private lateinit var btn: Button
    private var last = 0L

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        val prefs = getSharedPreferences("p", Context.MODE_PRIVATE)

        val l = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }
        etLink = EditText(this).apply {
            hint = "Download link yahan paste karein"
            inputType = InputType.TYPE_TEXT_VARIATION_URI or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            setText(prefs.getString("link", ""))
            maxLines = 4
        }
        tv = TextView(this).apply { text = "0.00 Mbps"; textSize = 32f; gravity = Gravity.CENTER }
        tvInfo = TextView(this).apply { gravity = Gravity.CENTER }
        btn = Button(this)
        btn.setOnClickListener {
            if (Engine.running) {
                startService(Intent(this, DlService::class.java).setAction("STOP"))
                Engine.stop()
            } else {
                val u = etLink.text.toString().trim()
                if (!u.startsWith("http")) {
                    Toast.makeText(this, "Sahi link daalein", Toast.LENGTH_SHORT).show()
                } else {
                    prefs.edit().putString("link", u).apply()
                    val i = Intent(this, DlService::class.java).putExtra("link", u)
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i) else startService(i)
                    last = 0
                }
            }
            refresh()
        }
        l.addView(etLink); l.addView(tv); l.addView(tvInfo); l.addView(btn)
        setContentView(l)
        refresh()
    }

    private val ticker = object : Runnable {
        override fun run() {
            val now = Engine.total.get()
            tv.text = if (Engine.running) "%.2f Mbps".format(((now - last).coerceAtLeast(0)) * 8 / 1_000_000.0) else "0.00 Mbps"
            last = now
            refresh()
            h.postDelayed(this, 1000)
        }
    }

    private fun refresh() {
        btn.text = if (Engine.running) "Stop" else "Start"
        etLink.isEnabled = !Engine.running
        tvInfo.text = "Total: %d MB | Files: %d | Errors: %d".format(
            Engine.total.get() / 1_048_576, Engine.filesDone.get(), Engine.errors.get())
    }

    override fun onResume() { super.onResume(); last = Engine.total.get(); h.post(ticker) }
    override fun onPause() { h.removeCallbacks(ticker); super.onPause() }
}
