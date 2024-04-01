package com.example.cpudefense

import android.graphics.*
import androidx.core.graphics.createBitmap
import com.example.cpudefense.gameElements.*
import com.example.cpudefense.networkmap.Link
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Track
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.blur
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class Stage(var theGame: Game) {

    class Identifier(var series: Int =1, var number: Int =0)
    /** A stage is identified by the combination of series (1 to 3) and the level number. */
    {
        fun next(): Identifier
        /** returns an identifier of the next level */
        { return Identifier(series, number+1)}

        fun previous(): Identifier
        /** returns an identifier of the previous level */
        { return Identifier(series, if (number<=1) 1 else number-1)}

        fun isGreaterThan(compare: Identifier): Boolean {
            return when {
                compare.series > this.series -> false
                compare.series < this.series -> true
                else -> compare.number < this.number
            }
        }

        fun mode(): Game.LevelMode
        {
            if (series == Game.SERIES_ENDLESS)
                return Game.LevelMode.ENDLESS
            else
                return Game.LevelMode.BASIC
        }
    }

    lateinit var network: Network
    var sizeX = 0
    var sizeY = 0

    var chips = hashMapOf<Int, Chip>()
    var tracks = hashMapOf<Int, Track>()
    var waves = CopyOnWriteArrayList<Wave>()

    private var ticksUntilFirstAttacker: Long = 0

    enum class Type { REGULAR, FINAL }


    data class Data (
        var ident: Identifier = Identifier(series=1, number=0),
        var type: Type = Type.REGULAR,
        var gridSizeX: Int = 1,
        var gridSizeY: Int = 1,
        var maxWaves: Int = 0,
        var wavesCount: Int = 0,
        var chips: HashMap<Int, Chip.Data> = hashMapOf(),
        var links: HashMap<Int, Link.Data> = hashMapOf(),
        var tracks: HashMap<Int, Track.Data> = hashMapOf(),
        var waves: CopyOnWriteArrayList<Wave.Data> = CopyOnWriteArrayList<Wave.Data>(),
        var attackers: CopyOnWriteArrayList<Attacker.Data> = CopyOnWriteArrayList<Attacker.Data>(),
        var chipsAllowed: Set<Chip.ChipUpgrades> = setOf(),
        var obstaclesRemovedCount: Int = 0,
        var difficulty: Double = 999.0,
        )
    var data = Data()

    data class Summary(
        var coinsAvailable: Int = 0,
        var coinsGot: Int = 0,
        var coinsMaxAvailable: Int = 0,
        var won: Boolean = false,
    )
    lateinit var summary: Summary

    var rewardCoins = 0  // number of coins that can be obtained by completing the level

    fun getLevel(): Int {return data.ident.number}

    fun getSeries(): Int {return data.ident.series}

    fun isInitialized(): Boolean
    {
        return this::network.isInitialized
    }

    fun calculateRewardCoins(previousSummary: Summary?): Int
     /** calculate the coins available for completing this level,
             * taking into account the coins already got in previous games.
             * @param previousSummary Saved data set for this level, contains number of coins got earlier
             * @return number of coins for the current game
              */
    {
        summary = previousSummary ?: Summary()
        summary.coinsAvailable = rewardCoins - summary.coinsGot
        summary.coinsMaxAvailable = rewardCoins
        return summary.coinsAvailable
    }

    fun provideStructureData(): Data
            /** serialize all objects that belong to this stage
             * and return the data object
             * for saving and restoring the game.
             */
    {
        data.gridSizeX = network.data.gridSizeX
        data.gridSizeY = network.data.gridSizeY
        data.chips.clear()
        chips.forEach()
        { (key, value) -> data.chips[key] = value.chipData }
        data.links.clear()
        network.links.forEach()
        { (key, value) -> data.links[key] = value.data }
        data.tracks.clear()
        tracks.forEach()
        { (key, track) -> data.tracks[key] = track.data }
        data.waves.clear()
        waves.forEach()
        { data.waves.add(it.data) }
        return data
    }
    fun provideData(): Data
            /** serialize all objects that belong to this stage
             * and return the data object
             * for saving and restoring the game.
             */
    {
        provideStructureData()
        data.attackers.clear()
        network.vehicles.forEach()
        { data.attackers.add((it as Attacker).provideData())}
        return data
    }

    companion object {
        fun fillEmptyStageWithData(stage: Stage, stageData: Data)
        /** restores the fixed part of a stage: Nodes, Links, Tracks.
         * @param stage The empty stage that must be filled
         * @param stageData the data structure used to create the stage */
        {
            if (stageData.gridSizeX <= 1 || stageData.gridSizeY <= 1)
                return
            stage.data = stageData
            stage.sizeX = stage.data.gridSizeX
            stage.sizeY = stage.data.gridSizeY
            stage.network = Network(stage.theGame, stage.sizeX, stage.sizeY)
            for ((id, chipData) in stage.data.chips)
            {
                val chip = Chip.createFromData(stage.network, chipData)
                stage.network.addNode(chip, id)
                stage.chips[id] = chip
            }
            for ((id, linkData) in stage.data.links)
            {
                val link = Link.createFromData(stage.network, linkData)
                stage.network.addLink(link, id)
            }
            for ((id, trackData) in stage.data.tracks)
            {
                val track = Track.createFromData(stage, trackData)
                stage.tracks[id] = track
            }
            // set summary and available coins
            stage.theGame.getSummaryOfStage(stage.data.ident)?.let {
                stage.summary = it
                stage.rewardCoins = it.coinsMaxAvailable
                // stage.theGame.state.coinsInLevel = it.coinsAvailable ?: 0
            }
        }
        fun createStageFromData(game: Game, stageData: Data): Stage
        {
            val stage = Stage(game)
            fillEmptyStageWithData(stage, stageData)
            for (waveData in stage.data.waves)
            {
                val wave = Wave.createFromData(game, waveData)
                stage.waves.add(wave)
            }
            for (attackerData in stage.data.attackers)
            {
                val attacker = Attacker.createFromData(stage, attackerData)
                stage.network.addVehicle(attacker)
            }
            return stage
        }
    }

    fun createNewAttacker(maxNumber: Int, speed: Float, isCoin: Boolean = false,
                          representation: Attacker.Representation = Attacker.Representation.BINARY)
    {
        val actualSpeed = speed * theGame.heroModifier(Hero.Type.DECREASE_ATT_SPEED)
        val attacker = if (isCoin)
            Cryptocoin(network, (maxNumber*1.5).toULong(), actualSpeed )
        else {
            val strength = Random.nextFloat()*(maxNumber+1) * theGame.heroModifier(Hero.Type.DECREASE_ATT_STRENGTH)
            Attacker(network, representation, strength.toULong(), actualSpeed)
        }
        network.addVehicle(attacker)

        if (tracks.size > 0)
            attacker.setOntoTrack(tracks[Random.nextInt(tracks.size)])
    }

    fun chipCount(type: Chip.ChipType): Int
            /**
             * @param type the type of chips to be counted
             *  @return the number of chips of this type in the network
             *  */
    {
        return chips.values.filter { it.chipData.type == type }.size
    }

    fun attackerCount(): Int
    {
        return network.vehicles.size
    }

    fun nextWave(): Wave?
    {
        when (theGame.state.phase)
        {
            Game.GamePhase.START -> return null
            Game.GamePhase.INTERMEZZO -> return null
            Game.GamePhase.MARKETPLACE -> return null
            else -> {
                if (waves.size == 0)
                {
                    theGame.onEndOfStage()
                    return null
                }
                else {
                    data.wavesCount++
                    return waves.removeFirst()
                }
            }
        }
    }

    /* methods for creating and setting up the stage */

    fun initializeNetwork(dimX: Int, dimY: Int)
    /** creates an empty network with the given grid dimensions */
    {
        sizeX = dimX
        sizeY = dimY
        network = Network(theGame, sizeX, sizeY)
        theGame.viewport.setGridSize(sizeX, sizeY)
    }

    fun createChip(gridX: Int, gridY: Int, ident: Int = -1, type: Chip.ChipType = Chip.ChipType.EMPTY): Chip
            /**
             * creates a chip at the given position. If an ident is given, it is used,
             * otherwise a new ident is created.
             * @param gridX Position in grid coordinates
             * @param gridY Position in grid coordinates
             * @param ident Node ident, or -1 to choose a new one
             */
    {
        var id = ident
        lateinit var chip: Chip
        when (type)
        {
            Chip.ChipType.ENTRY -> {
                chip = EntryPoint(network, gridX, gridY)
                if (id == -1)
                    id = 0  // default value, may be overridden
            }
            Chip.ChipType.CPU -> {
                chip = Cpu(network, gridX, gridY)
                if (id == -1)
                    id = 999  // default value
            }
            else -> { chip = Chip(network, gridX, gridY) }
        }
        id = network.addNode(chip, id)
        chips[id] = chip
        chip.setIdent(id)
        return chip
    }

    fun createLink(from: Int, to: Int, ident: Int, mask: Int = 0xF, variant: Link.Variant = Link.Variant.CONVEX): Link?
    /** adds a link between two existing nodes, referenced by ID
     * @param from Ident of first node
     * @param to Ident of 2nd node
     * @param ident Ident of the link, or -1 to choose a new one
     * @return the new link
     * */
    {
        val node1 = network.nodes[from] ?: return null
        val node2 = network.nodes[to] ?: return null
        if (network.links.containsKey(ident))
        {  // link already present, do not create a new one
            return network.links[ident]
        }
        else {
            val link = Link(network, node1, node2, ident, mask, variant)
            network.addLink(link, ident)
            node1.connectedLinks.add(link)
            node2.connectedLinks.add(link)
            return link
        }
    }

    fun createTrack(linkIdents: List<Int>, ident: Int)
            /** adds a track of connected links
             * @param linkIdents List of the link idents in the track
             * */
    {
        val track = network.createTrack(linkIdents, false)
        tracks[ident] = track
    }
    
    fun createWave(attackerCount: Int, attackerStrength: Int, attackerFrequency: Float, attackerSpeed: Float,
                           coins: Int = 0, representation: Attacker.Representation = Attacker.Representation.UNDEFINED)
    {
        val series = getSeries()
        var count = attackerCount
        var strength = attackerStrength
        var frequency = attackerFrequency
        var speed = attackerSpeed
        if (series==2)  // modifications in strength for turbo mode
        {
            count = (attackerCount * 1.5f).toInt()
            strength = (attackerStrength * ( 1 + waves.size*waves.size*0.2f + waves.size ) + 4).toInt()
            frequency = attackerFrequency * 1.6f
            speed = attackerSpeed * 1.2f
        }
        val waveData = Wave.Data(count, strength, frequency, speed,
            coins, currentCount = count, representation = representation, ticksUntilNextAttacker = ticksUntilFirstAttacker.toDouble())
        ticksUntilFirstAttacker = (4 * 20).toLong()  // after first wave, set a delay here. 20 ticks equal one second
        waves.add(Wave(theGame, waveData))
    }

    fun createWaveHex(attackerCount: Int, attackerStrength: Int, attackerFrequency: Float, attackerSpeed: Float, coins: Int = 0)
    {
        createWave(attackerCount, attackerStrength, attackerFrequency, attackerSpeed, coins, Attacker.Representation.HEX)
    }

    private fun createAttacker(data: Attacker.Data)
    {
        val attacker = Attacker.createFromData(this, data)
        network.addVehicle(attacker)
    }

    fun difficultyOfObstacles(): Double
    {
        var sumOfObstacles = 0.0
        chips.values.forEach()
        { sumOfObstacles += it.obstacleDifficulty() }
        return sumOfObstacles
    }
    fun calculateDifficulty()
    {
        var minLength = 999
        var sumLength = 0
        for (track in tracks.values)
        {
            sumLength += track.links.size
            if (minLength>track.links.size)
                minLength = track.links.size
        }
        if (minLength<4 || tracks.size == 0) {
            data.difficulty = 999.0  // too difficult
            return
        }
        /* calculate weighted difficulty */
        var difficulty = 16.0 // base value
        difficulty -= sumLength.toDouble()/tracks.size * 0.4  // mean length of tracks
        difficulty -= minLength * 0.7 // shortest track
        difficulty +=  difficultyOfObstacles()
        data.difficulty = difficulty
    }
    fun takeSnapshot(size: Int): Bitmap?
            /** gets a miniature picture of the current level
             * @param size snapshot size in pixels (square)
             * @return the bitmap that holds the snapshot
             */
    {
        val p: Viewport = theGame.viewport
        if (p.viewportWidth > 0 && p.viewportHeight > 0)
        {
            var bigSnapshot = createBitmap(p.viewportWidth, p.viewportHeight)
            network.makeSnapshot(Canvas(bigSnapshot), p)
            /* blur the image */
            bigSnapshot = bigSnapshot.blur(theGame.gameActivity, 3f) ?: bigSnapshot
            return Bitmap.createScaledBitmap(bigSnapshot, size, size, true)
        }
        else
            return null
    }
}