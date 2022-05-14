package com.example.cpudefense

import android.content.SharedPreferences
import com.google.gson.Gson
import java.util.concurrent.CopyOnWriteArrayList

class Persistency(var game: Game?) {
    data class SerializableStateData (
        val general: Game.StateData,
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
            /** saves all data that is needed to continue a game later.
             * This includes levels completed and coins got,
             * but also the state of the currently running game.
             */
    {
        game?.let {
            // save global data:
            var json = Gson().toJson(it.global)
            editor.putString("global", json)

            // save current game state:
            val data = SerializableStateData(
                general = it.state,
                stage = it.currentStage?.provideData()
            )
            json = Gson().toJson(data)
            editor.putString("state", json)

            // save upgrades got so far:
            val upgradeData = SerializableUpgradeData()
            for (upgrade in it.gameUpgrades.values)
                upgradeData.upgrades.add(upgrade.data)
            json = Gson().toJson(upgradeData)
            editor.putString("upgrades", json)

            // save level data:
            saveLevels(editor)
        }
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        game?.let {
            // get state of running game
            var json = sharedPreferences.getString("state", "none")
            if (json == "none")
                return
            val data: SerializableStateData = Gson().fromJson(json, SerializableStateData::class.java)
            it.state = data.general
            it.stageData = data.stage

            // get level data
            it.summaryPerLevel = loadLevelSummaries(sharedPreferences) ?: HashMap()

            // get global data
            it.global = loadGlobalData(sharedPreferences)
        }
    }

    fun saveLevels(editor: SharedPreferences.Editor)
    {
        game?.let {
            // level summary:
            val data = SerializableLevelData(it.summaryPerLevel)
            var json = Gson().toJson(data)
            editor.putString("levels", json)
            // level thumbnail:
            val thumbnail = SerializableThumbnailData(it.levelThumbnail)
            json = Gson().toJson(thumbnail)
            editor.putString("thumbnails", json)
        }
    }

    fun loadGlobalData(sharedPreferences: SharedPreferences): Game.GlobalData
    {
        val json = sharedPreferences.getString("global", "none")
        if (json == "none")
            return Game.GlobalData()
        else
            return Gson().fromJson(json, Game.GlobalData::class.java)
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
        val upgradeMap = HashMap<Upgrade.Type, Upgrade>()
        val json = sharedPreferences.getString("upgrades", "none")
        if (json != "none") game?.let {
            val data: SerializableUpgradeData = Gson().fromJson(json, SerializableUpgradeData::class.java)
            for (upgradeData in data.upgrades)
                upgradeMap[upgradeData.type] = Upgrade.createFromData(it, upgradeData)
        }
        return upgradeMap
    }
}