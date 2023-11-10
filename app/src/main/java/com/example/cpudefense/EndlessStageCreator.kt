package com.example.cpudefense

import android.graphics.Rect
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
    val dimY = 50
    val sectorSizeX = dimX / 3
    val sectorSizeY = (dimY -5) / 3
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

        val entry = stage.createChip(2, 2, type = Chip.ChipType.ENTRY, ident = identOfEntry)
        val cpu = stage.createChip(dimX-2, dimY-2, type = Chip.ChipType.CPU, ident = identOfCpu)

        // cut the stage area into sectors
        val sectorArea = Rect(0, 0, sectorSizeX, sectorSizeY)
        for (x in 0 until dimX/sectorSizeX)
            for (y in 0 until dimY/sectorSizeY) {
                var sectorIdent = SectorCoord(x, y)
                var sector = Sector(sectorIdent, Rect(sectorArea).setTopLeft(x*sectorSizeX, y*sectorSizeY))
                sector.createNodes(1)
                sectors.add(sector)
            }

        // create random paths of sectors
        var sectorPath = mutableListOf<Sector>()
        var firstSector = sectors[0] // starting sector becomes first element
        firstSector?.let {
            var sector = it
            do {
                var nextSector = getRandomNeighbour(sector, sectorPath)
                nextSector?.let {
                    sectorPath.add(it)
                    sector = it
                }} while (nextSector != null)
        }

        var path = mutableListOf<Chip>()
        sectorPath.forEach() {
            path += it.makePath()
        }
        var track = makeTrackFromPath(stage, path, entry, cpu)
        stage.createTrack(track, 0)

        /*

        val numberOfSlots = Random.nextInt(2, 4)
        var nodeIds = (1 .. numberOfSlots).asSequence()
        /** list of idents for each slot, excluding entry and CPU */
        for (id in nodeIds)
        {
            createChip(Random.nextInt(dimX), Random.nextInt(dimY), id)
        }
        var path = List(3) { Path(this) }
        for ((id, p) in path.withIndex())
        {
            p.nodeIds.add(identOfEntry)
            p.nodeIds.addAll(nodeIds.shuffled())
            p.nodeIds.add(identOfCpu)
            createTrack(p.createTrackFromNodes(), id)
        }

         */
        stage.createWave(10, 5, 0.1f, 1.5f)
        stage.data.maxWaves = stage.waves.size
        return
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
            stage.createLink(lastNode.data.ident, node.data.ident, linkIdent(lastNode, node), mask = 0x02)?.let { linkList.add(it.ident) }
            lastNode = node
        }
        stage.createLink(lastNode.data.ident, exit.data.ident, linkIdent(lastNode, exit), mask = 0x06)?.let { linkList.add(it.ident) }
        return linkList
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

    class Path(val stage: Stage)
    /** list of node IDs that are later connected by a Track */
    {
        var nodeIds = CopyOnWriteArrayList<Int>()
        var links = CopyOnWriteArrayList<Link>()

        fun createTrackFromNodes(): List<Int>
        {
            var prevNodeId = nodeIds[0]
            for (nodeId in nodeIds.drop(1))  // loop through the idents, starting with 2nd element
            {
                val link = stage.createLink(prevNodeId, nodeId, prevNodeId*100+nodeId)
                links.add(link)
                prevNodeId = nodeId
            }
            return links.map { it.data.ident }
        }
    }

    inner class Sector(val ident: SectorCoord, val area: Rect)
    /** representing a (virtual) part of the stage that may contain one or several nodes.
     * @param ident: Pair numbering the sectors. NOT the grid position. For instance,
     * if there are 3x3 sectors, _ident_ runs from (0, 0) to (2, 2).
     * @param area The size of the sector, in grid coordinates.
     */
    {
        var nodes = CopyOnWriteArrayList<Chip>()

        fun createNodes(count: Int = 1)
        {
            for (i in 1 .. count)
            {
                val newChip = stage.createChip(Random.nextInt(area.left+1, area.right-1),
                    Random.nextInt(area.top+1, area.bottom-1), nextIdent())
                nodes.add(newChip)
            }
        }

        fun makePath(): CopyOnWriteArrayList<Chip>
        {
            return nodes
        }
    }

}