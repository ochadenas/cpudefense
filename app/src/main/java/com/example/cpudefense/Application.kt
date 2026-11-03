package com.example.cpudefense
import android.app.Application

class ChipDefense: Application() {
    companion object { lateinit var instance: ChipDefense }
    override fun onCreate() { super.onCreate(); instance = this }
}