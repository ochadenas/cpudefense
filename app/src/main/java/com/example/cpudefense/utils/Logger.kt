package com.example.cpudefense.utils

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Logger(val activity: AppCompatActivity, val logLevel: Int = Log.INFO) {
    private val logfileName = "log.txt"

    private val timeFormatShort = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val timeFormatLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
    private val leveltext =
        hashMapOf(Log.INFO to "INFO", Log.WARN to "WARN", Log.DEBUG to "DBG", Log.ERROR to "ERR")
    private var logfile = File(activity.filesDir, logfileName)
    private var fileOutputStream: FileOutputStream? = null
    private var outputStreamWriter: OutputStreamWriter? = null
    private var currentIndent = 0

    fun start() {
        fileOutputStream = FileOutputStream(logfile, false)
        outputStreamWriter = OutputStreamWriter(fileOutputStream)
        currentIndent = 0
        val logString =
            "Start of log for ${activity.title}. Current time is ${timeFormatLong.format(Date())}"
        log(logString)
    }

    fun log(text: String, messagelevel: Int = Log.INFO, indent: Int = 0, unIndent: Int = 0) {
        if (messagelevel == Log.DEBUG && logLevel != Log.DEBUG)
            return
        currentIndent -= unIndent
        val logString = "%s [%-4.4s] %s%s\n".format(
                timeFormatShort.format(Date()),
                leveltext[messagelevel],
                " ".repeat(currentIndent),
                text,
        )
        outputStreamWriter?.write(logString)
        outputStreamWriter?.flush()
        if (messagelevel != Log.DEBUG)
            Log.println(messagelevel, "LOGGER", text)
        currentIndent += indent
    }

    fun debug(text: String, indent: Int = 0, unIndent: Int = 0) {
        log(text, Log.DEBUG, indent = indent, unIndent = unIndent)
    }

    fun err(text: String, indent: Int = 0, unIndent: Int = 0) {
        log(text, Log.ERROR, indent = indent, unIndent = unIndent)
    }

    fun stop() {
        currentIndent = 0
        val logString = "End of log. Current time is " + timeFormatLong.format(Date())
        log(logString)
        outputStreamWriter?.close()
        fileOutputStream?.close()
        outputStreamWriter = null
        fileOutputStream = null
    }
}