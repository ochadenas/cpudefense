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

class Persistency(private var activity: Activity)
{
    companion object
    {
        // DO NOT change the file names,
        // this would break the saved games.
        val filename_legacy      = "prefs.xml"
        val filename_settings    = "settings"
        val filename_structure   = "structure"
        val filename_thumbnails  = "thumbnails"
        val filename_saves       = "saves"
        val filename_state       = "state"
    }

    // define preferences files
    /** file that holds all settings and preferences.
     * For historical reasons, there are some more data in this file, such as:
     *
     */
    private val prefsLegacy: SharedPreferences = activity.getSharedPreferences(filename_legacy, AppCompatActivity.MODE_PRIVATE)
    /** file for the user's preferences and settings */
    private val prefsSettings: SharedPreferences = activity.getSharedPreferences(filename_settings, AppCompatActivity.MODE_PRIVATE)
    /** file that holds the structure of levels without any attackers. Used in ENDLESS series. */
    private val prefsStructure: SharedPreferences = activity.getSharedPreferences(filename_structure, AppCompatActivity.MODE_PRIVATE)
    /** file for the small thumbnails that are displayed in the level selector */
    private val prefsThumbnails: SharedPreferences = activity.getSharedPreferences(filename_thumbnails, AppCompatActivity.MODE_PRIVATE)
    /** file for the overall game progress, such as heroes or coins in the purse */
    private val prefsSaves: SharedPreferences = activity.getSharedPreferences(filename_saves, AppCompatActivity.MODE_PRIVATE)
    /** file for the state within the current level, such as attacker and chip positions, number of waves, etc. */
    private val prefsState: SharedPreferences = activity.getSharedPreferences(filename_state, AppCompatActivity.MODE_PRIVATE)

    data class SerializableStateData (
        val general: GameMechanics.StateData,
        val stage: Stage.Data?
            )

    // designators of the level data in the prefs
    private val seriesKey = hashMapOf(
            GameMechanics.SERIES_NORMAL to "levels",
            GameMechanics.SERIES_TURBO to "levels_series2",
            GameMechanics.SERIES_ENDLESS to "levels_endless")

    data class SerializableLevelSummary (
        val level: HashMap<Int, Stage.Summary> = HashMap()
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
    data class SerializableHolidays (
        val period: HashMap<Int, Hero.Holiday> = HashMap(),
    )

    fun saveLevelState(gameMechanics: GameMechanics)
            /** saves the state of the current level, i.e. the layout of the board, the level of chips,
             * position of attackers, etc.
             */
    {


    }

    fun saveState(gameMechanics: GameMechanics?)
            /** saves all data that is needed to continue a game later.
             * This includes levels completed and coins got,
             * but also the state of the currently running game.
             */
    {
        val editor = prefsLegacy.edit()
        gameMechanics?.let {
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
            editor.apply()

            // save coins in purse:
            val emptyContents = PurseOfCoins.Contents()
            val purseData = SerializablePurseContents(basic = emptyContents, endless = emptyContents)
            gameMechanics.purseOfCoins[GameMechanics.LevelMode.BASIC]?.contents?.let { purse -> purseData.basic = purse }
            gameMechanics.purseOfCoins[GameMechanics.LevelMode.ENDLESS]?.contents?.let { purse -> purseData.endless = purse }
            prefsSaves.edit().putString("coins", Gson().toJson(purseData)).commit()
        }
    }

    fun loadState(gameMechanics: GameMechanics?)
    {
        gameMechanics?.let {
            // get level data
            it.summaryPerNormalLevel = loadLevelSummaries(GameMechanics.SERIES_NORMAL)
            it.summaryPerTurboLevel = loadLevelSummaries(GameMechanics.SERIES_TURBO)
            it.summaryPerEndlessLevel = loadLevelSummaries(GameMechanics.SERIES_ENDLESS)

            // get global data
            it.global = loadGlobalData()

            // get upgrades
            it.heroes = loadHeroes(it, null)
            it.heroesByMode[GameMechanics.LevelMode.BASIC] =  loadHeroes(it, GameMechanics.LevelMode.BASIC)
            it.heroesByMode[GameMechanics.LevelMode.ENDLESS] =  loadHeroes(it, GameMechanics.LevelMode.ENDLESS)

            // get state of running game
            val json = prefsLegacy.getString("state", "none")
            if (json != "none") {
                val data: SerializableStateData =
                    Gson().fromJson(json, SerializableStateData::class.java)
                it.state = data.general
                it.stageData = data.stage
                (activity as GameActivity).setGameSpeed(it.state.speed)  // restore game speed mode
            }

            // load contents of purses
            loadCoins(gameMechanics)
        }
    }

    private fun saveLevels(gameMechanics: GameMechanics?)
    {
        val editor = prefsSaves.edit()
        gameMechanics?.let {
            // level summary for series 1:
            var data = SerializableLevelSummary(it.summaryPerNormalLevel)
            var json = Gson().toJson(data)
            editor.putString(seriesKey[GameMechanics.SERIES_NORMAL], json)
            // same for series 2:
            data = SerializableLevelSummary(it.summaryPerTurboLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[GameMechanics.SERIES_TURBO], json)
            // for endless series:
            data = SerializableLevelSummary(it.summaryPerEndlessLevel)
            json = Gson().toJson(data)
            editor.putString(seriesKey[GameMechanics.SERIES_ENDLESS], json)
            editor.commit()
        }
    }

    fun saveThumbnailOfLevel(gameMechanics: GameMechanics?, stage: Stage) {
        val editor = prefsThumbnails.edit()
        gameMechanics?.let {
            val levelIdent = stage.data.ident
            if (levelIdent.number != 0) {
                val outputStream = ByteArrayOutputStream()
                val snapshot: Bitmap? = when (levelIdent.series)
                {
                    GameMechanics.SERIES_ENDLESS -> it.levelThumbnailEndless[levelIdent.number]
                    else -> it.levelThumbnail[levelIdent.number]
                }
                snapshot?.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                val encodedImage: String =
                    Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
                val key: String = when (levelIdent.series)
                {
                    GameMechanics.SERIES_ENDLESS -> "thumbnail_%d_endless".format(levelIdent.number)
                    else -> "thumbnail_%d".format(levelIdent.number)
                }
                editor.putString(key, encodedImage)
                editor.apply()
            }
        }
    }

    fun saveHeroes(gameMechanics: GameMechanics)
    {
        // remove deprecated hero saves (prior to 1.34)
        var editor = prefsLegacy.edit()
        editor.remove("upgrades")
        editor.apply()

        editor = prefsSaves.edit()
        val heroData = SerializableHeroDataPerMode()
        gameMechanics.heroesByMode[GameMechanics.LevelMode.BASIC]?.values?.forEach { hero -> heroData.basic.add(hero.data) }
        gameMechanics.heroesByMode[GameMechanics.LevelMode.ENDLESS]?.values?.forEach { hero -> heroData.endless.add(hero.data) }
        editor.putString("heroes", Gson().toJson(heroData))
        editor.apply()
    }

    fun saveHolidays(gameMechanics: GameMechanics)
    {
        val editor = prefsSaves.edit()
        val data = SerializableHolidays(gameMechanics.holidays)
        val json = Gson().toJson(data)
        editor.putString("holidays", json)
        editor.apply()
    }

    fun loadGlobalData(): GameMechanics.GlobalData
    /** retrieve some global game data, such as total number of coins.
        Saving is done in saveState().
         */
    {
        val json = prefsLegacy.getString("global", "none")
        if (json == "none")
            return GameMechanics.GlobalData()
        else
            return Gson().fromJson(json, GameMechanics.GlobalData::class.java)
    }

    fun loadLevelSummaries(series: Int): HashMap<Int, Stage.Summary>
            /** loads the summaries for the given series (1, 2, ...).
             * @return the set of all level summaries, or null if none can be found (or the series doesn't exist)
             */
    {
        try {
            val key = seriesKey[series]
            var json = prefsSaves.getString(key, "none")
            if (json == "none")
            {
                // Migration hack: If the data is not in saves.xml, get it from the legacy file and then delete it there.
                json = prefsLegacy.getString(seriesKey[series], "none")
            }
            prefsLegacy.edit().let { it.remove(key); it.apply() }
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
            GameMechanics.SERIES_ENDLESS -> "thumbnail_%d_endless".format(level)
            else -> "thumbnail_%d".format(level)
        }
        var encodedString = prefsLegacy.getString(key, "")
        if (encodedString != "")
        {
            // migrate into new thumbnails file
            val editorOfNewFile = prefsThumbnails.edit()
            editorOfNewFile.putString(key, encodedString)
            editorOfNewFile.apply()
            val editorOfOldFile = prefsLegacy.edit()
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

    fun loadHeroes(gameMechanics: GameMechanics, mode: GameMechanics.LevelMode?): HashMap<Hero.Type, Hero>
            /** gets the heroes from the appropriate save file.
             * @param mode The level series moder (normal or endless). If 'null', get teh data from
             * the "old" heroes save file (now deprecated).
              */
    {
        val heroMap = HashMap<Hero.Type, Hero>()
        val file = if (mode == null) prefsLegacy else prefsSaves
        val key = if (mode == null) "upgrades" else "heroes"
        val json = file.getString(key, "none")
        if (json == "none")
            return heroMap
        try {
            val listOfHeroData: MutableList<Hero.Data> = when (mode) {
                null -> Gson().fromJson(json, SerializableHeroData::class.java).upgrades
                GameMechanics.LevelMode.BASIC -> Gson().fromJson(json, SerializableHeroDataPerMode::class.java).basic
                GameMechanics.LevelMode.ENDLESS -> Gson().fromJson(json, SerializableHeroDataPerMode::class.java).endless
            }
            for (heroData in listOfHeroData) {
                try {
                    heroMap[heroData.type] = Hero.createFromData(activity as GameActivity, heroData)
                } catch (ex: NullPointerException) {
                    /* may happen if a previously existing hero type is definitely removed from the game */
                }
            }
            loadHolidays(gameMechanics)
        }
        catch (ex: Exception) {
            // save file has not the expected structure
            activity.runOnUiThread {
                Toast.makeText(activity, "Save file corrupted.", Toast.LENGTH_LONG).show()
            }
        }
        return heroMap
    }

    fun loadCoins(gameMechanics: GameMechanics)
    {
        // get number of coins
        val json = prefsSaves.getString("coins", "none")
        if (json != "none") {
            val data: SerializablePurseContents =
                Gson().fromJson(json, SerializablePurseContents::class.java)
            gameMechanics.purseOfCoins[GameMechanics.LevelMode.BASIC]?.let { purse -> purse.contents = data.basic; purse.initialized = true }
            gameMechanics.purseOfCoins[GameMechanics.LevelMode.ENDLESS]?.let { purse -> purse.contents = data.endless; purse.initialized = true }
        }
    }


    private fun loadHolidays(gameMechanics: GameMechanics)
    {
        val json = prefsSaves.getString("holidays", "none")
        if (json != "none" && !GameMechanics.resetHeroHolidays) {
            val data: SerializableHolidays =
                Gson().fromJson(json, SerializableHolidays::class.java)
            gameMechanics.holidays = data.period
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