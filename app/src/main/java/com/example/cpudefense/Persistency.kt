package com.example.cpudefense

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cpudefense.activities.GameActivity
import com.google.gson.Gson
import com.google.gson.JsonParser
import java.io.ByteArrayOutputStream
import androidx.core.content.edit
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.set


class Persistency(private val activity: Activity)
{
    @Suppress("ConstPropertyName")
    companion object
    {
        // DO NOT change the file names,
        // this would break the saved games.
        const val filename_legacy      = "prefs.xml"
        const val filename_settings    = "settings"
        const val filename_structure   = "structure"
        const val filename_thumbnails  = "thumbnails"
        const val filename_saves       = "saves"
        const val filename_state       = "state"
    }

    // define preferences files
    /** file that holds all settings and preferences.
     * For historical reasons, there are some more data in this file, such as:
     *
     */
    private val prefsLegacy: SharedPreferences = activity.getSharedPreferences(filename_legacy, AppCompatActivity.MODE_PRIVATE)
    /** file for the user's preferences and settings */
    @Suppress("unused")
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

    /** Gson instance */
    private val gson = Gson()

    fun saveCurrentLevelState(gameMechanics: GameMechanics)
            /** saves the state of the current level, i.e. the layout of the board, the level of chips,
             * position of attackers, etc., and also their waves.
             */
    {
        prefsSaves.edit {
            val json = gson.toJson(gameMechanics.currentlyActiveStage?.provideData())
            putString("currentstage", json)
        }
    }

    fun saveGeneralState(gameMechanics: GameMechanics)
    /** saves the state of the current level (but not the level layout).
     * This includes current lives, speed control settings, game phase.
     */
    {
        prefsState.edit {
            val json = gson.toJson(gameMechanics.state)
            putString("GENERAL", json)
        }
    }

    fun saveCoins(gameMechanics: GameMechanics)
    {
        val emptyContents = PurseOfCoins.Contents()
        val purseData = SerializablePurseContents(basic = emptyContents, endless = emptyContents)
        gameMechanics.purseOfCoins[GameMechanics.LevelMode.BASIC]?.contents?.let { purse -> purseData.basic = purse }
        gameMechanics.purseOfCoins[GameMechanics.LevelMode.ENDLESS]?.contents?.let { purse -> purseData.endless = purse }
        prefsSaves.edit { putString("coins", gson.toJson(purseData)) }
    }

    fun saveStageSummaries(gameMechanics: GameMechanics, series: Int)
            /** saves all summaries of the levels that belong to the given series
             */
    {
        val editor = prefsSaves.edit()
        val data = when (series)
        {
            GameMechanics.SERIES_NORMAL -> SerializableLevelSummary(gameMechanics.summaryPerNormalLevel)
            GameMechanics.SERIES_TURBO -> SerializableLevelSummary(gameMechanics.summaryPerTurboLevel)
            GameMechanics.SERIES_ENDLESS -> SerializableLevelSummary(gameMechanics.summaryPerEndlessLevel)
            else -> null
        }
        data?.let {
            val json = gson.toJson(it)
            editor.putString(seriesKey[series], json)
            editor.apply()
        }
    }

    private fun saveAllStageSummaries(gameMechanics: GameMechanics?)
    {
        gameMechanics?.let {
            saveStageSummaries(it, GameMechanics.SERIES_NORMAL)
            saveStageSummaries(it, GameMechanics.SERIES_TURBO)
            saveStageSummaries(it, GameMechanics.SERIES_ENDLESS)
        }
    }

    fun saveThumbnailOfLevel(gameActivity: GameActivity, stage: Stage) {
        val editor = prefsThumbnails.edit()
        val levelIdent = stage.data.ident
        if (levelIdent.number != 0) {
            val outputStream = ByteArrayOutputStream()
            val snapshot: Bitmap? = when (levelIdent.series)
            {
                GameMechanics.SERIES_ENDLESS -> gameActivity.levelThumbnailEndless[levelIdent.number]
                else -> gameActivity.levelThumbnail[levelIdent.number]
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

    fun saveHeroes(gameMechanics: GameMechanics)
    {
        // remove deprecated hero saves (prior to 1.34)
        prefsLegacy.edit {
            remove("upgrades")
        }
        prefsSaves.edit {
            val heroData = SerializableHeroDataPerMode()
            gameMechanics.heroesByMode[GameMechanics.LevelMode.BASIC]?.values?.forEach { hero -> heroData.basic.add(hero.data) }
            gameMechanics.heroesByMode[GameMechanics.LevelMode.ENDLESS]?.values?.forEach { hero -> heroData.endless.add(hero.data) }
            putString("heroes", gson.toJson(heroData))
        }
    }

    fun saveHolidays(gameMechanics: GameMechanics) {
        prefsSaves.edit {
            val data = SerializableHolidays(gameMechanics.holidays)
            val json = gson.toJson(data)
            putString("holidays", json)
        }
    }

    fun loadCurrentLevelState(gameMechanics: GameMechanics)
    /** retrieves the state of the current level, i.e. the layout of the board, the level of chips,
     * position of attackers, etc.
     */
    {
        val key = "currentstage"
        val json = prefsSaves.getString(key, "none")
        if (json != "none") {
            gameMechanics.stageData =
                gson.fromJson(json, Stage.Data::class.java)
        }
        prefsLegacy.edit { remove(key); }
    }

    fun loadGeneralState(gameMechanics: GameMechanics, jsonString: String? = null)
            /** loads the state of the current level (but not the level layout).
             * This includes current lives, speed control settings, game phase.
             */
    {
        val key = "GENERAL"
        val json = jsonString ?: prefsState.getString(key, "none")
        if (json != "none") {
            gameMechanics.state =
                gson.fromJson(json, GameMechanics.StateData::class.java)
        }
        prefsLegacy.edit { remove(key);  }
    }

    fun loadStageSummaries(series: Int, jsonString: String? = null): HashMap<Int, Stage.Summary>
            /** loads the summaries for the given series (1, 2, ...).
             * @param series one of GameMechanics.SERIES_NORMAL, _TURBO, _ENDLESS
             * @return the set of all level summaries, or null if none can be found (or the series doesn't exist)
             */
    {
        try {
            val key = seriesKey[series]
            var json = jsonString ?: prefsSaves.getString(key, "none")
            if (json == "none")
            {
                // Migration hack: If the data is not in saves.xml, get it from the legacy file and then delete it there.
                json = prefsLegacy.getString(seriesKey[series], "none")
                // prefsLegacy.edit().let { it.remove(key); it.apply() } // TODO: activate this again in later versions
            }
            val data: SerializableLevelSummary =
                gson.fromJson(json, SerializableLevelSummary::class.java)
            return data.level
        }
        catch (_: Exception)
        {
            return hashMapOf()
        }
    }

    fun loadAllStageSummaries(gameMechanics: GameMechanics, jsonString: String? = null)
    {
        gameMechanics.summaryPerNormalLevel = loadStageSummaries(GameMechanics.SERIES_NORMAL, jsonString)
        gameMechanics.summaryPerTurboLevel = loadStageSummaries(GameMechanics.SERIES_TURBO, jsonString)
        gameMechanics.summaryPerEndlessLevel = loadStageSummaries(GameMechanics.SERIES_ENDLESS, jsonString)
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
            prefsThumbnails.edit { putString(key, encodedString) }
            prefsLegacy.edit { remove(key) }
        } else
            encodedString = prefsThumbnails.getString(key, "")

        // reconstruct bitmap from string saved in preferences
        var snapshot: Bitmap?
        try {
            val decodedBytes: ByteArray = Base64.decode(encodedString, Base64.DEFAULT)
            snapshot = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        }
        catch(_: Exception)
        {
            // unable to get a level snapshot, for whatever reason
            snapshot = null
        }
        return snapshot
    }

    fun loadAllHeroes(gameMechanics: GameMechanics)
    {
        gameMechanics.heroes = loadHeroes(gameMechanics, null)  // legacy, now deprecated
        gameMechanics.heroesByMode[GameMechanics.LevelMode.BASIC] =  loadHeroes(gameMechanics, GameMechanics.LevelMode.BASIC)
        gameMechanics.heroesByMode[GameMechanics.LevelMode.ENDLESS] =  loadHeroes(gameMechanics, GameMechanics.LevelMode.ENDLESS)
    }

    private fun loadHeroes(gameMechanics: GameMechanics, mode: GameMechanics.LevelMode?, jsonString: String? = null): HashMap<Hero.Type, Hero>
            /** gets the heroes from the appropriate save file.
             * @param mode The level series mode (normal or endless). If 'null', get the data from
             * the "old" heroes save file (now deprecated).
              */
    {
        val heroMap = HashMap<Hero.Type, Hero>()
        val file = if (mode == null) prefsLegacy else prefsSaves
        val key = if (mode == null) "upgrades" else "heroes"
        val json = jsonString ?: file.getString(key, "none")
        if (json == "none")
            return heroMap
        try {
            val listOfHeroData: MutableList<Hero.Data> = when (mode) {
                null -> gson.fromJson(json, SerializableHeroData::class.java).upgrades
                GameMechanics.LevelMode.BASIC -> gson.fromJson(json, SerializableHeroDataPerMode::class.java).basic
                GameMechanics.LevelMode.ENDLESS -> gson.fromJson(json, SerializableHeroDataPerMode::class.java).endless
            }
            for (heroData in listOfHeroData) {
                try {
                    heroMap[heroData.type] = Hero.createFromData(activity as GameActivity, heroData)
                } catch (_: NullPointerException) {
                    /* may happen if a previously existing hero type is definitely removed from the game */
                }
            }
            loadHolidays(gameMechanics)
        }
        catch (_: Exception) {
            // save file has not the expected structure
            activity.runOnUiThread {
                Toast.makeText(activity, "Save file corrupted.", Toast.LENGTH_LONG).show()
            }
        }
        return heroMap
    }

    fun loadCoins(gameMechanics: GameMechanics, jsonString: String? = null)
    {
        // get number of coins
        val json = jsonString ?: prefsSaves.getString("coins", "none")
        try {
            if (json != "none") {
                val data: SerializablePurseContents =
                    gson.fromJson(json, SerializablePurseContents::class.java)
                gameMechanics.purseOfCoins[GameMechanics.LevelMode.BASIC]?.let { purse -> purse.contents = data.basic; purse.initialized = true }
                gameMechanics.purseOfCoins[GameMechanics.LevelMode.ENDLESS]?.let { purse -> purse.contents = data.endless; purse.initialized = true }
            }
        }
        catch (_: Exception) {
            // save file has not the expected structure
            activity.runOnUiThread {
                Toast.makeText(activity, "Save file (coins info) corrupted.", Toast.LENGTH_LONG).show()
            }
        }
    }


    private fun loadHolidays(gameMechanics: GameMechanics, jsonString: String? = null)
    {
        val json = jsonString ?: prefsSaves.getString("holidays", "none")
        if (json != "none" && !GameMechanics.resetHeroHolidays) {
            val data: SerializableHolidays =
                gson.fromJson(json, SerializableHolidays::class.java)
            gameMechanics.holidays = data.period
        }
    }

    fun saveLevelStructure(series: Int, data: HashMap<Int, Stage.Data>)
            /** saves the structure data for all 'endless' levels to structure.xml.
             */
    {
        prefsStructure.edit {
            val levelData = SerializableLevelData(data)
            val json = gson.toJson(levelData)
            putString("series_%d".format(series), json)
        }
    }
    fun loadLevelStructure(series: Int, jsonString: String? = null): HashMap<Int, Stage.Data>
    {
        try {
            val json = jsonString ?: prefsStructure.getString("series_%d".format(series), "none")
            if (json == "none")
                return hashMapOf()
            val data: SerializableLevelData =
                gson.fromJson(json, SerializableLevelData::class.java)
            return data.level
        }
        catch (_: Exception)
        {
            return hashMapOf()
        }
    }

    data class SaveFileInfo(
            val gameVersion: String,
            val gameId: String,
            val fileVersion: Int,
            val exportDate: String,
            var maxStage: Int,
            var maxSeries: Int,
            var status: String,
            var turboAvailable: Boolean,
            var endlessAvailable: Boolean,
   )

    fun  prepareGameExport(): String
    {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC") // Zeitzone anpassen

        val gameData = SaveFileInfo(
                gameVersion = BuildConfig.VERSION_NAME,
                gameId = BuildConfig.APPLICATION_ID,
                fileVersion = 1,
                exportDate = dateFormat.format(Date()),
                maxStage = 0,
                maxSeries = 0,
                status = "complete",
                turboAvailable = false,
                endlessAvailable = false,
        )
        val exportData = HashMap<String, Any?>()
        exportData["info"] = gameData
        exportData["state"] = prefsState.all
        exportData["saves"] = prefsSaves.all
        exportData["structure"] = prefsStructure.all
        return gson.toJson(exportData)
    }

    fun gameInfoForForeignVersions(jsonObject: JsonObject): SaveFileInfo?
    /** method save files that do not provide the "info" structure,
     * e.g. from forked game versions */
    {
        return SaveFileInfo(
                gameVersion = "",
                gameId = "",
                fileVersion = 0,
                exportDate = "",
                maxStage = 0,
                maxSeries = 0,
                status = "complete",
                turboAvailable = false,
                endlessAvailable = false,
        )
    }

    fun parseGameImport(jsonString: String): SaveFileInfo?
    {
        var gameInfo: SaveFileInfo? = null
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        jsonObject.get("info")?.let {
            gameInfo = gson.fromJson(it, SaveFileInfo::class.java)
        }
        jsonObject.getAsJsonObject("state")?.let { jsonObject ->
            jsonObject.get("MAXSERIES")?.let { v -> gameInfo?.maxSeries = v.asInt }
            jsonObject.get("MAXSTAGE")?.let { v -> gameInfo?.maxStage = v.asInt }
            jsonObject.get("STATUS")?.let { v -> gameInfo?.status = v.asString }
            jsonObject.get("ENDLESS_AVAILABLE")?.let { v -> gameInfo?.endlessAvailable = v.asBoolean }
            jsonObject.get("TURBO_AVAILABLE")?.let { v -> gameInfo?.turboAvailable = v.asBoolean }
        }
        return gameInfo
    }

    private fun jsonAsString(element: Any?): String?
    /** This is a kind of 'safe cast' to a string which works for both JsonObjects and JsonPrimitives. */
    {
        return element?.let { element ->
            (element as? JsonPrimitive)?.asString ?: (element as? JsonObject)?.toString() }
    }

    fun performGameImport(jsonString: String, gameMechanics: GameMechanics): SaveFileInfo?
            /** reads the data from the file and replaces the respective data in gameMechanics.
             * The information on the current level is discarded.
             * @param jsonString The contents of the save file
             */
    {
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject

        val gameInfo: SaveFileInfo? = parseGameImport(jsonString)

        jsonObject.get("state")?.let {
            // data on the current level is not read (the "GENERAL" part).
        }
        try {
            jsonObject.get("saves")?.let {
                jsonAsString(it.asJsonObject.get(seriesKey[GameMechanics.SERIES_NORMAL]))?.let { jsonString ->
                    gameMechanics.summaryPerNormalLevel = loadStageSummaries(GameMechanics.SERIES_NORMAL, jsonString) }
                jsonAsString(it.asJsonObject.get(seriesKey[GameMechanics.SERIES_TURBO]))?.let { jsonString ->
                    gameMechanics.summaryPerTurboLevel = loadStageSummaries(GameMechanics.SERIES_TURBO, jsonString) }
                jsonAsString(it.asJsonObject.get(seriesKey[GameMechanics.SERIES_ENDLESS]))?.let { jsonString ->
                    gameMechanics.summaryPerEndlessLevel = loadStageSummaries(GameMechanics.SERIES_ENDLESS, jsonString)}
                jsonAsString(it.asJsonObject.get("heroes"))?.let { jsonString ->
                    gameMechanics.heroesByMode[GameMechanics.LevelMode.BASIC] = loadHeroes(gameMechanics, GameMechanics.LevelMode.BASIC, jsonString)
                    gameMechanics.heroesByMode[GameMechanics.LevelMode.ENDLESS] = loadHeroes(gameMechanics, GameMechanics.LevelMode.ENDLESS, jsonString) }
                jsonAsString(it.asJsonObject.get("coins"))?.let { jsonString ->
                    loadCoins(gameMechanics, jsonString) }
                jsonAsString(it.asJsonObject.get("holidays"))?.let { jsonString ->
                    loadHolidays(gameMechanics, jsonString) }
            }
            jsonObject.get("structure")?.let {
                loadLevelStructure(GameMechanics.SERIES_NORMAL, it.asJsonObject?.get(seriesKey[GameMechanics.SERIES_ENDLESS])?.asString)
            }
        } catch (e: Exception) {
            Toast.makeText(activity, activity.getString(R.string.error_reading_file).format(e.message), Toast.LENGTH_LONG).show()
            (activity as? GameActivity)?.logger?.log(activity.getString(R.string.error_reading_file).format(e.message))
        }
        return gameInfo
    }
}