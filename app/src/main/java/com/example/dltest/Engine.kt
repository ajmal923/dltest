package com.example.dltest

import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

object Engine {
    const val PARALLEL = 10
    @Volatile var running = false
    @Volatile var link = ""
    val total = AtomicLong(0)
    val filesDone = AtomicInteger(0)
    val errors = AtomicInteger(0)

    fun start(dir: File, url: String) {
        if (running) return
        link = url
        total.set(0); filesDone.set(0); errors.set(0)
        running = true
        repeat(PARALLEL) { i -> Thread { worker(dir, i) }.start() }
    }

    fun stop() { running = false }

    private fun worker(dir: File, id: Int) {
        val buf = ByteArray(64 * 1024)
        while (running) {
            val f = File(dir, "t_${id}_${System.nanoTime()}.tmp")
            var c: HttpURLConnection? = null
            try {
                c = URL(link).openConnection() as HttpURLConnection
                c.connectTimeout = 15000
                c.readTimeout = 15000
                c.instanceFollowRedirects = true
                c.inputStream.use { ins ->
                    f.outputStream().use { out ->
                        while (running) {
                            val n = ins.read(buf)
                            if (n < 0) { filesDone.incrementAndGet(); break }
                            out.write(buf, 0, n)
                            total.addAndGet(n.toLong())
                        }
                    }
                }
            } catch (e: Exception) {
                errors.incrementAndGet()
                try { Thread.sleep(500) } catch (_: Exception) {}
            } finally {
                c?.disconnect()
                f.delete()
            }
        }
    }
}
