package com.example.cpudefense

import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import kotlin.Exception

class Persistency(var game: Game?) {
    data class SerializableStateData (
        val general: Game.StateData,
        val stage: Stage.Data?
            )

    // designators of the level data in the prefs
    val seriesKey = hashMapOf<Int, String>(1 to "levels", 2 to "levels_series2", 3 to "levels_endless")

    data class SerializableLevelData (
        val level: HashMap<Int, Stage.Summary> = HashMap()
            )

    data class SerializableThumbnailData (
        val thumbnail: HashMap<Int, String> = HashMap()
    )

    data class SerializableUpgradeData (
        val upgrades: MutableList<Hero.Data> = mutableListOf<Hero.Data>()
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
            saveUpgrades(editor)

            // save level data:
            saveLevels(editor)
        }
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        game?.let {
            // get level data
            it.summaryPerLevelOfSeries1 = loadLevelSummaries(sharedPreferences, 1) ?: HashMap()
            it.summaryPerLevelOfSeries2 = loadLevelSummaries(sharedPreferences, 2) ?: HashMap()

            // get global data
            it.global = loadGlobalData(sharedPreferences)

            // get upgrades
            it.gameUpgrades = loadUpgrades(sharedPreferences)

            // get state of running game
            var json = sharedPreferences.getString("state", "none")
            if (json == "none")
                return
            val data: SerializableStateData = Gson().fromJson(json, SerializableStateData::class.java)
            it.state = data.general
            it.stageData = data.stage
            it.gameActivity.setGameSpeed(it.global.speed)  // resture game speed mode
        }
    }

    fun saveLevels(editor: SharedPreferences.Editor)
    {
        game?.let {
            // level summary for series 1:
            var data = SerializableLevelData(it.summaryPerLevelOfSeries1)
            var json = Gson().toJson(data)
            editor.putString(seriesKey[1], json)
            // same for series 2:
            data = SerializableLevelData(it.summaryPerLevelOfSeries2)
            json = Gson().toJson(data)
            editor.putString(seriesKey[2], json)
            // for endless series:
            data = SerializableLevelData(it.summaryPerLevelOfSeries3)
            json = Gson().toJson(data)
            editor.putString(seriesKey[2], json)
        }
    }

    fun saveThumbnailOfLevel(editor: SharedPreferences.Editor, level: Int) {
        game?.let {
            val level = it.currentStage?.getLevel() ?: 0
            if (level != 0) {
                var outputStream = ByteArrayOutputStream()
                var snapshot: Bitmap? = it.levelThumbnail[level]
                snapshot?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val encodedImage: String =
                    Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                var key: String = "thumbnail_%d".format(level)
                editor.putString(key, encodedImage)
            }
        }
    }

    fun saveUpgrades(editor: SharedPreferences.Editor)
    {
        game?.let {
            val upgradeData = SerializableUpgradeData()
            for (upgrade in it.gameUpgrades.values)
                upgradeData.upgrades.add(upgrade.data)
            var json = Gson().toJson(upgradeData)
            editor.putString("upgrades", json)
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

    fun loadLevelSummaries(sharedPreferences: SharedPreferences, series: Int): HashMap<Int, Stage.Summary>?
            /** loads the summaries for the given series (1, 2, ...).
             * @return the set of all level summaries, or null if none can be found (or the series doesn't exist)
             */
    {
        try {
            val json = sharedPreferences.getString(seriesKey[series], "none")
            val data: SerializableLevelData =
                Gson().fromJson(json, SerializableLevelData::class.java)
            return data.level
        }
        catch (e: Exception)
        {
            return null
        }
    }

    fun loadThumbnailOfLevel(sharedPreferences: SharedPreferences, level: Int): Bitmap?
    {
        var key: String = "thumbnail_%d".format(level)
        var encodedString = sharedPreferences.getString(key, "")
        // reconstruct bitmap from string saved in preferences
        var snapshot: Bitmap? = null
        try {
            val decodedBytes: ByteArray = Base64.decode(encodedString, Base64.DEFAULT)
            snapshot = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
        catch(e: Exception)
        {
            // unable to get a level snapshot, for whatever reason
            snapshot = null
        }
        return snapshot
    }

    fun loadUpgrades(sharedPreferences: SharedPreferences): HashMap<Hero.Type, Hero>
    {
        val upgradeMap = HashMap<Hero.Type, Hero>()
        val json = sharedPreferences.getString("upgrades", "none")
        if (json != "none") game?.let {
            val data: SerializableUpgradeData = Gson().fromJson(json, SerializableUpgradeData::class.java)
            for (upgradeData in data.upgrades) {
                try {
                    upgradeMap[upgradeData.type] = Hero.createFromData(it, upgradeData)
                }
                catch(ex: NullPointerException) {
                    /* may happen if a previously existing hero type is definitely removed from the game */
                }
            }
        }
        return upgradeMap
    }
}