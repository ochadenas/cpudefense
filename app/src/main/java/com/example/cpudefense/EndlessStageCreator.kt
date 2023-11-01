package com.example.cpudefense

import com.example.cpudefense.gameElements.Chip
import com.example.cpudefense.networkmap.Link
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.random.Random

class EndlessStageCreator
/**
 * This class provides methods to generate an aritrary number of random stage objects.
 * Like StageCatalog, it is not meant to be instantiated.
 */
{
    companion object {
        fun createStage(stage: Stage, level: Stage.Identifier)
            /**
             * @param stage Empty stage that must be filled with data
             * @param level The identifier of the level to be created
             */
        {
            with(stage)
            {
                data.ident = level
                waves.clear()
                data.type = Stage.Type.REGULAR
                val dimX = 40
                val dimY = 40
                data.chipsAllowed =
                    setOf(Chip.ChipUpgrades.ACC, Chip.ChipUpgrades.SUB, Chip.ChipUpgrades.SHR,
                        Chip.ChipUpgrades.MEM, Chip.ChipUpgrades.CLK, Chip.ChipUpgrades.POWERUP,
                        Chip.ChipUpgrades.REDUCE, Chip.ChipUpgrades.SELL )
                initializeNetwork(dimX, dimY)

                val identOfEntry = 80  // values 80 to 89 reserved for entry points
                val identOfCpu = 90  // values 90 to 99 reserved for cpus

                createChip(2, 2, type = Chip.ChipType.ENTRY, ident = identOfEntry)
                createChip(dimX-2, dimY-2, type = Chip.ChipType.CPU, ident = identOfCpu)
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
                createWave(10, 5, 0.01f, 1.5f)
            }
            return
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

    }
}