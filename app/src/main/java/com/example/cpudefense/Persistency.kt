package com.example.cpudefense

import android.content.SharedPreferences
import com.google.gson.Gson
import java.util.concurrent.CopyOnWriteArrayList

class Persistency(var game: Game?) {
    data class SerializableGameData (
        val general: Game.Data,
        val stage: Stage.Data?
            )

    data class SerializableLevelData (
        val level: HashMap<Int, Stage.Summary> = HashMap()
            )

    data class SerializableThumbnailData (
        val thumbnail: HashMap<Int, String> = HashMap()
    )

    data class SerializableUpgradeData (
        val upgrades: CopyOnWriteArrayList<Upgrade.Data> = CopyOnWriteArrayList<Upgrade.Data>()
    )

    fun saveState(editor: SharedPreferences.Editor)
    {
        game?.let {
            val data = SerializableGameData(
                general = it.data,
                stage = it.currentStage?.provideData()
            )
            var json = Gson().toJson(data)
            editor.putString("state", json)

            val upgradeData = SerializableUpgradeData()
            for (upgrade in it.gameUpgrades.values)
                upgradeData.upgrades.add(upgrade.data)
            json = Gson().toJson(upgradeData)
            editor.putString("upgrades", json)

            saveLevels(editor)
        }
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        game?.let {
            val json = sharedPreferences.getString("state", "none")
            if (json == "none")
                return   // no saved data present, start new game
            val data: SerializableGameData = Gson().fromJson(json, SerializableGameData::class.java)
            it.data = data.general
            it.stageData = data.stage
            it.summaryPerLevel = loadLevelSummaries(sharedPreferences) ?: HashMap()
        }
    }

    fun saveLevels(editor: SharedPreferences.Editor)
    {
        game?.let {
            // level summary:
            var data = SerializableLevelData(it.summaryPerLevel)
            var json = Gson().toJson(data)
            editor.putString("levels", json)
            // level thumbnail:
            val thumbnail = SerializableThumbnailData(it.levelThumbnail)
            json = Gson().toJson(thumbnail)
            editor.putString("thumbnails", json)
        }
    }

    fun loadLevelSummaries(sharedPreferences: SharedPreferences): HashMap<Int, Stage.Summary>?
    {
        val json = sharedPreferences.getString("levels", "none")
        if (json == "none")
            return null
        val data: SerializableLevelData = Gson().fromJson(json, SerializableLevelData::class.java)
        return data.level
    }

    fun loadLevelThumbnails(sharedPreferences: SharedPreferences): HashMap<Int, String>?
    {
        val json = sharedPreferences.getString("thumbnails", "none")
        if (json == "none")
            return null
        val data: SerializableThumbnailData = Gson().fromJson(json, SerializableThumbnailData::class.java)
        return data.thumbnail
    }

    fun loadUpgrades(sharedPreferences: SharedPreferences): HashMap<Upgrade.Type, Upgrade>
    {
        var upgradeMap = HashMap<Upgrade.Type, Upgrade>()
        val json = sharedPreferences.getString("upgrades", "none")
        if (json != "none") game?.let {
            val data: SerializableUpgradeData = Gson().fromJson(json, SerializableUpgradeData::class.java)
            for (upgradeData in data.upgrades)
                upgradeMap[upgradeData.type] = Upgrade.createFromData(it, upgradeData)
        }
        return upgradeMap
    }
}