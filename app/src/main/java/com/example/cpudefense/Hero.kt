@file:Suppress("DEPRECATION", "SpellCheckingInspection")

package com.example.cpudefense

import android.content.res.Resources
import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.activities.GameActivity
import com.example.cpudefense.effects.Fader
import com.example.cpudefense.gameElements.Button
import com.example.cpudefense.gameElements.HeroCard
import com.example.cpudefense.utils.setTopLeft
import kotlin.math.exp
import kotlin.math.truncate

class Hero(var gameActivity: GameActivity, type: Type)
/** class representing the various personalities in the game, and the effect
 * they have on game play.
 * @param gameActivity Reference to the main game object. Mainly needed for access to resources.
 * @param type Type of the hero (main key for all hero attributes)
 */
{
    /*
    potential Heroes for versions to come:
    - Babbage?
    - Berners-Lee?
    - Torvalds
    - Baudot
    - Auguste Kerckhoff?
    - Al Chwarizmi
    - Goldstine (he & she)
    - Hoare
     */

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SUB_RANGE, DOUBLE_HIT_SUB,
        INCREASE_CHIP_SHR_SPEED,  INCREASE_CHIP_SHR_RANGE, DOUBLE_HIT_SHR,
        INCREASE_CHIP_MEM_SPEED,  INCREASE_CHIP_MEM_RANGE, ENABLE_MEM_UPGRADE,
        INCREASE_CHIP_RES_STRENGTH, INCREASE_CHIP_RES_DURATION, CONVERT_HEAT,
        DECREASE_ATT_FREQ, DECREASE_ATT_SPEED, DECREASE_ATT_STRENGTH, DECREASE_COIN_STRENGTH, REDUCE_HEAT,
        ADDITIONAL_LIVES, INCREASE_MAX_HERO_LEVEL, LIMIT_UNWANTED_CHIPS, CREATE_ADDITIONAL_CHIPS,
        INCREASE_STARTING_CASH, GAIN_CASH,
        DECREASE_UPGRADE_COST, INCREASE_REFUND, GAIN_CASH_ON_KILL, DECREASE_REMOVAL_COST}

    data class Data (
        /** type of the hero, actually its name */
        val type: Type,
        /** upgrade level */
        var level: Int = 0,
        /** coins spent for all upgrades so far */
        var coinsSpent: Int = 0,
    )
    var data = Data(type = type)
    
    val resources: Resources = gameActivity.resources

    /** only for the current level: whether hero is on leave */
    var isOnLeave = false

    /* string variables, will be overwritten later */
    var shortDesc: String = "effect description"
    var strengthDesc: String = "format string"
    var upgradeDesc: String = " → next level"
    private var costDesc: String = "[cost: ]"

    /** reference to the person data of this hero */
    var person = Person(type)

    var biography: Biography? = null
    var effect: String = ""
    var vitae: String = ""

    /** hero cannot upgraded beyond this level. This value can be modified for certain heroes,
     * or by the effect of Sid Meier
      */
    private var maxLevel = 7

    /** reference to the graphical representation of this hero */
    var card = HeroCard(gameActivity.gameView, this)

    fun createBiography(area: Rect)
    /** create the biography object if it does not exist */
    {
        if (biography == null)
            biography = Biography(area)
        biography?.createBiography(this)

    }

    fun setDesc()
    /** sets the description string of this hero, depending
     * on the type and its upgrade level.
     */
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SUB_SPEED -> {
                shortDesc = resources.getString(R.string.shortdesc_SUB)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → x %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = resources.getString(R.string.shortdesc_startinfo)
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " → %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHR_SPEED ->             {
                shortDesc = resources.getString(R.string.shortdesc_SHR)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_SPEED -> {
                shortDesc = resources.getString(R.string.shortdesc_MEM)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_RES_STRENGTH -> {
                shortDesc = resources.getString(R.string.shortdesc_RES)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_RES_DURATION -> {
                shortDesc = resources.getString(R.string.shortdesc_duration)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.REDUCE_HEAT -> {
                shortDesc = resources.getString(R.string.shortdesc_heat)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.DECREASE_UPGRADE_COST -> {
                shortDesc = resources.getString(R.string.shortdesc_upgrade)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.ADDITIONAL_LIVES -> {
                shortDesc = resources.getString(R.string.shortdesc_lives)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " → %d".format(next.toInt())
                maxLevel = 3
            }
            Type.DECREASE_ATT_FREQ -> {
                shortDesc = resources.getString(R.string.shortdesc_frequency)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_SPEED -> {
                shortDesc = resources.getString(R.string.shortdesc_att_speed)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_COIN_STRENGTH -> {
                shortDesc = resources.getString(R.string.shortdesc_coin_strength)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_MAX_HERO_LEVEL ->
            {
                shortDesc = resources.getString(R.string.shortdesc_max_hero_upgrade)
                strengthDesc = "+%d".format(strength.toInt())
                upgradeDesc = " → +%d".format(next.toInt())
                maxLevel = 3
            }
            Type.LIMIT_UNWANTED_CHIPS ->
            {
                shortDesc = resources.getString(R.string.shortdesc_limit_unwanted)
                strengthDesc = "-%d".format(strength.toInt())
                upgradeDesc = " → -%d".format(next.toInt())
            }
            Type.CREATE_ADDITIONAL_CHIPS ->
            {
                shortDesc = resources.getString(R.string.shortdesc_create_wanted)
                strengthDesc = "+%d".format(strength.toInt())
                upgradeDesc = " → +%d".format(next.toInt())
            }
            Type.ENABLE_MEM_UPGRADE ->
            {
                shortDesc = resources.getString(R.string.shortdesc_enable_mem_upgrade)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " → %d".format(next.toInt())
                maxLevel = GameMechanics.maxInternalChipStorage - 1
            }
            Type.GAIN_CASH ->
            {
                shortDesc = resources.getString(R.string.shortdesc_info_gain)
                strengthDesc = "1 bit/%d ticks".format(strength.toInt())
                upgradeDesc = " → 1/%d ticks".format(next.toInt())
            }
            Type.GAIN_CASH_ON_KILL ->
            {
                shortDesc = resources.getString(R.string.shortdesc_info_on_kill)
                strengthDesc = "%d bit/kill".format(strength.toInt())
                upgradeDesc = " → %d bit/kill".format(next.toInt())
            }
            Type.INCREASE_REFUND ->
            {
                shortDesc = resources.getString(R.string.shortdesc_refund)
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
                maxLevel = 5  // even at level 6, refund is more than 100%
            }
            Type.INCREASE_CHIP_SUB_RANGE ->
            {
                shortDesc = resources.getString(R.string.shortdesc_range).format("SUB")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_SHR_RANGE ->
            {
                shortDesc = resources.getString(R.string.shortdesc_range).format("SHR")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_RANGE ->
            {
                shortDesc = resources.getString(R.string.shortdesc_range).format("MEM")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_STRENGTH -> {
                shortDesc = resources.getString(R.string.shortdesc_att_strength)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_REMOVAL_COST -> {
                shortDesc = resources.getString(R.string.shortdesc_reduce_removal)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.CONVERT_HEAT -> {
                shortDesc = resources.getString(R.string.shortdesc_heat_conversion)
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
            }
            Type.DOUBLE_HIT_SUB -> {
                shortDesc = resources.getString(R.string.shortdesc_double_chance).format("SUB")
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
            }
            Type.DOUBLE_HIT_SHR -> {
                shortDesc = resources.getString(R.string.shortdesc_double_chance).format("SHR")
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
            }
        }
        val cost = getPrice(data.level)
        costDesc = resources.getString(R.string.cost_desc).format(cost)
        if (data.level >= getMaxUpgradeLevel()) {
            upgradeDesc = ""
            costDesc = ""
        }
    }


    fun getStrength(level: Int = data.level): Float
    /** determines the numerical effect ("strength") of the upgrade,
     * depending on its level
     */
    {
        return getStrengthOfType(data.type, level)
    }

    fun getMaxUpgradeLevel(): Int
    /** @return The maximal allowed upgrade level for this hero,
     * taking into account the type of the card and
     * the possible effect of Sid Meier
     */
    {
        val additionalUpgradePossibility = gameActivity.gameMechanics.heroModifier(Type.INCREASE_MAX_HERO_LEVEL).toInt()
        if (data.type in
            listOf( Type.ADDITIONAL_LIVES, Type.INCREASE_MAX_HERO_LEVEL, Type.GAIN_CASH, Type.INCREASE_REFUND, Type.ENABLE_MEM_UPGRADE))
            return maxLevel
        else
            return maxLevel + additionalUpgradePossibility
    }

    private fun upgradeLevel(type: Type): Int
    /** gets the upgrade level of a hero (different from this one)
     * @param type The hero's type */
    {
        val level = gameActivity.gameMechanics.currentHeroes()[type]?.data?.level
        return level ?: 0
    }

    fun isAvailable(stageIdentifier: Stage.Identifier): Boolean
    /** function that evaluates certain restrictions on upgrades.
     * Some upgrades require others to reach a certain level, etc.
     * @param stageIdentifier cards may depend on the stage and/or series
     */
    {
        return when (data.type) {
            Type.LIMIT_UNWANTED_CHIPS ->    upgradeLevel(Type.INCREASE_MAX_HERO_LEVEL) >= 3
            Type.CREATE_ADDITIONAL_CHIPS -> upgradeLevel(Type.LIMIT_UNWANTED_CHIPS) >= 3
            Type.INCREASE_MAX_HERO_LEVEL -> upgradeLevel(Type.ADDITIONAL_LIVES) >= 3
            Type.DECREASE_COIN_STRENGTH ->  upgradeLevel(Type.DECREASE_ATT_STRENGTH) >= 3
            Type.DECREASE_ATT_STRENGTH ->   upgradeLevel(Type.DECREASE_ATT_SPEED) >= 3
            Type.DECREASE_ATT_SPEED ->      upgradeLevel(Type.DECREASE_ATT_FREQ) >= 5
            Type.ADDITIONAL_LIVES ->        upgradeLevel(Type.DECREASE_ATT_FREQ) >= 3
            Type.DECREASE_ATT_FREQ ->       upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 3
            Type.GAIN_CASH_ON_KILL ->       upgradeLevel(Type.INCREASE_REFUND) >= 3
            Type.INCREASE_REFUND ->         upgradeLevel(Type.DECREASE_UPGRADE_COST) >= 3
            Type.DECREASE_UPGRADE_COST ->   upgradeLevel(Type.GAIN_CASH) >= 3
            Type.GAIN_CASH ->               upgradeLevel(Type.INCREASE_STARTING_CASH) >= 3
            Type.DECREASE_REMOVAL_COST ->   upgradeLevel(Type.GAIN_CASH) >= 3 && stageIdentifier.number >= 24
            Type.REDUCE_HEAT ->             upgradeLevel(Type.INCREASE_CHIP_MEM_SPEED) >= 3
            Type.INCREASE_CHIP_MEM_SPEED -> stageIdentifier.number >= 14
            Type.INCREASE_CHIP_SUB_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SUB_SPEED) >= 5
            Type.INCREASE_CHIP_SHR_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 5
            Type.INCREASE_CHIP_MEM_RANGE -> upgradeLevel(Type.INCREASE_CHIP_MEM_SPEED) >= 5
            Type.ENABLE_MEM_UPGRADE ->      upgradeLevel(Type.INCREASE_CHIP_MEM_RANGE) >= 3
            Type.INCREASE_CHIP_RES_STRENGTH -> stageIdentifier.number >= 32
            Type.INCREASE_CHIP_RES_DURATION -> upgradeLevel(Type.INCREASE_CHIP_RES_STRENGTH) >= 3
            Type.CONVERT_HEAT           -> upgradeLevel(Type.INCREASE_CHIP_RES_DURATION) >= 3
            Type.DOUBLE_HIT_SUB          -> upgradeLevel(Type.INCREASE_CHIP_SUB_RANGE) >= 3
            Type.DOUBLE_HIT_SHR          -> upgradeLevel(Type.INCREASE_CHIP_SHR_RANGE) >= 3
            else -> true
        }
    }

    fun getPrice(level: Int): Int
    /**
     * Cost of next hero upgrade.
     * @param level The current level of the hero
     * @return the cost (in coins) for reaching the next level
     */
    {
        return if (level == 0) 1 else level
    }

    @Suppress("unused")
    fun upgradeInfo(): String
    /** displays a text with info on the next available upgrade */
    {
        return "%s %s\n%s %s".format(shortDesc, strengthDesc, upgradeDesc, costDesc)
    }

    fun doUpgrade()
    /** actually do a rise in level, including starting the animation */
    {
        if (data.level >= getMaxUpgradeLevel())
            return
        data.level += 1
        setDesc()
        card.upgradeAnimation()
    }

    fun doDowngrade()
    /** actually do lowering of a level, including starting the animation */
    {
        if (data.level <= 0)
            return
        data.level -= 1
        Persistency(gameActivity).saveHeroes(gameActivity.gameMechanics)
        card.downgradeAnimation()
    }

    fun resetUpgrade()
    /** sets the level to 0 */
    {
        data.level = 0
        data.coinsSpent = 0
        card.heroOpacity = 0f
        setDesc()
    }

    companion object {
        fun createFromData(gameActivity: GameActivity, data: Data): Hero
                /** reconstruct a Hero object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Hero(gameActivity, data.type)
            newInstance.data.level = data.level
            newInstance.data.coinsSpent = data.coinsSpent
            newInstance.person.setType()
            newInstance.card.heroOpacity = when (data.level) { 0 -> 0f else -> 1f}
            newInstance.setDesc()
            newInstance.isOnLeave = newInstance.isOnLeave(gameActivity.gameMechanics.currentStageIdent)
            return newInstance
        }

        fun getStrengthOfType(type: Type, level: Int = 0): Float
                /** determines the numerical effect ("strength") of
                 * a hero of the given type, depending on its level.
                 * "level = 0" corresponds to "hero not present".
                 */
        {
            when (type) {
                Type.INCREASE_CHIP_SUB_SPEED -> return 1.0f + level / 20f
                Type.INCREASE_CHIP_SHR_SPEED -> return 1.0f + level / 20f
                Type.INCREASE_CHIP_MEM_SPEED -> return 1.0f + level / 20f
                Type.INCREASE_STARTING_CASH -> return GameMechanics.minimalAmountOfCash.toFloat() + level * level
                Type.REDUCE_HEAT -> return level * 10f
                Type.DECREASE_UPGRADE_COST -> return level * 6f
                Type.DECREASE_REMOVAL_COST -> return level * 8f
                Type.ADDITIONAL_LIVES -> return level.toFloat()
                Type.DECREASE_ATT_FREQ -> return 1.0f - level * 0.05f
                Type.DECREASE_ATT_SPEED -> return 1.0f - level * 0.04f
                Type.DECREASE_ATT_STRENGTH -> return exp(- level / 3.0).toFloat()
                Type.DECREASE_COIN_STRENGTH -> return 1.0f - level * 0.05f
                Type.INCREASE_MAX_HERO_LEVEL -> return level.toFloat()
                Type.LIMIT_UNWANTED_CHIPS -> return level.toFloat()
                Type.CREATE_ADDITIONAL_CHIPS -> return level.toFloat()
                Type.ENABLE_MEM_UPGRADE -> return (level+1).toFloat()
                Type.GAIN_CASH -> return if (level>0) (8f - level) * 9 else 0f
                Type.GAIN_CASH_ON_KILL -> return truncate((level+1) * 0.5f)
                Type.INCREASE_REFUND -> return (50f + level * 10)
                Type.INCREASE_CHIP_SUB_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_SHR_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_MEM_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_RES_STRENGTH -> return 1.0f + level * 0.2f
                Type.INCREASE_CHIP_RES_DURATION -> return 1.0f + level * 0.2f
                Type.CONVERT_HEAT -> return level * 3f
                Type.DOUBLE_HIT_SUB -> return if (level < 10) level * 10f else 100f
                Type.DOUBLE_HIT_SHR -> return if (level < 10) level * 10f else 100f
                else -> return level.toFloat()
            }
        }
    }

    inner class Person(var type: Type)
    /** data related to the historical person behind the hero, such as description, photo, cv */
    {
        var name = ""
        var fullName = ""
        /** the hero's photo */
        var picture: Bitmap? = null
        /** link to the hero's wikipedia article */
        var url = ""

        fun setType()
        {
            when (type)
            {
                Type.INCREASE_CHIP_SUB_SPEED ->
                {
                    name = "Turing"
                    fullName = "Alan Turing"
                    effect = resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SUB")
                    vitae = resources.getString(R.string.turing)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.turing)
                }
                Type.INCREASE_CHIP_SHR_SPEED ->
                {
                    name = "Lovelace"
                    fullName = "Ada Lovelace"
                    effect = resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SHR")
                    vitae = resources.getString(R.string.lovelace)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.lovelace)
                }
                Type.INCREASE_CHIP_MEM_SPEED ->
                {
                    name = "Knuth"
                    fullName = "Donald E. Knuth"
                    effect = resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("MEM")
                    vitae = resources.getString(R.string.knuth)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.knuth)
                }
                Type.REDUCE_HEAT ->
                {
                    name = "Chappe"
                    fullName = "Claude Chappe"
                    effect = resources.getString(R.string.HERO_EFFECT_HEAT)
                    vitae = resources.getString(R.string.chappe)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.chappe)
                }
                Type.INCREASE_STARTING_CASH ->
                {
                    name = "Hollerith"
                    fullName = "Herman Hollerith"
                    effect = resources.getString(R.string.HERO_EFFECT_STARTINFO)
                    vitae = resources.getString(R.string.hollerith)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.hollerith)
                }
                Type.DECREASE_UPGRADE_COST ->
                {
                    name = "Osborne"
                    fullName = "Adam Osborne"
                    effect = resources.getString(R.string.HERO_EFFECT_UPGRADECOST)
                    vitae = resources.getString(R.string.osborne)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.osborne)
                }
                Type.ADDITIONAL_LIVES ->
                {
                    name = "Zuse"
                    fullName = "Konrad Zuse"
                    effect = resources.getString(R.string.HERO_EFFECT_LIVES)
                    vitae = resources.getString(R.string.zuse)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.zuse)
                }
                Type.LIMIT_UNWANTED_CHIPS ->
                {
                    name = "Kilby"
                    fullName = "Jack Kilby"
                    effect = resources.getString(R.string.HERO_EFFECT_LIMITUNWANTED)
                    vitae = resources.getString(R.string.kilby)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.kilby)
                }
                Type.CREATE_ADDITIONAL_CHIPS ->
                {
                    name = "Neumann"
                    fullName = "John von Neumann"
                    effect = resources.getString(R.string.HERO_CREATE_CHIPS)
                    vitae = resources.getString(R.string.neumann)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.neumann)
                }
                Type.ENABLE_MEM_UPGRADE ->
                {
                    name = "Leibniz"
                    fullName = "Gottfried Wilhelm Leibniz"
                    effect = resources.getString(R.string.HERO_EFFECT_ENABLEMEM)
                    vitae = resources.getString(R.string.leibniz)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.leibniz)
                }
                Type.DECREASE_ATT_FREQ ->
                {
                    name = "LHC"
                    fullName = "Les Horribles Cernettes"
                    effect = resources.getString(R.string.HERO_EFFECT_FREQUENCY)
                    vitae = resources.getString(R.string.cernettes)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.cernettes)
                }
                Type.DECREASE_COIN_STRENGTH ->
                {
                    name = "Diffie"
                    fullName = "Whit Diffie"
                    effect = resources.getString(R.string.HERO_EFFECT_COINSTRENGTH)
                    vitae = resources.getString(R.string.diffie)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.diffie)
                }
                Type.GAIN_CASH ->
                {
                    name = "Franke"
                    fullName = "Herbert W. Franke"
                    effect = resources.getString(R.string.HERO_EFFECT_INFOOVERTIME)
                    vitae = resources.getString(R.string.franke)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.franke)
                }
                Type.GAIN_CASH_ON_KILL ->
                {
                    name = "Mandelbrot"
                    fullName = "Benoît B. Mandelbrot"
                    effect = resources.getString(R.string.HERO_EFFECT_GAININFO)
                    vitae = resources.getString(R.string.mandelbrot)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.mandelbrot)
                }
                Type.DECREASE_REMOVAL_COST ->
                {
                    name = "Hamilton"
                    fullName = "Margaret Hamilton"
                    effect = resources.getString(R.string.HERO_EFFECT_DECREASEREMOVAL)
                    vitae = resources.getString(R.string.hamilton)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.hamilton)
                }
                Type.DECREASE_ATT_SPEED ->
                {
                    name = "Vaughan"
                    fullName = "Dorothy Vaughan"
                    effect = resources.getString(R.string.HERO_EFFECT_ATTSPEED)
                    vitae = resources.getString(R.string.vaughan)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.vaughan)
                }
                Type.DECREASE_ATT_STRENGTH ->
                {
                    name = "Schneier"
                    fullName = "Bruce Schneier"
                    effect = resources.getString(R.string.HERO_EFFECT_ATTSTRENGTH)
                    vitae = resources.getString(R.string.schneier)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.schneier)
                }
                Type.INCREASE_REFUND ->
                {
                    name = "Tramiel"
                    fullName = "Jack Tramiel"
                    effect = resources.getString(R.string.HERO_EFFECT_REFUNDPRICE)
                    vitae = resources.getString(R.string.tramiel)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.tramiel)
                }
                Type.INCREASE_CHIP_SUB_RANGE ->
                {
                    name = "Wiener"
                    fullName = "Norbert Wiener"
                    effect = resources.getString(R.string.HERO_EFFECT_RANGE).format("SUB")
                    vitae = resources.getString(R.string.wiener)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.wiener)
                }
                Type.INCREASE_CHIP_SHR_RANGE ->
                {
                    name = "Pascal"
                    fullName = "Blaise Pascal"
                    effect = resources.getString(R.string.HERO_EFFECT_RANGE).format("SHR")
                    vitae = resources.getString(R.string.pascal)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.pascal)
                }
                Type.INCREASE_CHIP_MEM_RANGE ->
                {
                    name = "Hopper"
                    fullName = "Grace Hopper"
                    effect = resources.getString(R.string.HERO_EFFECT_RANGE).format("MEM")
                    vitae = resources.getString(R.string.hopper)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.hopper)
                }
                Type.INCREASE_MAX_HERO_LEVEL ->
                {
                    name = "Meier"
                    fullName = "Sid Meier"
                    effect = resources.getString(R.string.HERO_EFFECT_MAXHEROUPGRADE)
                    vitae = resources.getString(R.string.meier)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.meier)
                }
                Type.INCREASE_CHIP_RES_STRENGTH ->
                {
                    name = "Ohm"
                    fullName = "Georg Ohm"
                    effect = resources.getString(R.string.HERO_EFFECT_RES_STRENGTH)
                    vitae = resources.getString(R.string.ohm)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.ohm)
                }
                Type.INCREASE_CHIP_RES_DURATION ->
                {
                    name = "Volta"
                    fullName = "Alessandro Volta"
                    effect = resources.getString(R.string.HERO_EFFECT_RES_DURATION)
                    vitae = resources.getString(R.string.volta)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.volta)
                }
                Type.CONVERT_HEAT -> {
                    name = "Shannon"
                    fullName = "Claude Shannon"
                    effect = resources.getString(R.string.HERO_EFFECT_CONVERT_HEAT)
                    vitae = resources.getString(R.string.shannon)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.shannon)
                }
                Type.DOUBLE_HIT_SUB -> {
                    name = "Boole"
                    fullName = "George Boole"
                    effect = resources.getString(R.string.HERO_EFFECT_CHANCE_DOUBLE).format("SUB")
                    vitae = resources.getString(R.string.boole)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.boole)
                }
                Type.DOUBLE_HIT_SHR -> {
                    name = "Conway"
                    fullName = "John Horton Conway"
                    effect = resources.getString(R.string.HERO_EFFECT_CHANCE_DOUBLE).format("SHR")
                    vitae = resources.getString(R.string.conway)
                    picture = BitmapFactory.decodeResource(resources, R.drawable.conway)
                }
            }

            // determine the wikipedia link
            try {
                val resourceId =
                    resources.getIdentifier("url_" + name.toLowerCase(), "string", gameActivity.packageName)
                url = resources.getString(resourceId)
            }
            catch (_: Exception) {
                url = resources.getString(R.string.url_wikipedia_fallback)  // resource doesn't exist
            }
        }
    }

    inner class Biography(private var screenArea: Rect)
    /** The curriculum vitae of the hero, including graphical representation on the screen,
     * @param screenArea The rectangle on the screen provided for the biography. */
    {
        var area = Rect(screenArea)
        var bitmap: Bitmap = createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
        var viewOffset: Float = 0f
        /** the amount by which the biography should be scrolled at the most */
        private var maxViewOffset = 0f
        private var canvas = Canvas(bitmap)
        private var paintBiography = TextPaint()
        var wikiButton = Button(gameActivity.gameView, resources.getString(R.string.button_wiki),
                                        textSize = GameView.purchaseButtonTextSize * gameActivity.gameView.textScaleFactor,
                                        style = Button.Style.FRAME, preferredWidth = area.width()-4)
        var wikiButtonVisible = false
        /** whether clicking on the button triggers an action */
        var wikiButtonActive = false
        /** distance to lower edge of area where the wikipedia button begins to fade */
        private var margin = 10 * gameActivity.gameView.scaleFactor

        fun createBiography(selected: Hero?)
        {
            val text: String
            if (data.level>0)
            {
                text = vitae + "\n"
                paintBiography.color = selected?.card?.activeColor ?: Color.WHITE
                wikiButton.color = paintBiography.color
                if (gameActivity.gameMechanics.currentStageIdent.series > GameMechanics.SERIES_NORMAL)
                    wikiButtonVisible = true
            }
            else
            {
                text = "%s\n\n%s".format(person.fullName, effect)
                paintBiography.color = selected?.card?.inactiveColor ?: Color.WHITE
                wikiButtonVisible = false
            }
            canvas.drawColor(Color.BLACK)
            paintBiography.textSize = GameView.biographyTextSize*gameActivity.gameView.textScaleFactor
            paintBiography.alpha = 255
            val textLayout = StaticLayout(
                    text, paintBiography, screenArea.width(),
                    Layout.Alignment.ALIGN_NORMAL,1.0f,
                    0.0f,
                    false
            )
            area.bottom = screenArea.top + textLayout.height  // may be bigger or smaller than myArea
            this.bitmap = createBitmap(area.width(), area.height(), Bitmap.Config.ARGB_8888)
            canvas = Canvas(bitmap)
            textLayout.draw(canvas)
            maxViewOffset = (area.height()+2*wikiButton.area.height()-screenArea.height()).toFloat()
            maxViewOffset = if (maxViewOffset<0f) 0f else maxViewOffset
            wikiButtonActive = false
            wikiButton.alpha = 0
            placeButton()
        }

        fun display(canvas: Canvas)
        {
            val sourceRect = Rect(0, -viewOffset.toInt(), bitmap.width, screenArea.height()-viewOffset.toInt())
            canvas.drawBitmap(bitmap, sourceRect, screenArea, paintBiography)
            if (wikiButtonVisible) wikiButton.display(canvas)
        }

        fun placeButton()
        {
            if (!wikiButtonVisible)
                return
            wikiButton.area.setTopLeft(area.left, (area.bottom+viewOffset).toInt())
            val buttonDisappearsBelowThisLine = screenArea.bottom-margin
            if (!wikiButtonActive && wikiButton.area.bottom < buttonDisappearsBelowThisLine)
            {
                wikiButtonActive = true
                Fader(gameActivity.gameView, wikiButton, Fader.Type.APPEAR, Fader.Speed.FAST)
            }
            else if (wikiButtonActive && wikiButton.area.bottom > buttonDisappearsBelowThisLine)
            {
                wikiButtonActive = false
                Fader(gameActivity.gameView, wikiButton, Fader.Type.DISAPPEAR, Fader.Speed.IMMEDIATE)
            }
        }

        fun scroll(displacement: Float)
        {
            val scrollFactor = 1.0f  // higher values make scrolling faster
            viewOffset -= displacement * scrollFactor
            if (viewOffset > 0f) viewOffset = 0f // avoid scrolling when already at end of area
            if (viewOffset < -maxViewOffset) viewOffset = -maxViewOffset
            placeButton()
        }
    }

    /** Class that represents one single holiday for a certain hero */
    data class Holiday (
        val hero: Type,
        val from: Int,
        val to: Int,
    )

    fun isOnLeave(level: Stage.Identifier, leaveStartsOnLevel: Boolean = false): Boolean
    /**
     * @param leaveStartsOnLevel if true, consider only heroes that are actually leaving on this level.
     * Otherwise, also include those that are _still_ on leave.
     * @return whether the hero is on leave for the given stage. */
    {
        if (level.series != GameMechanics.SERIES_ENDLESS)
            return false
        if (leaveStartsOnLevel)
        {
            return gameActivity.gameMechanics.holidays[level.number]?.hero == this.data.type
        }
        else
            gameActivity.gameMechanics.holidays.values.forEach()
            {
                if (it.hero == this.data.type && it.from <= level.number && it.to >= level.number)
                    return true
            }
        return false
    }

    fun addLeave(level: Stage.Identifier, duration: Int)
    /** make this hero (the containing object) go on holiday.
     * @param level The stage where the leave starts
     * @param duration how many stages the leave will last (including start and end) */
    {
        val levelTo = Stage.Identifier(level.series, level.number+duration-1)
        gameActivity.gameMechanics.holidays[level.number]=Holiday(data.type, level.number, levelTo.number)
        Persistency(gameActivity).saveHolidays(gameActivity.gameMechanics)
    }

}

