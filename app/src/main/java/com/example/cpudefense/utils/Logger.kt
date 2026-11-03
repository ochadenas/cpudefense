package com.example.cpudefense.utils

import android.util.Log
import com.example.cpudefense.ChipDefense
import com.example.cpudefense.GameMechanics
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Logger object, implemented as singleton. It exists independently of the activity and is
 * accessible from all fragments.
 */
object Logger
{
    private const val LOGFILE_NAME = "log.txt"

    /** tag for filtering in logcat */
    private const val TAG = "Application"

    /** whether a log is to be written */
    private var loggingEnabled: Boolean = true

    private val timeFormatShort = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private val timeFormatLong = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.getDefault())
    private val leveltext = hashMapOf(Log.INFO to "INFO", Log.WARN to "WARN", Log.DEBUG to "DBG", Log.ERROR to "ERR")
    private var fileOutputStream: FileOutputStream? = null
    private var outputStreamWriter: OutputStreamWriter? = null

    /** used for special formatting */
    private var currentIndent = 0

    fun start()
    {
        if (GameMechanics.enableLogging) {
            val logfile = ChipDefense.instance.let { app ->
                File(app.filesDir, LOGFILE_NAME).apply {
                    if (!exists()) createNewFile()
                }
            }
            fileOutputStream = FileOutputStream(logfile, false)
            outputStreamWriter = OutputStreamWriter(fileOutputStream)
            val logString = "Start of log. Current time is " + timeFormatLong.format(Date())
            info(logString)
        }
    }

    fun disable()
    {
        if (loggingEnabled)
        {
            info("Logging is disabled by settings.")
            loggingEnabled = false
        }
    }

    fun enable()
    {
        if (!loggingEnabled)
        {
            info("Re-enabling logging.")
            loggingEnabled = true
        }
    }

    fun log(text: String, messagelevel: Int = Log.INFO, indent: Int =0)
    {
        if (GameMechanics.enableLogging != true || !loggingEnabled)
            return
        if (indent<0) // un-indent
            currentIndent = (currentIndent+indent).coerceAtLeast(0)
        val logString = "%s [%-4.4s] %s%s\n".format(timeFormatShort.format(Date()), leveltext[messagelevel], " ".repeat(currentIndent), text)
        outputStreamWriter?.write(logString)
        outputStreamWriter?.flush()
        if (indent>0) currentIndent += indent
    }

    fun info(text: String, messagelevel: Int = Log.INFO, indent: Int =0)
    {
        log(text, Log.DEBUG, indent)
        Log.i(TAG, text)
    }

    fun debug(text: String, indent: Int =0)
    {
        log(text, Log.DEBUG, indent)
        Log.d(TAG, text)
    }

    fun warn(text: String, indent: Int =0)
    {
        log(text, Log.WARN, indent)
        Log.w(TAG, text)
    }

    fun err(text: String, indent: Int =0)
    {
        log(text, Log.ERROR, indent)
        Log.e(TAG, text)
    }

    fun stop()
    {
        currentIndent = 0
        val logString = "End of log. Current time is "+timeFormatLong.format(Date())
        info(logString)
        outputStreamWriter?.close()
        fileOutputStream?.close()
        outputStreamWriter = null
        fileOutputStream = null
    }
}