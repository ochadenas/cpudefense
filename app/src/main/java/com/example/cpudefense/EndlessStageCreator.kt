package com.example.cpudefense

import android.graphics.Rect
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.utils.setTopLeft
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class EndlessStageCreator(val stage: Stage)
/**
 * This class provides methods to generate an aritrary number of random stage objects.
 * Like StageCatalog, it is not meant to be instantiated.
 */
{
    val dimX = 50
    val dimY = 60
    private val sectorSizeX = dimX / 3
    private val sectorSizeY = dimY / 4
    private val numberOfSectorsX = dimX / sectorSizeX
    private val numberOfSectorsY = dimY / sectorSizeY
    private var sectors = mutableListOf<Sector>()
    private val paths = mutableListOf<Path>()

    private var chipIdent = 0
    /** global count for the chips */

    enum class Direction {UP, DOWN, LEFT, RIGHT, DOWNLEFT}
    enum class SectorType { ENTRY, EXIT, NORMAL }

    private fun nextIdent(): Int
    { chipIdent++; return chipIdent }

    fun createStage(level: Stage.Identifier)
        /**
         * Main method for the stage creation algorithm. It works as follows:
         * First, the whole stage is divided into a coarse grid of Sectors.
         * Through the Sectors, several Paths are laid that run from a start sector
         * to the exit sector.
         * Then the Sectors are populated with nodes, according to the incoming and
         * outgoing connections. Finally, Tracks are created from all nodes of the
         * Sectors on the Path.
         * @param level The identifier of the level to be created
         */
    {
        stage.data.ident = level
        stage.waves.clear()
        stage.data.type = Stage.Type.REGULAR
        stage.data.chipsAllowed =
            setOf(Chip.ChipUpgrades.ACC, Chip.ChipUpgrades.SUB, Chip.ChipUpgrades.SHR,
                Chip.ChipUpgrades.MEM, Chip.ChipUpgrades.CLK, Chip.ChipUpgrades.POWERUP,
                Chip.ChipUpgrades.REDUCE, Chip.ChipUpgrades.SELL )
        stage.initializeNetwork(dimX, dimY)

        // cut the stage area into sectors
        val areaOfOneSector = Rect(0, 0, sectorSizeX, sectorSizeY)  // area in grid coordinates
        for (x in 0 until numberOfSectorsX)
            for (y in 0 until numberOfSectorsY)
            {
                val sectorIdent = SectorCoord(x, y)
                val sector = Sector(sectorIdent, Rect(areaOfOneSector).setTopLeft(x*sectorSizeX, y*sectorSizeY))
                sectors.add(sector)
            }
        // determine entries and exits
        var entrySectors = mutableListOf<Sector?>()
        if (Random.nextFloat() < .7)
            entrySectors.add(getByCoordinate(SectorCoord(0,0)))
        if (Random.nextFloat() < .5)
            entrySectors.add(getByCoordinate(SectorCoord(2,0)))
        if (Random.nextFloat() < .2)
            entrySectors.add(getByCoordinate(SectorCoord(1,0)))
        if (Random.nextFloat() < .3)
            entrySectors.add(getByCoordinate(SectorCoord(0,1)))
        entrySectors.forEach {
            it?.type = SectorType.ENTRY
        }
        val exit = getByCoordinate(SectorCoord(numberOfSectorsX-1,numberOfSectorsY-1))
        exit?.type = SectorType.EXIT

        for (count in 1 .. 5)
            entrySectors.random()?.let { sector ->
                createPath(sector)?.let { path -> paths.add(path)}
            }

        for (sector in sectors)
            sector.createNodes()

        paths.forEachIndexed { ident, path ->
            path.makeListOfNodes()
            val track = createTrackFromPath(path)
            stage.createTrack(track, ident)
        }
        
        createWaves()
        stage.rewardCoins = 3
        return
    }

    fun createWaves()
    {
        val levelNumber = stage.data.ident.number
        for (waveNumber in 1 .. (levelNumber + 3)) {
            val attackerCount = 16
            val strength = waveNumber+levelNumber
            val attackerStrength = (Attacker.powerOfTwo[strength] ?: 1048576u).toInt()
            val attackerSpeed = (12 + strength) * 0.07f
            val attackerFrequency = (12 + strength) * 0.008f
            val coins = if (Random.nextFloat()>0.92) 1 else 0
            stage.createWave(attackerCount, attackerStrength, attackerFrequency, attackerSpeed, coins = coins)
        }
        stage.data.maxWaves = stage.waves.size
    }

    /*
    fun createTrack(startSector: Sector, ident: Int, entries: List<Chip>, exits: List<Chip>)
    /** creates a random Track object in the Stage with the given ident
     * starting on startSector
     * @param entries List of nodes that can be used as entry points
     * @param exits List of nodes that can be used as exit points
     */
    {
        var sectorPath = createPath(startSector)
        var path = mutableListOf<Chip>()
        sectorPath?.forEach() {
            path += it.makePath()
        }
        var track = makeTrackFromPath(stage, path, entries[0], exits[0])
        stage.createTrack(track, ident)
    }

     */
    
    fun createPath(firstSector: Sector): Path?
    /** tries to find a path to an exit sector.
     * @param firstSector Start of path
     * @return list of sectors, or null if no path is found */
    {
        for (count in 1 .. 10) // number of tries
            pathToExit(firstSector)?.let { return it}
        return null
    }

    fun pathToExit(firstSector: Sector): Path?
    /** create a random path of sectors, starting with firstSector.
     * @return a list of sectors ending with an "exit" sector, or null if
     * the path does not reach an exit sector */
    {
        var sectorPath = Path()
        sectorPath.sectors.add(firstSector)
        var sector = firstSector
        var nextSector: Sector?
        do {
            nextSector = getRandomNeighbour(sector, sectorPath.sectors)
            nextSector?.let {
                sectorPath.sectors.add(it)
                sector = it
                if (nextSector.type == SectorType.EXIT)
                    return sectorPath
            }
        } while (nextSector != null)
        return null
    }

    fun getMask(): Int
    /** returns a random value for the link mask */
    {
        return when (Random.nextInt(10))
        {
            in 0..1 -> 0x06
            in 2..3 -> 0x03
            in 4..5 -> 0x07
            6 -> 0x0f
            in 7 .. 8 -> 0x04
            else -> 0x02
        }
    }

    fun getByCoordinate(coord: SectorCoord): Sector?
    {
        return sectors.find { sec -> sec.ident.isEqual(coord) }
    }

    fun getNeighbour(sector: Sector, direction: Direction): Sector?
    /** returns the sector in the indicated direction, or null if
     * there is no such direction
     */
    {
        return getByCoordinate(sector.ident.plus(direction))
    }
    fun getRandomNeighbour(thisSector: Sector, exclude: MutableList<Sector>, allowEntries: Boolean = false): Sector?
    /** returns a random neighbour of this sector that is not already
     * included in the list
     * @param allowEntries: whether to avoid entries as possible neighbour (false) or allow them (true) */
    {
        val possibleDirections = Direction.values()
        possibleDirections.shuffle()

        for (dir in possibleDirections)
        {
            val otherSector = getNeighbour(thisSector, dir)
            if (otherSector !in exclude &&
                (allowEntries || otherSector?.type != SectorType.ENTRY))
                otherSector?.let {
                    thisSector.exitsUsed.add(dir)
                    it.entriesUsed.add(dir)
                    return it
            }
        }
        return null
    }

    fun linkIdent(node1: Chip, node2: Chip): Int
    /** @returns a unique ident for the connection between node1 and node2 */
    {
        return "%02d%02d".format(node1.data.ident, node2.data.ident).toInt()
    }

    private fun createTrackFromPath(path: Path): MutableList<Int>
    {
        var linkList = mutableListOf<Int>()
        try {
            var lastNode = path.nodes[0]
            for (node in path.nodes.subList(1, path.nodes.size))
            {
                stage.createLink(lastNode.data.ident, node.data.ident, linkIdent(lastNode, node),
                    mask = getMask())?.let { linkList.add(it.ident) }
                lastNode = node
            }
        }
        catch (ex: Exception)
        {}
        return linkList
    }

    class SectorCoord(var horizontal: Int, var vertical: Int)
    /** coordinate of a sector, consisting of horizontal and vertical component */
    {
        fun isEqual (other: SectorCoord?): Boolean
        {
            other?.let {
                return it.horizontal == this.horizontal && it.vertical == this.vertical
            }
            return false
        }
        fun plus(other: SectorCoord?): SectorCoord
        {
            other?.let {
                return SectorCoord(horizontal+it.horizontal, vertical+other.vertical)
            }
            return this
        }

        private fun displacement(direction: Direction): SectorCoord {
            return when (direction) {
                Direction.UP -> SectorCoord(0, -1)
                Direction.DOWN -> SectorCoord(0, 1)
                Direction.LEFT -> SectorCoord(-1, 0)
                Direction.RIGHT -> SectorCoord(1, 0)
                Direction.DOWNLEFT -> SectorCoord(-1, 1)
            }
        }

        fun plus(direction: Direction): SectorCoord
        {
            return this.plus(displacement(direction))
        }

        fun minus(other: SectorCoord?): SectorCoord
        {
            other?.let {
                return SectorCoord(horizontal-it.horizontal, vertical-other.vertical)
            }
            return this
        }

        fun minus(direction: Direction): SectorCoord
        {
            return this.minus(displacement(direction))
        }
    }

    class Path()
    /** a list of sectors that constitute a continuous path from an entry to an exit point */
    {
        var sectors = mutableListOf<Sector>()
        var nodes = mutableListOf<Chip>()

        fun makeListOfNodes()
        {
            for (sector in sectors)
                nodes.addAll(sector.nodes)
        }
    }

    inner class Sector(val ident: SectorCoord, val area: Rect)
    /** representing a (virtual) part of the stage that may contain one or several nodes.
     * @param ident: Pair numbering the sectors. NOT the grid position. For instance,
     * if there are 3x3 sectors, _ident_ runs from (0, 0) to (2, 2).
     * @param area The size of the sector, in grid coordinates.
     */
    {
        var type: SectorType = SectorType.NORMAL
        var nodes = CopyOnWriteArrayList<Chip>()
        val model = Random.nextInt(20)
        var possibleEntries = Direction.values() // per default, all directions are permitted
        var possibleExits = Direction.values()
        var exitsUsed = mutableSetOf<Direction>()
        var entriesUsed = mutableSetOf<Direction>()

        fun selectModel(): Model
                /** returns a sector "model" based on which entries/exits are already used,
                 * and which are permitted by the model.
                 *
                 */
        {
            lateinit var model: Model
            for (i in 1 .. 5)
            {
                model = Model(i)
                if (model.isCompatibleWith(entriesUsed, exitsUsed))
                    break
            }
            return model
        }

        fun createNodes() {
            when (type)
            {
                SectorType.ENTRY -> nodes.add(stage.createChip(area.centerX(), area.centerY(), type = Chip.ChipType.ENTRY, ident = nextIdent()))
                SectorType.EXIT -> nodes.add(stage.createChip(area.centerX(), area.centerY(), type = Chip.ChipType.CPU, ident = nextIdent()))
                SectorType.NORMAL -> {
                    var model = selectModel()
                    model.createNodes.invoke(stage, area)
                }
            }
        }

        fun makePath(): CopyOnWriteArrayList<Chip> {
            return nodes
        }

        inner class Model(val number: Int) {
            lateinit var createNodes: (Stage, Rect) -> Unit
            var possibleEntries = Direction.values()
            var possibleExits = Direction.values()

            init {
                when (number) {
                    1 -> {
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                area.centerX(), (area.top + area.centerY()) / 2,
                                nextIdent()
                            )
                            val chip2 = stage.createChip(
                                area.centerX(), (area.bottom + area.centerY()) / 2,
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                        }
                        possibleEntries = arrayOf(Direction.DOWN)
                        possibleExits = arrayOf(
                            Direction.DOWN,
                            Direction.LEFT,
                            Direction.RIGHT,
                            Direction.DOWNLEFT
                        )
                    }

                    2 -> {
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                (area.centerX() + area.left) / 2, area.centerY(),
                                nextIdent()
                            )
                            val chip2 = stage.createChip(
                                (area.centerX() + area.right) / 2, area.centerY(),
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                        }
                        possibleEntries = arrayOf(Direction.RIGHT)
                        possibleExits = arrayOf(Direction.RIGHT, Direction.UP, Direction.DOWN)
                    }

                    3 -> {
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                area.centerX(), (2 * area.top + area.centerY()) / 3,
                                nextIdent()
                            )
                            val chip2 =
                                stage.createChip(area.centerX(), (area.centerY()), nextIdent())
                            val chip3 = stage.createChip(
                                area.centerX(), (2 * area.bottom + area.centerY()) / 3,
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                            nodes.add(chip3)
                        }
                        possibleEntries = arrayOf(Direction.DOWN)
                        possibleExits = arrayOf(Direction.DOWN)
                    }

                    4 -> {
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                (area.centerX() + area.right) / 2,
                                (area.centerY() + area.bottom) / 2,
                                nextIdent()
                            )
                            val chip2 = stage.createChip(
                                (area.centerX() + area.left) / 2,
                                (area.centerY() + area.top) / 2,
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                        }
                        possibleEntries = arrayOf(Direction.LEFT, Direction.UP)
                        possibleExits = arrayOf(Direction.LEFT, Direction.UP)
                    }
                    else -> {
                        createNodes = { stage: Stage, area: Rect ->
                            val newChip = stage.createChip(
                                Random.nextInt(area.left + 2, area.right - 2),
                                Random.nextInt(area.top + 2, area.bottom - 2), nextIdent()
                            )
                            nodes.add(newChip)
                        }
                    }
                }
            }

            fun isCompatibleWith(entries: Set<Direction>, exits: Set<Direction>): Boolean
            {
                for (dir in entries){
                    if (dir !in possibleEntries)
                        return false
                }
                for (dir in exits)
                {
                    if (dir !in possibleExits)
                        return false
                }
                return true
            }
        }
    }
}