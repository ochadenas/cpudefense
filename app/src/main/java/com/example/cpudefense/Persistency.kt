package com.example.cpudefense

import android.content.SharedPreferences
import com.example.cpudefense.networkmap.Network
import com.google.gson.Gson

class Persistency(var game: Game) {
    data class GameData (
        val general: Game.Data,
        // val network: Network.Data?,
        val stage: Stage.Data?
            )

    fun saveState(editor: SharedPreferences.Editor)
    {
        val data = GameData(
            general = game.data,
            stage = game.currentStage?.provideData()
        )
        val json = Gson().toJson(data)
        editor.putString("state", json)
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        val json = sharedPreferences.getString("state", "none")
        if (json == "none")
            return   // no saved data present, start new game
        val data: GameData = Gson().fromJson(json, GameData::class.java)
        game.data = data.general
        game.stageData = data.stage
        // game.network?.let { it.data = data.network ?: Network.Data() } // TODO
    }
}