package com.example.cpudefense.extras

import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.google.gson.reflect.TypeToken
import kotlinx.serialization.Serializable

enum class AchievementType { UNDEFINED, LEVEL_LOST, LOSE_BY_COIN, LOSE_BY_HEAT,
    EASTER, CHRISTMAS, PURCHASE_LIFE, SPIN, WIKI, VERSIONINFO,
    INFO_8B, INFO_64B, INFO_1KB, INFO_1KIB, INFO_4KIB,
    TOTAL_64KIB, TOTAL_1MB, TOTAL_1MIB, TOTAL_8MIB, TOTAL_32MIB, TOTAL_100MB, TOTAL_128MIB, TOTAL_1GB, TOTAL_1GIB,
    BANK_0, BANK_4, BANK_8, BANK_MIB, BANK_GIB }

@Serializable
data class Achievement (
    var type: AchievementType = AchievementType.UNDEFINED,
    /**  "binary" means: either the achievement is completed or not. Otherwise, completion is measured in a percentage. */
    var binary: Boolean = true,
    /** the percentage of achievement completion, if applicable */
    var fraction: Float = 0.0f,
    /** timestamp when the achievement hast been obtained, in epoch milliseconds (UTC) */
    var granted: Long? = null
)

interface ActivityWithAchievements {
    var achievements: PlayerAchievements
}

class PlayerAchievements
{
    var confirmed: HashMap<AchievementType, Achievement> = hashMapOf()

    /** put the achievements here as long as the player has not been notfied. */
    var pending: HashMap<AchievementType, Achievement> = hashMapOf()

    fun grant(type: AchievementType)
    {
        val a = confirmed[type]
        a ?: run {
            val achievement = Achievement(type, binary = true, 1.0f, granted = System.currentTimeMillis())
            pending[type] = achievement
        }
    }

    fun processPending(activity: AppCompatActivity): List<Achievement>
    {
        val processed = pending.values.filter { it.fraction >= 1.0f }
        processed.forEach {
            it.fraction = 1.0f
            confirmed[it.type] = it
            pending.remove(it.type)
        }
        Persistency(activity).saveAchievements()
        return processed
    }

    /** total number of possible achievements */
    val totalCount = AchievementType.entries.size - 1 /* subtract 1 for UNDEFINED */
    /** number of achievements gained */
    val obtainedCount: Int
        get() { return confirmed.values.filter { it.fraction >= 1f }.size }
    /** fraction of obtained achievements (of all available) in percent */
    val obtainedPercentage: Float
        get() { return obtainedCount * 100f / totalCount }

    companion object {

        /** type token of the achievements lists (used in serialization) */
        val listType = object : TypeToken<HashMap<AchievementType, Achievement>>() {}.type

        /** retrieves the description of the achievement of given type */
        fun getText(res: Resources, type: AchievementType): String
        {
            return when (type)
            {
                AchievementType.UNDEFINED -> "Undefined Achievement, should not happen"
                AchievementType.LEVEL_LOST -> res.getString(R.string.achieve_LEVEL_LOST)
                AchievementType.EASTER -> res.getString(R.string.achieve_EASTER)
                AchievementType.CHRISTMAS -> res.getString(R.string.achieve_CHRISTMAS)
                AchievementType.PURCHASE_LIFE -> res.getString(R.string.achieve_PURCHASE_LIFE)
                AchievementType.SPIN -> res.getString(R.string.achieve_SPIN)
                AchievementType.WIKI -> res.getString(R.string.achieve_WIKI)
                AchievementType.VERSIONINFO -> res.getString(R.string.achieve_VERSIONINFO)
                AchievementType.BANK_0 -> res.getString(R.string.achieve_BANK_0)
                AchievementType.BANK_4 -> res.getString(R.string.achieve_BANK_4)
                AchievementType.BANK_8 -> res.getString(R.string.achieve_BANK_8)
                AchievementType.BANK_MIB -> res.getString(R.string.achieve_BANK_MIB)
                AchievementType.BANK_GIB -> res.getString(R.string.achieve_BANK_GIB)
                AchievementType.LOSE_BY_COIN -> res.getString(R.string.achieve_LOSE_BY_COIN)
                AchievementType.LOSE_BY_HEAT -> res.getString(R.string.achieve_LOSE_BY_HEAT)
                AchievementType.INFO_8B -> res.getString(R.string.achieve_INFO_8)
                AchievementType.INFO_64B -> res.getString(R.string.achieve_INFO_64B)
                AchievementType.INFO_1KIB -> res.getString(R.string.achieve_INFO_1KIB)
                AchievementType.INFO_4KIB -> res.getString(R.string.achieve_INFO_4KIB)
                AchievementType.INFO_1KB -> res.getString(R.string.achieve_INFO_1KB)
                AchievementType.TOTAL_64KIB -> res.getString(R.string.achieve_TOTAL_64KIB)
                AchievementType.TOTAL_1MB -> res.getString(R.string.achieve_TOTAL_1MB)
                AchievementType.TOTAL_1MIB -> res.getString(R.string.achieve_TOTAL_1MIB)
                AchievementType.TOTAL_8MIB -> res.getString(R.string.achieve_TOTAL_8MIB)
                AchievementType.TOTAL_32MIB -> res.getString(R.string.achieve_TOTAL_32MIB)
                AchievementType.TOTAL_100MB -> res.getString(R.string.achieve_TOTAL_100MB)
                AchievementType.TOTAL_128MIB -> res.getString(R.string.achieve_TOTAL_128MIB)
                AchievementType.TOTAL_1GB -> res.getString(R.string.achieve_TOTAL_1GB)
                AchievementType.TOTAL_1GIB -> res.getString(R.string.achieve_TOTAL_1GIB)
            }
        }
    }
}