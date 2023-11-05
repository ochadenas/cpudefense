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
    val seriesKey = hashMapOf<Int, String>(
        Game.SERIES_NORMAL to "levels",
        Game.SERIES_TURBO to "levels_series2",
        Game.SERIES_ENDLESS to "levels_endless")

    data class SerializableLevelData (
        val level: HashMap<Int, Stage.Summary> = HashMap()
            )

    data class SerializableThumbnailData (
        val thumbnail: HashMap<Int, String> = HashMap()
    )

    data class SerializableHeroData (
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
            saveHeroes(editor)

            // save level data:
            saveLevels(editor)
        }
    }

    fun loadState(sharedPreferences: SharedPreferences)
    {
        game?.let {
            // get level data
            it.summaryPerNormalLevel = loadLevelSummaries(sharedPreferences, Game.SERIES_NORMAL) ?: HashMap()
            it.summaryPerTurboLevel = loadLevelSummaries(sharedPreferences, Game.SERIES_TURBO) ?: HashMap()
            it.summaryPerEndlessLevel = loadLevelSummaries(sharedPreferences, Game.SERIES_ENDLESS) ?: HashMap()

            // get global data
            it.global = loadGlobalData(sharedPreferences)

            // get upgrades
            it.heroes = loadHeroes(sharedPreferences)

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
            var data = SerializableLevelData(it.summaryPerNormalLevel)
            var json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_NORMAL], json)
            // same for series 2:
            data = SerializableLevelData(it.summaryPerTurboLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_TURBO], json)
            // for endless series:
            data = SerializableLevelData(it.summaryPerEndlessLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_ENDLESS], json)
        }
    }

    fun saveThumbnailOfLevel(editor: SharedPreferences.Editor, stage: Stage) {
        game?.let {
            val levelIdent = stage.data.ident
            if (levelIdent.number != 0) {
                var outputStream = ByteArrayOutputStream()
                var snapshot: Bitmap? = when (levelIdent.series)
                {
                    Game.SERIES_ENDLESS -> it.levelThumbnailEndless[levelIdent.number]
                    else -> it.levelThumbnail[levelIdent.number]
                }
                snapshot?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val encodedImage: String =
                    Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                var key: String = when (levelIdent.series)
                {
                    Game.SERIES_ENDLESS -> "thumbnail_%d_endless".format(levelIdent.number)
                    else -> "thumbnail_%d".format(levelIdent.number)
                }
                editor.putString(key, encodedImage)
            }
        }
    }

    fun saveHeroes(editor: SharedPreferences.Editor)
    {
        game?.let {
            val heroData = SerializableHeroData()
            for (hero in it.heroes.values)
                heroData.upgrades.add(hero.data)
            var json = Gson().toJson(heroData)
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

    fun loadThumbnailOfLevel(sharedPreferences: SharedPreferences, level: Int, series: Int): Bitmap?
    {
        var key: String = when(series)
        {
            Game.SERIES_ENDLESS -> "thumbnail_%d_endless".format(level)
            else -> "thumbnail_%d".format(level)
        }
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

    fun loadHeroes(sharedPreferences: SharedPreferences): HashMap<Hero.Type, Hero>
    {
        val heroMap = HashMap<Hero.Type, Hero>()
        val json = sharedPreferences.getString("upgrades", "none")
        if (json != "none") game?.let {
            val data: SerializableHeroData = Gson().fromJson(json, SerializableHeroData::class.java)
            for (heroData in data.upgrades) {
                try {
                    heroMap[heroData.type] = Hero.createFromData(it, heroData)
                }
                catch(ex: NullPointerException) {
                    /* may happen if a previously existing hero type is definitely removed from the game */
                }
            }
        }
        return heroMap
    }
}