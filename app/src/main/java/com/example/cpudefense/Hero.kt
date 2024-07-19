@file:Suppress("DEPRECATION")

package com.example.cpudefense

import android.graphics.*
import android.graphics.Bitmap.createBitmap
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.cpudefense.effects.Fadable
import com.example.cpudefense.gameElements.HeroCard
import kotlin.math.exp
import kotlin.math.truncate

class Hero(var game: Game, type: Type)
{
    /*
    potential Heroes for versions to come:
    - John von Neuman?
    - Babbage?
    - Berners-Lee?
    - Torvalds
    - Baudot
    - John Conway
     */

    enum class Type { INCREASE_CHIP_SUB_SPEED, INCREASE_CHIP_SUB_RANGE,
        INCREASE_CHIP_SHR_SPEED,  INCREASE_CHIP_SHR_RANGE,
        INCREASE_CHIP_MEM_SPEED,  INCREASE_CHIP_MEM_RANGE, ENABLE_MEM_UPGRADE,
        INCREASE_CHIP_RES_STRENGTH, INCREASE_CHIP_RES_DURATION,
        REDUCE_HEAT,
        DECREASE_ATT_FREQ, DECREASE_ATT_SPEED, DECREASE_ATT_STRENGTH,
        ADDITIONAL_LIVES, INCREASE_MAX_HERO_LEVEL, LIMIT_UNWANTED_CHIPS,
        INCREASE_STARTING_CASH, GAIN_CASH, DECREASE_REMOVAL_COST,
        DECREASE_UPGRADE_COST, INCREASE_REFUND, GAIN_CASH_ON_KILL}

    data class Data (
        val type: Type,
        var level: Int = 0,
        var coinsSpent: Int = 0
    )
    var data = Data(type = type)

    var shortDesc: String = "effect description"
    var strengthDesc: String = "format string"
    var upgradeDesc: String = " → next level"
    var costDesc: String = "[cost: ]"
    var person = Person(type)
    var card = HeroCard(game, this)

    var biography: Biography? = null
    var effect: String = ""
    var vitae: String = ""
    /** cannot upgrade beyond this level. This value can be modified for certain heroes,
     * or by the effect of Sid Meier
      */
    private var maxLevel = 7

    fun createBiography(area: Rect)
    {
        if (biography == null)
            biography = Biography(Rect(0,0,area.width(), area.height()))
        biography?.createBiography(this)

    }

    fun setDesc()
    {
        val strength = getStrength(data.level)
        val next = getStrength(data.level+1)
        when (data.type)
        {
            Type.INCREASE_CHIP_SUB_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_SUB)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → x %.2f".format(next)
            }
            Type.INCREASE_STARTING_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_startinfo)
                strengthDesc = "%d bits".format(strength.toInt())
                upgradeDesc = " → %d bits".format(next.toInt())
            }
            Type.INCREASE_CHIP_SHR_SPEED ->             {
                shortDesc = game.resources.getString(R.string.shortdesc_SHR)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_MEM)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_RES_STRENGTH -> {
                shortDesc = game.resources.getString(R.string.shortdesc_RES)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_RES_DURATION -> {
                shortDesc = game.resources.getString(R.string.shortdesc_duration)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.REDUCE_HEAT -> {
                shortDesc = game.resources.getString(R.string.shortdesc_heat)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.DECREASE_UPGRADE_COST -> {
                shortDesc = game.resources.getString(R.string.shortdesc_upgrade)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
            Type.ADDITIONAL_LIVES -> {
                shortDesc = game.resources.getString(R.string.shortdesc_lives)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " → %d".format(next.toInt())
                maxLevel = 3
            }
            Type.DECREASE_ATT_FREQ -> {
                shortDesc = game.resources.getString(R.string.shortdesc_frequency)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_SPEED -> {
                shortDesc = game.resources.getString(R.string.shortdesc_att_speed)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_MAX_HERO_LEVEL ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_max_hero_upgrade)
                strengthDesc = "+%d".format(strength.toInt())
                upgradeDesc = " → +%d".format(next.toInt())
                maxLevel = 3
            }
            Type.LIMIT_UNWANTED_CHIPS ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_limit_unwanted)
                strengthDesc = "-%d".format(strength.toInt())
                upgradeDesc = " → -%d".format(next.toInt())
            }
            Type.ENABLE_MEM_UPGRADE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_enable_mem_upgrade)
                strengthDesc = "%d".format(strength.toInt())
                upgradeDesc = " → %d".format(next.toInt())
                maxLevel = Game.maxInternalChipStorage - 1
            }
            Type.GAIN_CASH ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_info_gain)
                strengthDesc = "1 bit/%d ticks".format(strength.toInt())
                upgradeDesc = " → 1/%d ticks".format(next.toInt())
            }
            Type.GAIN_CASH_ON_KILL ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_info_on_kill)
                strengthDesc = "%d bit/kill".format(strength.toInt())
                upgradeDesc = " → %d bit/kill".format(next.toInt())
            }
            Type.INCREASE_REFUND ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_refund)
                strengthDesc = "%d%%".format(strength.toInt())
                upgradeDesc = " → %d%%".format(next.toInt())
                maxLevel = 5  // even at level 6, refund is more than 100%
            }
            Type.INCREASE_CHIP_SUB_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range).format("SUB")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }  
            Type.INCREASE_CHIP_SHR_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range).format("SHR")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.INCREASE_CHIP_MEM_RANGE ->
            {
                shortDesc = game.resources.getString(R.string.shortdesc_range).format("MEM")
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_ATT_STRENGTH -> {
                shortDesc = game.resources.getString(R.string.shortdesc_att_strength)
                strengthDesc = "x %.2f".format(strength)
                upgradeDesc = " → %.2f".format(next)
            }
            Type.DECREASE_REMOVAL_COST -> {
                shortDesc = game.resources.getString(R.string.shortdesc_reduce_removal)
                strengthDesc = "-%d%%".format(strength.toInt())
                upgradeDesc = " → -%d%%".format(next.toInt())
            }
        }
        val cost = getPrice(data.level)
        costDesc = game.resources.getString(R.string.cost_desc).format(cost)
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
        val additionalUpgradePossibility = game.heroModifier(Type.INCREASE_MAX_HERO_LEVEL).toInt()
        if (data.type in
            listOf( Type.ADDITIONAL_LIVES, Type.INCREASE_MAX_HERO_LEVEL, Type.GAIN_CASH, Type.INCREASE_REFUND, Type.ENABLE_MEM_UPGRADE))
            return maxLevel
        else
            return maxLevel + (additionalUpgradePossibility ?: 0)
    }

    private fun upgradeLevel(type: Type): Int
    {
        val level = game.currentHeroes()[type]?.data?.level
        return level ?: 0
    }

    fun isAvailable(stageIdentifier: Stage.Identifier): Boolean
            /** function that evaluates certain restrictions on upgrades.
             * Some upgrades require others to reach a certain level, etc.
             * @param stageIdentifier cards may depend on the stage and/or series
             */
    {
        if (stageIdentifier.series > 1)  // restrictions only apply for series 1
            return true
        return when (data.type) {
            Type.LIMIT_UNWANTED_CHIPS ->    upgradeLevel(Type.INCREASE_MAX_HERO_LEVEL) >= 3
            Type.INCREASE_MAX_HERO_LEVEL -> upgradeLevel(Type.ADDITIONAL_LIVES) >= 3
            Type.DECREASE_ATT_STRENGTH ->   upgradeLevel(Type.DECREASE_ATT_SPEED) >= 3
            Type.DECREASE_ATT_SPEED ->      upgradeLevel(Type.DECREASE_ATT_FREQ) >= 3
            Type.ADDITIONAL_LIVES ->        upgradeLevel(Type.DECREASE_ATT_SPEED) >= 5
            Type.DECREASE_ATT_FREQ ->       upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 3
            Type.GAIN_CASH_ON_KILL ->       upgradeLevel(Type.INCREASE_REFUND) >= 3
            Type.INCREASE_REFUND ->         upgradeLevel(Type.DECREASE_UPGRADE_COST) >= 3
            Type.DECREASE_UPGRADE_COST ->   upgradeLevel(Type.GAIN_CASH) >= 3
            Type.GAIN_CASH ->               upgradeLevel(Type.INCREASE_STARTING_CASH) >= 3
            Type.DECREASE_REMOVAL_COST ->   upgradeLevel(Type.GAIN_CASH) >= 3
            Type.REDUCE_HEAT ->             upgradeLevel(Type.INCREASE_CHIP_MEM_SPEED) >= 3
            Type.INCREASE_CHIP_MEM_SPEED -> stageIdentifier.number >= 14
            Type.INCREASE_CHIP_SUB_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SUB_SPEED) >= 5
            Type.INCREASE_CHIP_SHR_RANGE -> upgradeLevel(Type.INCREASE_CHIP_SHR_SPEED) >= 5
            Type.INCREASE_CHIP_MEM_RANGE -> upgradeLevel(Type.INCREASE_CHIP_MEM_SPEED) >= 5
            Type.ENABLE_MEM_UPGRADE ->      upgradeLevel(Type.INCREASE_CHIP_MEM_RANGE) >= 3
            Type.INCREASE_CHIP_RES_STRENGTH -> stageIdentifier.number >= 32
            Type.INCREASE_CHIP_RES_DURATION -> upgradeLevel(Type.INCREASE_CHIP_RES_STRENGTH) >= 3
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

    fun upgradeInfo(): String
    /** displays a text with info on the next available upgrade */
    {
        return "%s %s\n%s %s".format(shortDesc, strengthDesc, upgradeDesc, costDesc)
    }

    fun doUpgrade()
    {
        if (data.level >= getMaxUpgradeLevel())
            return
        data.level += 1
        setDesc()
        card.upgradeAnimation()
    }

    fun doDowngrade()
    {
        if (data.level <= 0)
            return
        data.level -= 1
        Persistency(game.gameActivity).saveHeroes(game)
        card.downgradeAnimation()
    }
    fun resetUpgrade()
    {
        data.level = 0
        data.coinsSpent = 0
        card.heroOpacity = 0f
        setDesc()
    }

    companion object {
        fun createFromData(game: Game, data: Data): com.example.cpudefense.Hero
                /** reconstruct a Hero object based on the saved data
                 * and set all inner proprieties
                 */
        {
            val newInstance = Hero(game, data.type)
            newInstance.data.level = data.level
            newInstance.data.coinsSpent = data.coinsSpent
            newInstance.person.setType()
            newInstance.card.heroOpacity = when (data.level) { 0 -> 0f else -> 1f}
            newInstance.setDesc()
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
                Type.INCREASE_STARTING_CASH -> return Game.minimalAmountOfCash.toFloat() + level * level
                Type.REDUCE_HEAT -> return level * 10f
                Type.DECREASE_UPGRADE_COST -> return level * 6f
                Type.DECREASE_REMOVAL_COST -> return level * 8f
                Type.ADDITIONAL_LIVES -> return level.toFloat()
                Type.DECREASE_ATT_FREQ -> return 1.0f - level * 0.05f
                Type.DECREASE_ATT_SPEED -> return 1.0f - level * 0.04f
                Type.DECREASE_ATT_STRENGTH -> return exp(- level / 3.0).toFloat()
                Type.INCREASE_MAX_HERO_LEVEL -> return level.toFloat()
                Type.LIMIT_UNWANTED_CHIPS -> return level.toFloat()
                Type.ENABLE_MEM_UPGRADE -> return (level+1).toFloat()
                Type.GAIN_CASH -> return if (level>0) (8f - level) * 9 else 0f
                Type.GAIN_CASH_ON_KILL -> return truncate((level+1) * 0.5f)
                Type.INCREASE_REFUND -> return (50f + level * 10)
                Type.INCREASE_CHIP_SUB_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_SHR_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_MEM_RANGE -> return 1.0f + level / 10f
                Type.INCREASE_CHIP_RES_STRENGTH -> return 1.0f + level * 0.2f
                Type.INCREASE_CHIP_RES_DURATION -> return 1.0f + level * 0.2f
                else -> return level.toFloat()
            }
        }

    }

    inner class Person(var type: Type)
    {
        var name = ""
        var fullName = ""
        var picture: Bitmap? = null

        fun setType()
        {
            when (type)
            {
                Type.INCREASE_CHIP_SUB_SPEED ->
                {
                    name = "Turing"
                    fullName = "Alan Turing"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SUB")
                    vitae = game.resources.getString(R.string.turing)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.turing)
                }
                Type.INCREASE_CHIP_SHR_SPEED ->
                {
                    name = "Lovelace"
                    fullName = "Ada Lovelace"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("SHR")
                    vitae = game.resources.getString(R.string.lovelace)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.lovelace)
                }
                Type.INCREASE_CHIP_MEM_SPEED ->
                {
                    name = "Knuth"
                    fullName = "Donald E. Knuth"
                    effect = game.resources.getString(R.string.HERO_EFFECT_CHIPSPEED).format("MEM")
                    vitae = game.resources.getString(R.string.knuth)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.knuth)
                }
                Type.REDUCE_HEAT ->
                {
                    name = "Chappe"
                    fullName = "Claude Chappe"
                    effect = game.resources.getString(R.string.HERO_EFFECT_HEAT)
                    vitae = game.resources.getString(R.string.chappe)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.chappe)
                }
                Type.INCREASE_STARTING_CASH ->
                {
                    name = "Hollerith"
                    fullName = "Herman Hollerith"
                    effect = game.resources.getString(R.string.HERO_EFFECT_STARTINFO)
                    vitae = game.resources.getString(R.string.hollerith)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hollerith)
                }
                Type.DECREASE_UPGRADE_COST ->
                {
                    name = "Osborne"
                    fullName = "Adam Osborne"
                    effect = game.resources.getString(R.string.HERO_EFFECT_UPGRADECOST)
                    vitae = game.resources.getString(R.string.osborne)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.osborne)
                }
                Type.ADDITIONAL_LIVES ->
                {
                    name = "Zuse"
                    fullName = "Konrad Zuse"
                    effect = game.resources.getString(R.string.HERO_EFFECT_LIVES)
                    vitae = game.resources.getString(R.string.zuse)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.zuse)
                }
                Type.LIMIT_UNWANTED_CHIPS ->
                {
                    name = "Kilby"
                    fullName = "Jack Kilby"
                    effect = game.resources.getString(R.string.HERO_EFFECT_LIMITUNWANTED)
                    vitae = game.resources.getString(R.string.kilby)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.kilby)
                }
                Type.ENABLE_MEM_UPGRADE ->
                {
                    name = "Leibniz"
                    fullName = "Gottfried Wilhelm Leibniz"
                    effect = game.resources.getString(R.string.HERO_EFFECT_ENABLEMEM)
                    vitae = game.resources.getString(R.string.leibniz)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.leibniz)
                }
                Type.DECREASE_ATT_FREQ ->
                {
                    name = "LHC"
                    fullName = "Les Horribles Cernettes"
                    effect = game.resources.getString(R.string.HERO_EFFECT_FREQUENCY)
                    vitae = game.resources.getString(R.string.cernettes)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.cernettes)
                }
                Type.GAIN_CASH ->
                {
                    name = "Franke"
                    fullName = "Herbert W. Franke"
                    effect = game.resources.getString(R.string.HERO_EFFECT_INFOOVERTIME)
                    vitae = game.resources.getString(R.string.franke)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.franke)
                }
                Type.GAIN_CASH_ON_KILL ->
                {
                    name = "Mandelbrot"
                    fullName = "Benoît B. Mandelbrot"
                    effect = game.resources.getString(R.string.HERO_EFFECT_GAININFO)
                    vitae = game.resources.getString(R.string.mandelbrot)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.mandelbrot)
                }
                Type.DECREASE_REMOVAL_COST ->
                {
                    name = "Hamilton"
                    fullName = "Margaret Hamilton"
                    effect = game.resources.getString(R.string.HERO_EFFECT_DECREASEREMOVAL)
                    vitae = game.resources.getString(R.string.hamilton)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hamilton)
                }
                Type.DECREASE_ATT_SPEED ->
                {
                    name = "Vaughan"
                    fullName = "Dorothy Vaughan"
                    effect = game.resources.getString(R.string.HERO_EFFECT_ATTSPEED)
                    vitae = game.resources.getString(R.string.vaughan)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.vaughan)
                }
                Type.DECREASE_ATT_STRENGTH ->
                {
                    name = "Schneier"
                    fullName = "Bruce Schneier"
                    effect = game.resources.getString(R.string.HERO_EFFECT_ATTSTRENGTH)
                    vitae = game.resources.getString(R.string.schneier)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.schneier)
                }
                Type.INCREASE_REFUND ->
                {
                    name = "Tramiel"
                    fullName = "Jack Tramiel"
                    effect = game.resources.getString(R.string.HERO_EFFECT_REFUNDPRICE)
                    vitae = game.resources.getString(R.string.tramiel)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.tramiel)
                }
                Type.INCREASE_CHIP_SUB_RANGE ->
                {
                    name = "Wiener"
                    fullName = "Norbert Wiener"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("SUB")
                    vitae = game.resources.getString(R.string.wiener)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.wiener)
                }
                Type.INCREASE_CHIP_SHR_RANGE ->
                {
                    name = "Pascal"
                    fullName = "Blaise Pascal"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("SHR")
                    vitae = game.resources.getString(R.string.pascal)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.pascal)
                }
                Type.INCREASE_CHIP_MEM_RANGE ->
                {
                    name = "Hopper"
                    fullName = "Grace Hopper"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RANGE).format("MEM")
                    vitae = game.resources.getString(R.string.hopper)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.hopper)
                }
                Type.INCREASE_MAX_HERO_LEVEL ->
                {
                    name = "Meier"
                    fullName = "Sid Meier"
                    effect = game.resources.getString(R.string.HERO_EFFECT_MAXHEROUPGRADE)
                    vitae = game.resources.getString(R.string.meier)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.meier)
                }
                Type.INCREASE_CHIP_RES_STRENGTH ->
                {
                    name = "Ohm"
                    fullName = "Georg Ohm"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RES_STRENGTH)
                    vitae = game.resources.getString(R.string.ohm)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.ohm)
                }
                Type.INCREASE_CHIP_RES_DURATION ->
                {
                    name = "Volta"
                    fullName = "Alessandro Volta"
                    effect = game.resources.getString(R.string.HERO_EFFECT_RES_DURATION)
                    vitae = game.resources.getString(R.string.volta)
                    picture = BitmapFactory.decodeResource(game.resources, R.drawable.volta)
                }
            }
        }

    }

    inner class Biography(var myArea: Rect)
    {
        var bitmap: Bitmap = createBitmap(myArea.width(), myArea.height(), Bitmap.Config.ARGB_8888)
        private var canvas = Canvas(bitmap)
        private var paintBiography = TextPaint()

        fun createBiography(selected: com.example.cpudefense.Hero?)
        {
            val text: String
            if (data.level>0)
            {
                text = vitae
                paintBiography.color = selected?.card?.activeColor ?: Color.WHITE
            }
            else
            {
                text = "%s\n\n%s".format(person.fullName, effect)
                paintBiography.color = selected?.card?.inactiveColor ?: Color.WHITE
            }
            canvas.drawColor(Color.BLACK)
            paintBiography.textSize = Game.biographyTextSize*game.resources.displayMetrics.scaledDensity
            paintBiography.alpha = 255
            val textLayout = StaticLayout(
                text, paintBiography, myArea.width(),
                Layout.Alignment.ALIGN_NORMAL,
                1.0f,
                0.0f,
                false
            )
            // if the text exceeds the area provided, enlarge the bitmap
            if (textLayout.height>this.bitmap.height)
            {
                this.bitmap = createBitmap(myArea.width(), textLayout.height, Bitmap.Config.ARGB_8888)
                canvas = Canvas(bitmap)
            }
            textLayout.draw(canvas)
        }
    }
}
