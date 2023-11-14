package com.example.cpudefense

import android.graphics.Rect
import com.example.cpudefense.gameElements.Attacker
import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.networkmap.Link
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
    val dimY = 55
    val sectorSizeX = dimX / 3
    val sectorSizeY = (dimY - 5) / 4
    val numberOfSectorsX = dimX / sectorSizeX
    val numberOfSectorsY = dimY / sectorSizeY
    var sectors = mutableListOf<Sector>()

    var chipIdent = 0
    /** global count for the chips */

    enum class Direction {UP, DOWN, LEFT, RIGHT}

    fun nextIdent(): Int
    { chipIdent++; return chipIdent }

    fun createStage(level: Stage.Identifier)
        /**
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

        val identOfEntry = 80  // values 80 to 89 reserved for entry points
        val identOfCpu = 90  // values 90 to 99 reserved for cpus

        val entry = listOf<Chip>(
            stage.createChip(2, 2, type = Chip.ChipType.ENTRY, ident = identOfEntry),
            stage.createChip(dimX-2, 2, type = Chip.ChipType.ENTRY, ident = identOfEntry+1))
        val cpu = listOf<Chip>(
            stage.createChip(dimX-2, dimY-2, type = Chip.ChipType.CPU, ident = identOfCpu))
        val exitPos = SectorCoord(numberOfSectorsX-1, numberOfSectorsY-1)

        // cut the stage area into sectors
        val sectorArea = Rect(0, 0, sectorSizeX, sectorSizeY)
        for (x in 0 until numberOfSectorsX)
            for (y in 0 until numberOfSectorsY) {
                var sectorIdent = SectorCoord(x, y)
                var sector = Sector(sectorIdent, Rect(sectorArea).setTopLeft(x*sectorSizeX, y*sectorSizeY))
                sector.createNodes()
                sectors.add(sector)
                if (sectorIdent.isEqual(exitPos))
                    sector.isExit = true
            }

        for (trackNumber in 0 until 10)
            createTrack(sectors[0], trackNumber, entry, cpu)

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
            val attackerStrength = (Attacker.powerOfTwo[strength] ?: 65526u).toInt()
            val attackerSpeed = (10 + strength) * 0.08f
            val attackerFrequency = (10 + strength) * 0.01f
            val coins = if (Random.nextFloat()>0.92) 1 else 0
            stage.createWave(attackerCount, attackerStrength, attackerFrequency, attackerSpeed, coins = coins)
        }
        stage.data.maxWaves = stage.waves.size
    }

    fun createTrack(startSector: Sector, ident: Int, entries: List<Chip>, exits: List<Chip>): Unit
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
    
    fun createPath(firstSector: Sector): MutableList<Sector>?
    /** tries to find a path to an exit sector.
     * @param firstSector Start of path
     * @return list of sectors, or null if no path is found */
    {
        for (count in 1 .. 10) // number of tries
            pathToExit(firstSector)?.let { return it}
        return null
    }

    fun pathToExit(firstSector: Sector): MutableList<Sector>?
    /** create a random path of sectors, starting with firstSector.
     * @return a list of sectors ending with an "exit" sector, or null if
     * the path does not reach an exit sector */
    {
        var sectorPath = mutableListOf<Sector>()
        var sector = firstSector
        var nextSector: Sector? = null
        do {
            nextSector = getRandomNeighbour(sector, sectorPath)
            nextSector?.let {
                sectorPath.add(it)
                sector = it
                if (nextSector.isExit)
                    return sectorPath
            }
        } while (nextSector != null)
        return null
    }

    fun makeTrackFromPath(stage: Stage, nodes: List<Chip>, entry: Chip, exit: Chip): MutableList<Int>
            /** converts a list of nodes into a list of links between them.
             * Entry and exit are added to the list.
             * @return list of link idents
             */
    {
        var linkList = mutableListOf<Int>()
        var lastNode = entry
        for (node in nodes)
        {
            stage.createLink(lastNode.data.ident, node.data.ident, linkIdent(lastNode, node), mask = getMask())?.let { linkList.add(it.ident) }
            lastNode = node
        }
        stage.createLink(lastNode.data.ident, exit.data.ident, linkIdent(lastNode, exit), mask = 0x07)?.let { linkList.add(it.ident) }
        return linkList
    }

    fun getMask(): Int
    {
        return when (Random.nextInt(5))
        {
            0 -> 0x06
            1 -> 0x03
            else -> 0x02
        }
    }

    fun getNeighbour(sector: Sector, direction: Direction): Sector?
    {
        val displacement = when (direction)
        {
            Direction.UP -> SectorCoord(0,-1)
            Direction.DOWN -> SectorCoord(0,1)
            Direction.LEFT -> SectorCoord(-1,0)
            Direction.RIGHT -> SectorCoord(1,0)
        }
        val identOfTarget = sector.ident.plus(displacement)
        val res = sectors.find { sec -> sec.ident.isEqual(identOfTarget) }
        return res
    }
    fun getRandomNeighbour(sector: Sector, exclude: MutableList<Sector>): Sector?
            /** returns a random neighbour of this sector that is not already
             * included in the list */
    {
        var neighbours = mutableListOf<Sector>()
        for (dir in Direction.values())
            getNeighbour(sector, dir)?.let { neighbours.add(it) }
        for (sec in neighbours.shuffled()) {
            if (sec in this.sectors && sec !in exclude)
                return sec
        }
        return null
    }

    fun linkIdent(node1: Chip, node2: Chip): Int
    {
        return "%02d%02d".format(node1.data.ident, node2.data.ident).toInt()
    }

    class SectorCoord(var horizontal: Int, var vertical: Int)
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
    }

    inner class Sector(val ident: SectorCoord, val area: Rect)
    /** representing a (virtual) part of the stage that may contain one or several nodes.
     * @param ident: Pair numbering the sectors. NOT the grid position. For instance,
     * if there are 3x3 sectors, _ident_ runs from (0, 0) to (2, 2).
     * @param area The size of the sector, in grid coordinates.
     */
    {
        var isEntry = false
        var isExit = false
        var nodes = CopyOnWriteArrayList<Chip>()
        val model = Random.nextInt(20)

        fun createNodes()
        {
            when (model)
            {
                in 0..2 -> {
                    val chip1 = stage.createChip(area.centerX(), (area.top+area.centerY())/2, nextIdent())
                    val chip2 = stage.createChip(area.centerX(), (area.bottom+area.centerY())/2, nextIdent())
                    nodes.add(chip1)
                    nodes.add(chip2)
                }
                in 3 .. 5 -> {
                    val chip1 = stage.createChip((area.centerX()+area.right)/2, area.centerY(), nextIdent())
                    val chip2 = stage.createChip((area.centerX()+area.left)/2, area.centerY(), nextIdent())
                    nodes.add(chip1)
                    nodes.add(chip2)
                }
                6 -> {
                    val chip1 = stage.createChip(area.centerX(), (area.top+area.centerY())/2, nextIdent())
                    val chip2 = stage.createChip(area.centerX(), (area.centerY()), nextIdent())
                    val chip3 = stage.createChip(area.centerX(), (area.bottom+area.centerY())/2, nextIdent())
                    nodes.add(chip1)
                    nodes.add(chip2)
                    nodes.add(chip3)
                }
                else -> {
                    val newChip = stage.createChip(Random.nextInt(area.left+1, area.right-1),
                        Random.nextInt(area.top+1, area.bottom-1), nextIdent())
                    nodes.add(newChip)
                }
            }
        }

        fun makePath(): CopyOnWriteArrayList<Chip>
        {
            return nodes
        }
    }

}