package com.example.cpudefense

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.networkmap.Link
import com.example.cpudefense.networkmap.Network
import com.example.cpudefense.networkmap.Node
import com.example.cpudefense.networkmap.Viewport
import com.example.cpudefense.utils.setTopLeft
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt
import kotlin.random.Random

class EndlessStageCreator(val stage: Stage)
/**
 * This class provides methods to generate an arbitrary number of random stage objects.
 * Like StageCatalog, it is not meant to be instantiated.
 */
{
    private var sectorSizeX = 16
    private var sectorSizeY = 12
    private var dimX: Int = 0
    private var dimY: Int = 0
    private var numberOfSectorsX = 0
    private var numberOfSectorsY = 0
    private var sectors = mutableListOf<Sector>()
    private val paths = mutableListOf<Path>()

    private var chipIdent = 0
    /** global count for the chips */

    enum class Direction {UP, DOWN, LEFT, RIGHT }
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

        /* determine difficulties based on level */
        val numberOfSectors: Pair<Int, Int> = when (Random.nextInt(level.number+7))
        {
            in 0 .. 5 -> Pair(4, 4)
            in 6 .. 8 -> Pair(3, 5)
            in 9 .. 10 -> Pair(3, 4)
            in 11 .. 12 -> Pair(4, 2)
            in 13 .. 15 -> Pair(2, 4)
            else -> Pair(3, 3)
        }
        numberOfSectorsX = numberOfSectors.first
        numberOfSectorsY = numberOfSectors.second

        val numberOfPaths = when (level.number)
        {
            in 0..3 -> 3
            in 4 .. 10 -> level.number
            else -> 10
        }

        dimX = numberOfSectorsX * sectorSizeX
        dimY = numberOfSectorsY * sectorSizeY
        stage.initializeNetwork(dimX, dimY)
        stage.network.data.sectorSizeX = sectorSizeX
        stage.network.data.sectorSizeY = sectorSizeY

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
        val possibleEntrySectors = listOf(
            SectorCoord(0,0),
            SectorCoord(1,0),
            SectorCoord(2,0),
            SectorCoord(0,1),
        ).shuffled()
        var numberOfEntries = (1 + Random.nextFloat() * sqrt(level.number * 0.2)).toInt()
        if (numberOfEntries>4) numberOfEntries=4
        val entrySectors = mutableListOf<Sector?>()
        possibleEntrySectors.subList(0,numberOfEntries).forEach {
            entrySectors.add(getByCoordinate(it)) }
        entrySectors.forEach { it?.type = SectorType.ENTRY }

        val possibleExitSectors = listOf(
            SectorCoord(numberOfSectorsX-1,numberOfSectorsY-1),
            SectorCoord(numberOfSectorsX-1,numberOfSectorsY-2),
            SectorCoord(numberOfSectorsX-2,numberOfSectorsY-1),
        ).shuffled()
        var numberOfExits = (1 + Random.nextFloat() * sqrt(level.number * 0.1)).toInt()
        if (numberOfExits>3) numberOfExits=3
        val exitSectors = mutableListOf<Sector?>()
        possibleExitSectors.subList(0,numberOfExits).forEach {
            exitSectors.add(getByCoordinate(it)) }
        exitSectors.forEach { it?.type = SectorType.EXIT }

        for (count in 1 .. numberOfPaths)
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

        // remove isolated chips
        // stage.network.nodes.entries.removeIf { it.value.connectedLinks.size == 0 }  // preferred solution, but requires API24

        // solution by copying the whole hash map:
        val nodesWithConnectors: Map<Int, Node> = stage.network.nodes.filter { it.value.connectedLinks.size > 0 }
        stage.network.nodes = nodesWithConnectors as HashMap<Int, Node>
        stage.chips = nodesWithConnectors as HashMap<Int, Chip>

        // set mask for the graphical representations of the links
        for (link in stage.network.links.values)
            setMask(link)

        createWaves()
        stage.rewardCoins = 3
        return
    }

    fun createWaves()
    {
        val levelNumber = stage.data.ident.number
        val waveCount = sqrt(2 * levelNumber.toDouble()).toInt() + 2
        for (waveNumber in 1 .. waveCount) {
            val attackerCount = 16
            val strength = (waveNumber+levelNumber)/2
            val attackerStrength = (Attacker.powerOfTwo[strength] ?: 1048576u).toInt()
            val attackerSpeed = (16 + strength) * 0.06f
            val attackerFrequency = (8 + strength) * 0.006f
            val coins = if (Random.nextFloat()>0.92) 1 else 0
            stage.createWave(attackerCount, attackerStrength, attackerFrequency, attackerSpeed, coins = coins)
        }
        stage.data.maxWaves = stage.waves.size
    }

    private fun createPath(firstSector: Sector): Path?
    /** tries to find a path to an exit sector.
     * @param firstSector Start of path
     * @return list of sectors, or null if no path is found */
    {
        for (count in 1 .. 10) // number of tries
            pathToExit(firstSector)?.let { return it}
        return null
    }

    private fun pathToExit(firstSector: Sector): Path?
    /** create a random path of sectors, starting with firstSector.
     * @return a list of sectors ending with an "exit" sector, or null if
     * the path does not reach an exit sector */
    {
        val sectorPath = Path()
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

    @Suppress("UNUSED_PARAMETER")
    private fun getMask(link: Link? = null): Int
    /** returns a random value for the link mask */
    {
        return 0x06
    }

    private fun setMask(link: Link?)
    {
        link?.let {
            when (link.usageCount) {
                0 -> it.mask = 0x08 // should not happen
                1 -> it.mask = 0x01
                2 -> it.mask = 0x06
                3 -> it.mask = 0x07
                else -> it.mask = 0x0F
            }
        }
    }

    private fun getByCoordinate(coord: SectorCoord): Sector?
    {
        return sectors.find { sec -> sec.ident.isEqual(coord) }
    }

    private fun getNeighbour(sector: Sector, direction: Direction): Sector?
    /** returns the sector in the indicated direction, or null if
     * there is no such direction
     */
    {
        return getByCoordinate(sector.ident.plus(direction))
    }
    private fun getRandomNeighbour(thisSector: Sector, exclude: MutableList<Sector>, allowEntries: Boolean = false): Sector?
    /** returns a random neighbour of this sector that is not already
     * included in the list
     * @param allowEntries: whether to avoid entries as possible neighbour (false) or allow them (true) */
    {
        // val possibleDirections = Direction.values()
        val possibleDirections = mutableListOf(Direction.DOWN, Direction.LEFT, Direction.RIGHT)
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

    private fun linkIdent(node1: Chip, node2: Chip): Int
    /** @return a unique ident for the connection between node1 and node2 */
    {
        return "%02d%02d".format(node1.data.ident, node2.data.ident).toInt()
    }

    private fun createTrackFromPath(path: Path): MutableList<Int>
    {
        val linkList = mutableListOf<Int>()
        try {
            var lastNode = path.nodes[0]
            for (node in path.nodes.subList(1, path.nodes.size))
            {
                stage.createLink(lastNode.data.ident, node.data.ident, linkIdent(lastNode, node),
                    mask = getMask())?.let { linkList.add(it.ident) }
                lastNode = node
            }
        }
        catch (_: Exception)
        {}
        return linkList
    }

    class SectorCoord(private var horizontal: Int, var vertical: Int)
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

    class Path
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
        var exitsUsed = mutableSetOf<Direction>()
        var entriesUsed = mutableSetOf<Direction>()

        private fun selectModel(): Model
                /** returns a sector "model" based on which entries/exits are already used,
                 * and which are permitted by the model.
                 *
                 */
        {
            var model: Model? = null
            for (i in (1..14).toList().shuffled())
            {
                if (Model(i).isCompatibleWith(entriesUsed, exitsUsed))
                {
                    model = Model(i)
                    break
                }
            }
            return model ?: Model(0)
        }

        fun createNodes() {
            when (type)
            {
                SectorType.ENTRY -> nodes.add(stage.createChip(area.centerX(), area.centerY(), type = Chip.ChipType.ENTRY, ident = nextIdent()))
                SectorType.EXIT -> nodes.add(stage.createChip(area.centerX(), area.centerY(), type = Chip.ChipType.CPU, ident = nextIdent()))
                SectorType.NORMAL -> {
                    val model = selectModel()
                    model.createNodes.invoke(stage, area)
                }
            }
        }

        inner class Model(val number: Int) {
            var createNodes: (Stage, Rect) -> Unit
            private var possibleEntries = Direction.values()
            private var possibleExits = Direction.values()

            init {
                when (number) {
                    1 -> {
                        // three nodes in a vertical line
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
                        possibleEntries = arrayOf(Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                        possibleExits = arrayOf(Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                    }
                    2 -> {
                        // three chips, angled
                        createNodes = { stage: Stage, area: Rect ->
                            val x1 = (area.left + area.centerX()) / 2
                            val x2 = (2 * area.right + area.centerX()) / 3
                            val y1 = (area.top + area.centerY()) / 2
                            val y2 = (area.bottom + area.centerY()) / 2
                            val chip1 = stage.createChip(x1, y1, nextIdent())
                            val chip2 = stage.createChip(x2, y1, nextIdent())
                            val chip3 = stage.createChip(x2, y2, nextIdent())
                            nodes.add(chip1)
                            nodes.add(chip2)
                            nodes.add(chip3)
                        }
                        possibleEntries = arrayOf(Direction.DOWN, Direction.RIGHT)
                        possibleExits = arrayOf(Direction.DOWN, Direction.RIGHT)
                    }
                    3 -> {
                        // two chips diagonal
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
                    4 -> {
                        // two chips diagonal
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                (area.centerX() + area.right) / 2,
                                (area.centerY() + area.top) / 2,
                                nextIdent()
                            )
                            val chip2 = stage.createChip(
                                (area.centerX() + area.left) / 2,
                                (area.centerY() + area.bottom) / 2,
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                        }
                        possibleEntries = arrayOf(Direction.LEFT, Direction.DOWN)
                        possibleExits = arrayOf(Direction.LEFT, Direction.DOWN)
                    }
                    5 -> {
                        // two nodes in a vertical line
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
                        possibleEntries = arrayOf(Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                        possibleExits = arrayOf(Direction.DOWN, Direction.LEFT, Direction.RIGHT)
                    }
                    6-> {
                        // two nodes in a horizontal line
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
                        possibleEntries = arrayOf(Direction.RIGHT, Direction.UP, Direction.DOWN)
                        possibleExits = arrayOf(Direction.RIGHT, Direction.UP, Direction.DOWN)
                    }
                    7 -> {
                        // three chips, angled
                        createNodes = { stage: Stage, area: Rect ->
                            val x1 = (area.left + area.centerX()) / 2
                            val x2 = (2 * area.right + area.centerX()) / 3
                            val y1 = (area.top + area.centerY()) / 2
                            val y2 = (area.bottom + area.centerY()) / 2
                            val chip1 = stage.createChip(x2, y1, nextIdent())
                            val chip2 = stage.createChip(x1, y1, nextIdent())
                            val chip3 = stage.createChip(x1, y2, nextIdent())
                            nodes.add(chip1)
                            nodes.add(chip2)
                            nodes.add(chip3)
                        }
                        possibleEntries = arrayOf(Direction.DOWN, Direction.LEFT)
                        possibleExits = arrayOf(Direction.DOWN, Direction.RIGHT, Direction.LEFT)
                    }
                    8 -> {
                        // four nodes
                        createNodes = { stage: Stage, area: Rect ->
                            val chip1 = stage.createChip(
                                (area.centerX()+area.left)/2, (area.centerY()+area.top)/2,
                                nextIdent()
                            )
                            val chip2 = stage.createChip(
                                (area.centerX()+area.right)/2, (area.centerY()+area.top)/2,
                                nextIdent()
                            )
                            val chip3 = stage.createChip(
                                (area.centerX()+area.right)/2, (area.centerY()+area.bottom)/2,
                                nextIdent()
                            )
                            val chip4 = stage.createChip(
                                (area.centerX()+area.left)/2, (area.centerY()+area.bottom)/2,
                                nextIdent()
                            )
                            nodes.add(chip1)
                            nodes.add(chip2)
                            nodes.add(chip3)
                            nodes.add(chip4)
                        }
                        possibleEntries = arrayOf(Direction.RIGHT, Direction.DOWN)
                        possibleExits = arrayOf(Direction.LEFT, Direction.DOWN)
                    }
                    in 9 .. 10 -> {
                        // one node
                        createNodes = { stage: Stage, area: Rect ->
                            val newChip = stage.createChip(
                                Random.nextInt(area.left + 2, area.right - 2),
                                Random.nextInt(area.top + 2, area.bottom - 2), nextIdent()
                            )
                            nodes.add(newChip)
                        }
                    }
                    else -> {
                        // one node, centered
                        createNodes = { stage: Stage, area: Rect ->
                            val newChip = stage.createChip(area.centerX(), area.centerY(), nextIdent())
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

    companion object {
        fun displaySectors(canvas: Canvas, viewport: Viewport, data: Network.Data)
        {
            val numberSectorsX = data.gridSizeX / data.sectorSizeX
            val numberSectorsY = data.gridSizeY / data.sectorSizeY
            val paint = Paint()
            paint.color = Color.WHITE
            paint.style = Paint.Style.STROKE
            for (x in 0 until numberSectorsX)
                for (y in 0 until numberSectorsY)
                {
                    val sector = Rect(x*data.sectorSizeX, y*data.sectorSizeY, (x+1)*data.sectorSizeX, (y+1)*data.sectorSizeY)
                    canvas.drawRect(viewport.rectToViewport(sector), paint)
                }
        }
    }
}