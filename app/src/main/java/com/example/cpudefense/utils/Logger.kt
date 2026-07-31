package com.example.cpudefense.utils

import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Logger(val activity: AppCompatActivity, val logLevel: Level = Level.MESSAGE)
{
    private val logfileName = "log.txt"
    enum class Level { DEBUG, MESSAGE, WARN, ERROR }
    private val timeFormatShort = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val timeFormatLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
    private val leveltext = hashMapOf(Level.MESSAGE to "INFO", Level.WARN to "WARN", Level.DEBUG to "DBG", Level.ERROR to "ERR")
    private var logfile = File(activity.filesDir, logfileName)
    private var fileOutputStream: FileOutputStream? = null
    private var outputStreamWriter: OutputStreamWriter? = null
    private var currentIndent = 0

    fun start()
    {
        fileOutputStream = FileOutputStream(logfile, false)
        outputStreamWriter = OutputStreamWriter(fileOutputStream)
        currentIndent = 0
        val logString = "Start of log for ${activity.title}. Current time is ${timeFormatLong.format(Date())}"
        log(logString)
    }

    fun log(text: String, messagelevel: Level = Level.MESSAGE, indent: Int = 0, unIndent: Int = 0)
    {
        if (messagelevel == Level.DEBUG && logLevel != Level.DEBUG)
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
        if (messagelevel != Level.DEBUG)
            print(text)
        currentIndent += indent
    }

    fun debug(text: String, indent: Int =0, unIndent: Int =0)
    { log(text, Level.DEBUG, indent=indent, unIndent=unIndent) }

    fun warn(text: String, indent: Int =0, unIndent: Int =0)
    { log(text, Level.WARN, indent=indent, unIndent=unIndent) }

    fun err(text: String, indent: Int =0, unIndent: Int =0)
    { log(text, Level.ERROR, indent=indent, unIndent=unIndent) }

    fun stop()
    {
        currentIndent = 0
        val logString = "End of log. Current time is "+timeFormatLong.format(Date())
        log(logString)
        outputStreamWriter?.close()
        fileOutputStream?.close()
        outputStreamWriter = null
        fileOutputStream = null
    }
}