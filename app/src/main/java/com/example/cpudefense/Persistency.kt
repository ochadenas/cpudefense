package com.example.cpudefense

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import java.io.ByteArrayOutputStream
import kotlin.Exception

class Persistency(activity: Activity) {

    // define preferences files
    private val prefs: SharedPreferences = activity.getSharedPreferences(activity.getString(R.string.pref_filename),
        AppCompatActivity.MODE_PRIVATE)
    private val prefsStructure: SharedPreferences = activity.getSharedPreferences("structure", AppCompatActivity.MODE_PRIVATE)
    private val prefsThumbnails: SharedPreferences = activity.getSharedPreferences("thumbnails", AppCompatActivity.MODE_PRIVATE)
    private val prefsSaves: SharedPreferences = activity.getSharedPreferences(activity.getString(R.string.pref_filename_savegames), AppCompatActivity.MODE_PRIVATE)
    data class SerializableStateData (
        val general: Game.StateData,
        val stage: Stage.Data?
            )

    // designators of the level data in the prefs
    private val seriesKey = hashMapOf(
        Game.SERIES_NORMAL to "levels",
        Game.SERIES_TURBO to "levels_series2",
        Game.SERIES_ENDLESS to "levels_endless")

    data class SerializableLevelSummary (
        val level: HashMap<Int, Stage.Summary> = HashMap()
            )

    data class SerializableThumbnailData (
        val thumbnail: HashMap<Int, String> = HashMap()
    )

    data class SerializableLevelData (
        val level: HashMap<Int, Stage.Data> = HashMap()
    )

    data class SerializableHeroData (
        val upgrades: MutableList<Hero.Data> = mutableListOf()
    )
    data class SerializableHeroDataPerMode (
        val basic: MutableList<Hero.Data> = mutableListOf(),
        val endless: MutableList<Hero.Data> = mutableListOf()
    )

    data class SerializablePurseContents (
        var basic: PurseOfCoins.Contents,
        var endless: PurseOfCoins.Contents,
    )
    fun saveState(game: Game?)
            /** saves all data that is needed to continue a game later.
             * This includes levels completed and coins got,
             * but also the state of the currently running game.
             */
    {
        val editor = prefs.edit()
        game?.let {
            // save global data:
            var json = Gson().toJson(it.global)
            editor.putString("global", json)

            // save current game state:
            val stateData = SerializableStateData(
                general = it.state,
                stage = it.currentlyActiveStage?.provideData()
            )
            json = Gson().toJson(stateData)
            editor.putString("state", json)

            // save upgrades got so far:
            saveHeroes(it)

            // save level data:
            saveLevels(it)
            editor.commit()

            // save coins in purse:
            val emptyContents = PurseOfCoins.Contents()
            var purseData = SerializablePurseContents(basic = emptyContents, endless = emptyContents)
            game.purseOfCoins[Game.LevelMode.BASIC]?.contents?.let { purse -> purseData.basic = purse }
            game.purseOfCoins[Game.LevelMode.ENDLESS]?.contents?.let { purse -> purseData.endless = purse }
            prefsSaves.edit().putString("coins", Gson().toJson(purseData)).commit()

            // it.coins.values.forEach { purse -> purse.saveContentsOfPurse() }
        }
    }

    fun loadState(game: Game?)
    {
        game?.let {
            // get level data
            it.summaryPerNormalLevel = loadLevelSummaries(Game.SERIES_NORMAL)
            it.summaryPerTurboLevel = loadLevelSummaries(Game.SERIES_TURBO)
            it.summaryPerEndlessLevel = loadLevelSummaries(Game.SERIES_ENDLESS)

            // get global data
            it.global = loadGlobalData()

            // get upgrades
            it.heroes = loadHeroes(it, null)
            it.heroesByMode[Game.LevelMode.BASIC] =  loadHeroes(it, Game.LevelMode.BASIC)
            it.heroesByMode[Game.LevelMode.ENDLESS] =  loadHeroes(it, Game.LevelMode.ENDLESS)

            // get state of running game
            var json = prefs.getString("state", "none")
            if (json != "none") {
                val data: SerializableStateData =
                    Gson().fromJson(json, SerializableStateData::class.java)
                it.state = data.general
                it.stageData = data.stage
                it.gameActivity.setGameSpeed(it.global.speed)  // restore game speed mode
            }

            // load contents of purses
            loadCoins(game)
        }
    }

    private fun saveLevels(game: Game?)
    {
        val editor = prefs.edit()
        game?.let {
            // level summary for series 1:
            var data = SerializableLevelSummary(it.summaryPerNormalLevel)
            var json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_NORMAL], json)
            // same for series 2:
            data = SerializableLevelSummary(it.summaryPerTurboLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_TURBO], json)
            // for endless series:
            data = SerializableLevelSummary(it.summaryPerEndlessLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[Game.SERIES_ENDLESS], json)
            editor.commit()
        }
    }

    fun saveThumbnailOfLevel(game: Game?, stage: Stage) {
        val editor = prefsThumbnails.edit()
        game?.let {
            val levelIdent = stage.data.ident
            if (levelIdent.number != 0) {
                val outputStream = ByteArrayOutputStream()
                val snapshot: Bitmap? = when (levelIdent.series)
                {
                    Game.SERIES_ENDLESS -> it.levelThumbnailEndless[levelIdent.number]
                    else -> it.levelThumbnail[levelIdent.number]
                }
                snapshot?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val encodedImage: String =
                    Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                val key: String = when (levelIdent.series)
                {
                    Game.SERIES_ENDLESS -> "thumbnail_%d_endless".format(levelIdent.number)
                    else -> "thumbnail_%d".format(levelIdent.number)
                }
                editor.putString(key, encodedImage)
                editor.commit()
            }
        }
    }

    fun saveHeroes(game: Game)
    {
        var editor = prefs.edit()
        val upgradesData = SerializableHeroData()
        for (hero in game.heroes.values)
            upgradesData.upgrades.add(hero.data)
        editor.putString("upgrades", Gson().toJson(upgradesData))
        editor.apply()

        editor = prefsSaves.edit()
        val heroData = SerializableHeroDataPerMode()
        game.heroesByMode[Game.LevelMode.BASIC]?.values?.forEach { hero -> heroData.basic.add(hero.data) }
        game.heroesByMode[Game.LevelMode.ENDLESS]?.values?.forEach { hero -> heroData.endless.add(hero.data) }
        editor.putString("heroes", Gson().toJson(heroData))
        editor.apply()
    }

    fun loadGlobalData(): Game.GlobalData
    /** retrieve some global game data, such as total number of coins.
        Saving is done in saveState().
         */
    {
        val json = prefs.getString("global", "none")
        if (json == "none")
            return Game.GlobalData()
        else
            return Gson().fromJson(json, Game.GlobalData::class.java)
    }

    fun loadLevelSummaries(series: Int): HashMap<Int, Stage.Summary>
            /** loads the summaries for the given series (1, 2, ...).
             * @return the set of all level summaries, or null if none can be found (or the series doesn't exist)
             */
    {
        try {
            val json = prefs.getString(seriesKey[series], "none")
            val data: SerializableLevelSummary =
                Gson().fromJson(json, SerializableLevelSummary::class.java)
            return data.level
        }
        catch (e: Exception)
        {
            return hashMapOf()
        }
    }

    fun loadThumbnailOfLevel(level: Int, series: Int): Bitmap?
    {
        val key: String = when(series)
        {
            Game.SERIES_ENDLESS -> "thumbnail_%d_endless".format(level)
            else -> "thumbnail_%d".format(level)
        }
        var encodedString = prefs.getString(key, "")
        if (encodedString != "")
        {
            // migrate into new thumbnails file
            val editorOfNewFile = prefsThumbnails.edit()
            editorOfNewFile.putString(key, encodedString)
            editorOfNewFile.apply()
            val editorOfOldFile = prefs.edit()
            editorOfOldFile.remove(key)
            editorOfOldFile.apply()
        }
        else
            encodedString = prefsThumbnails.getString(key, "")

        // reconstruct bitmap from string saved in preferences
        var snapshot: Bitmap?
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

    fun loadHeroes(game: Game, mode: Game.LevelMode?): HashMap<Hero.Type, Hero>
    {
        val heroMap = HashMap<Hero.Type, Hero>()
        val file = if (mode == null) prefs else prefsSaves
        val key = if (mode == null) "upgrades" else "heroes"
        val json = file.getString(key, "none")
        if (json == "none")
            return heroMap
        try {
            val listOfHeroData: MutableList<Hero.Data> = when (mode) {
                null -> Gson().fromJson(json, SerializableHeroData::class.java).upgrades
                Game.LevelMode.BASIC -> Gson().fromJson(json, SerializableHeroDataPerMode::class.java).basic
                Game.LevelMode.ENDLESS -> Gson().fromJson(json, SerializableHeroDataPerMode::class.java).endless
            }
            for (heroData in listOfHeroData) {
                try {
                    heroMap[heroData.type] = Hero.createFromData(game, heroData)
                } catch (ex: NullPointerException) {
                    /* may happen if a previously existing hero type is definitely removed from the game */
                }
            }
        }
        catch (ex: Exception) {
            // save file has not the expected structure
            game.gameActivity.runOnUiThread {
                Toast.makeText(game.gameActivity, "Save file corrupted.", Toast.LENGTH_LONG).show()
            }
        }
        return heroMap
    }

    fun loadCoins(game: Game)
    {
        // get number of coins
        val json = prefsSaves.getString("coins", "none")
        if (json != "none") {
            val data: SerializablePurseContents =
                Gson().fromJson(json, SerializablePurseContents::class.java)
            game.purseOfCoins[Game.LevelMode.BASIC]?.let { purse -> purse.contents = data.basic; purse.initialized = true }
            game.purseOfCoins[Game.LevelMode.ENDLESS]?.let { purse -> purse.contents = data.endless; purse.initialized = true }
        }
    }

    fun saveLevelStructure(series: Int, data: HashMap<Int, Stage.Data>)
            /** saves the structure data for all 'endless' levels to structure.xml.
             */
    {
        val editor = prefsStructure.edit()
        val levelData = SerializableLevelData(data)
        val json = Gson().toJson(levelData)
        editor.putString("series_%d".format(series), json)
        editor.apply()
    }
    fun loadLevelStructure(series: Int): HashMap<Int, Stage.Data>
    {
        try {
            val json = prefsStructure.getString("series_%d".format(series), "none")
            if (json == "none")
                return hashMapOf()
            val data: SerializableLevelData =
                Gson().fromJson(json, SerializableLevelData::class.java)
            return data.level
        }
        catch (e: Exception)
        {
            return hashMapOf()
        }
    }
}