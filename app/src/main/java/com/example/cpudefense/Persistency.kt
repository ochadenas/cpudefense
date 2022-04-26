package com.example.cpudefense

import android.content.SharedPreferences
import com.google.gson.Gson

class Persistency(var game: Game?) {
    data class GameData (
        val general: Game.Data,
        val stage: Stage.Data?
            )

    data class LevelData (
        val level: HashMap<Int, Stage.Summary> = HashMap()
            )

    data class ThumbnailData (
        val thumbnail: HashMap<Int, String> = HashMap()
    )

    fun saveState(editor: SharedPreferences.Editor)
    {
        game?.let {
            val data = GameData(
                general = it.data,
                stage = it.currentStage?.provideData()
            )
            val json = Gson().toJson(data)
            editor.putString("state", json)
            saveLevels(editor)
        }
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        game?.let {
            val json = sharedPreferences.getString("state", "none")
            if (json == "none")
                return   // no saved data present, start new game
            val data: GameData = Gson().fromJson(json, GameData::class.java)
            it.data = data.general
            it.stageData = data.stage
            it.summaryPerLevel = loadLevelSummaries(sharedPreferences) ?: HashMap()
        }
    }

    fun saveLevels(editor: SharedPreferences.Editor)
    {
        game?.let {
            // level summary:
            var data = LevelData(it.summaryPerLevel)
            var json = Gson().toJson(data)
            editor.putString("levels", json)
            // level thumbnail:
            val thumbnail = ThumbnailData(it.levelThumbnail)
            json = Gson().toJson(thumbnail)
            editor.putString("thumbnails", json)
        }
    }

    fun loadLevelSummaries(sharedPreferences: SharedPreferences): HashMap<Int, Stage.Summary>?
    {
        val json = sharedPreferences.getString("levels", "none")
        if (json == "none")
            return null
        val data: LevelData = Gson().fromJson(json, LevelData::class.java)
        return data.level
    }

    fun loadLevelThumbnails(sharedPreferences: SharedPreferences): HashMap<Int, String>?
    {
        val json = sharedPreferences.getString("thumbnails", "none")
        if (json == "none")
            return null
        val data: ThumbnailData = Gson().fromJson(json, ThumbnailData::class.java)
        return data.thumbnail
    }
}