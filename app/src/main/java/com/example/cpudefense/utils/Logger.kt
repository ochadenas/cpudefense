package com.example.cpudefense.utils

import com.example.cpudefense.activities.GameActivity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Logger(activity: GameActivity)
{
    private val logfileName = "log.txt"
    enum class Level { DEBUG, MESSAGE, WARN, ERROR }
    private val timeFormatShort = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val timeFormatLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
    private val leveltext = hashMapOf(Level.MESSAGE to "INFO", Level.WARN to "WARN", Level.DEBUG to "DBG", Level.ERROR to "ERR")
    private var logfile = File(activity.filesDir, logfileName)
    private var fileOutputStream: FileOutputStream? = null
    private var outputStreamWriter: OutputStreamWriter? = null

    fun start()
    {
        fileOutputStream = FileOutputStream(logfile, false)
        outputStreamWriter = OutputStreamWriter(fileOutputStream)
        val logString = "Start of log. Current time is "+timeFormatLong.format(Date())
        log(logString)
    }

    fun log(text: String, loglevel: Level = Level.MESSAGE, indent: Int =0)
    {

        val logString = "%s [%-4.4s] %s%s\n".format(
                timeFormatShort.format(Date()),
                leveltext[loglevel],
                " ".repeat(indent),
                text)
        outputStreamWriter?.write(logString)
        outputStreamWriter?.flush()
        if (loglevel != Level.DEBUG)
            print(text)
    }

    fun debug(text: String, indent: Int =0)
    { log(text, Level.DEBUG, indent) }

    fun warn(text: String, indent: Int =0)
    { log(text, Level.WARN, indent) }

    fun err(text: String, indent: Int =0)
    { log(text, Level.ERROR, indent) }

    fun stop()
    {
        val logString = "End of log. Current time is "+timeFormatLong.format(Date())
        log(logString)
        outputStreamWriter?.close()
        fileOutputStream?.close()
        outputStreamWriter = null
        fileOutputStream = null
    }
}