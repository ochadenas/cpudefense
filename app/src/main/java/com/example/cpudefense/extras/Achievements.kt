package com.example.cpudefense.extras

import android.content.res.Resources
import com.example.cpudefense.Persistency
import com.example.cpudefense.R
import com.example.cpudefense.activities.GameActivity
import kotlinx.serialization.Serializable

enum class AchievementType { UNDEFINED, LEVEL_LOST, EASTER, CHRISTMAS, PURCHASE_LIFE }

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

data class Achievements (
    val achievements: HashMap<AchievementType, Achievement> = hashMapOf()
)

interface ActivityWithAchievements {
    var achievements: PlayerAchievements
}

class PlayerAchievements
{
    var confirmed = Achievements()

    /** put the achievements here as long as the player has not been notfied. */
    var pending = Achievements()

    fun grant(type: AchievementType)
    {
        val a = confirmed.achievements[type]
        a ?: run {
            val achievement = Achievement(type, binary = true, 1.0f, granted = System.currentTimeMillis())
            pending.achievements[type] = achievement
        }
    }

    fun processPending(gameActivity: GameActivity): List<Achievement>
    {
        val processed = pending.achievements.values.filter { it.fraction >= 1.0f }
        processed.forEach {
            it.fraction = 1.0f
            confirmed.achievements[it.type] = it
            pending.achievements.remove(it.type)
        }
        Persistency(gameActivity).saveAchievements()
        return processed
    }

    /** total number of possible achievements */
    val totalCount = AchievementType.entries.size - 1 /* subtract 1 for UNDEFINED */
    /** number of achievements gained */
    val obtainedCount: Int
        get() { return confirmed.achievements.values.filter { it.fraction >= 1f }.size }
    /** fraction of obtained achievements (of all available) in percent */
    val obtainedPercentage: Float
        get() { return obtainedCount * 100f / totalCount }

    companion object {
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
            }
        }
    }
}