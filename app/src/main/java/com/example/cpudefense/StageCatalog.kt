package com.example.cpudefense

import com.example.cpudefense.gameElements.*
import com.example.cpudefense.networkmap.Link
import kotlin.math.sqrt
import kotlin.random.Random

class StageCatalog
{
    /** This class is used to create all the pre-defined stages in series 1 and 2.
     * It is not meant to be instantiated.
     */
    companion object {
        private val possibleChipTypesWhereObstaclesCanBePut =
            setOf(Chip.ChipType.EMPTY, Chip.ChipType.ADD, Chip.ChipType.SHL, Chip.ChipType.NOP)
        private val possibleChipTypesWhereChipsCanBePut =
            setOf(Chip.ChipType.EMPTY, Chip.ChipType.SUB, Chip.ChipType.SHR, Chip.ChipType.RES, Chip.ChipType.MEM, Chip.ChipType.ACC)

        fun createStage(stage: Stage, level: Stage.Identifier) {
            when (level.series) {
                GameMechanics.SERIES_NORMAL ->
                    createStageWithoutObstacles(stage, level)

                GameMechanics.SERIES_TURBO -> {
                    createStageWithoutObstacles(stage, level)  // make basic layout
                    val difficulty = when (level.number) {
                        1 -> 0
                        2 -> 1
                        3 -> 2
                        4 -> 2
                        5 -> 3
                        6 -> 3
                        7 -> 4
                        8 -> 4
                        9 -> 3
                        10 -> 4
                        11 -> 3
                        22 -> 2
                        24 -> 4
                        27 -> 4
                        29 -> 3
                        else -> 6
                    }
                    createObstaclesForDifficulty(stage, difficulty.toDouble())
                    createAdditionalChips(stage)
                }

                GameMechanics.SERIES_ENDLESS -> {
                    // if the stage is in the save file (from an earlier try on this level),
                    // restore the structure. Otherwise, create an empty level.
                    // Depending on the settings, always create a new random level.
                    val structure: HashMap<Int, Stage.Data> =
                        Persistency(stage.gameView.gameActivity).loadLevelStructure(GameMechanics.SERIES_ENDLESS)
                    if (stage.gameView.gameActivity.settings.keepLevels)
                        structure[level.number]?.let {
                            Stage.fillEmptyStageWithData(stage, it)
                            EndlessStageCreator(stage).createWaves()
                            stage.calculateDifficulty()
                        }
                    else {
                        while (stage.data.difficulty > 900f) {
                            EndlessStageCreator(stage).createStage(level)
                            stage.calculateDifficulty() // avoid levels that are impossible to play
                        }
                        val targetDifficulty = 4 + 1.5 * sqrt(level.number.toDouble())
                        createObstaclesForDifficulty(stage, targetDifficulty - stage.data.difficulty)
                        createAdditionalChips(stage)
                        stage.provideStructureData()
                        structure[level.number] = stage.data
                        Persistency(stage.gameView.gameActivity).saveLevelStructure(GameMechanics.SERIES_ENDLESS, structure)
                    }
                    stage.rewardCoins = GameMechanics.defaultRewardCoins
                }
            }
            stage.calculateDifficulty()
        }

        private fun createFixedNumberOfObstacles(stage: Stage, numberOfObstacles: Int) {
            val reduce =
                stage.gameMechanics.heroModifier(Hero.Type.LIMIT_UNWANTED_CHIPS) // consider Kilby's effect
            val reducedNumberOfObstacles = numberOfObstacles - reduce.toInt()
            if (reducedNumberOfObstacles > 0)
                for (i in 1..reducedNumberOfObstacles)  // set or upgrade the slots
                {
                    val possibleSlotsForObstacles =
                        stage.chips.values.filter { it.chipData.type in possibleChipTypesWhereObstaclesCanBePut }
                    if (possibleSlotsForObstacles.isNotEmpty()) {
                        val obstacleSlot = possibleSlotsForObstacles.random()
                        when (obstacleSlot.chipData.type) {
                            in listOf(Chip.ChipType.ADD, Chip.ChipType.SHL, Chip.ChipType.NOP) -> obstacleSlot.addPower(1)
                            Chip.ChipType.EMPTY -> obstacleSlot.setType(Chip.obstacleTypes.random())
                            else -> {}
                        }
                    }
                }
        }

        private fun createObstaclesForDifficulty(stage: Stage, difficulty: Double)
        /** creates an undetermined number of obstacles for the given stage,
         * so that the cumulated "strength" of these obstacles does not exceed the given difficulty.
         * Difficulty values may be < 0.
         */
        {
            val reduce = stage.gameMechanics.heroModifier(Hero.Type.LIMIT_UNWANTED_CHIPS)
            val targetDifficulty = difficulty - reduce
            var stageDifficulty = stage.difficultyOfObstacles()
            while (stageDifficulty < targetDifficulty) {
                val possibleSlotsForObstacles =
                    stage.chips.values.filter { it.chipData.type in possibleChipTypesWhereObstaclesCanBePut }
                if (possibleSlotsForObstacles.isNotEmpty()) {
                    val obstacleSlot = possibleSlotsForObstacles.random()
                    when (obstacleSlot.chipData.type) {
                        in listOf(Chip.ChipType.ADD, Chip.ChipType.SHL, Chip.ChipType.NOP) -> obstacleSlot.addPower(1)
                        Chip.ChipType.EMPTY -> obstacleSlot.setType(Chip.obstacleTypes.random())
                        else -> {}
                    }
                    stageDifficulty = stage.difficultyOfObstacles()
                } else
                    return // no more obstacles can be placed
            }
        }

        private fun createAdditionalChips(stage: Stage)
        /** creates an undetermined number of additional chips for the given stage,
         * so that the cumulated "strength" of these chips does not exceed the given difficulty.
         */
        {
            var additionalCount = stage.gameMechanics.heroModifier(Hero.Type.CREATE_ADDITIONAL_CHIPS).toDouble()
            while (additionalCount > 0) {
                val possibleSlotsForChips = stage.chips.values.filter { it.chipData.type in possibleChipTypesWhereChipsCanBePut }
                if (possibleSlotsForChips.isNotEmpty()) {
                    val slot = possibleSlotsForChips.random()
                    when (slot.chipData.type) {
                        in listOf(Chip.ChipType.SUB, Chip.ChipType.SHR, Chip.ChipType.RES) -> {
                            // chip already present, buy an upgrade
                            val upgrade = ChipUpgrade(slot, Chip.ChipUpgrades.POWERUP, 0, 0, 0)
                            upgrade.buyUpgrade(Chip.ChipUpgrades.POWERUP, doForFree = true)
                            additionalCount -= Chip.chipStrength[slot.chipData.type] ?: 0.0
                        }
                        Chip.ChipType.MEM -> {}
                        Chip.ChipType.ACC -> {}
                        Chip.ChipType.EMPTY -> {
                            Chip.desiredTypes.random().let {
                                slot.setType(it)
                                additionalCount -= Chip.chipStrength[it] ?: 0.0
                            }
                        }
                        else -> {}
                    }
                } else
                    return // no more chips can be placed
            }
        }


        private fun createStageWithoutObstacles(stage: Stage, level: Stage.Identifier) {
            stage.data.ident = level
            stage.waves.clear()
            stage.data.type = Stage.Type.REGULAR
            stage.data.chipsAllowed =
                setOf(
                        Chip.ChipUpgrades.ACC,
                        Chip.ChipUpgrades.SUB,
                        Chip.ChipUpgrades.SHR,
                        Chip.ChipUpgrades.MEM,
                        Chip.ChipUpgrades.CLK,
                        Chip.ChipUpgrades.POWERUP,
                        Chip.ChipUpgrades.REDUCE,
                        Chip.ChipUpgrades.SELL
                )

            val specialLevel = GameMechanics.specialLevel(stage.data.ident)
            if (specialLevel != GameMechanics.Params.Season.DEFAULT)
                createSpecialStageWithoutObstacles(stage, level, specialLevel)
            else with(stage)
            {
                when (stage.getLevel()) {
                    1 -> {
                        initializeNetwork(40, 40)

                        createChip(20, 1, type = Chip.ChipType.ENTRY)
                        createChip(15, 20, 1)
                        createChip(20, 38, type = Chip.ChipType.CPU)

                        createLink(0, 1, 0, mask = 0x06)
                        createLink(1, 999, 1, mask = 0x06)

                        createTrack(listOf(0, 1), 0)

                        createWave(4, 1, .075f, 1f)

                        data.chipsAllowed = setOf(Chip.ChipUpgrades.SUB)
                    }

                    2 -> {
                        initializeNetwork(40, 40)

                        createChip(20, 1, type = Chip.ChipType.ENTRY)
                        createChip(15, 20, 1)
                        createChip(20, 38, type = Chip.ChipType.CPU)

                        createLink(0, 1, 0, mask = 0x06)
                        createLink(1, 999, 1, mask = 0x06)

                        createTrack(listOf(0, 1), 0)

                        createWave(8, 1, .075f, 1f)
                        createWave(8, 1, .075f, 1f)

                        data.chipsAllowed = setOf(Chip.ChipUpgrades.SUB)
                    }

                    3 -> {
                        initializeNetwork(50, 50)

                        createChip(8, 1, type = Chip.ChipType.ENTRY)
                        createChip(20, 18, 1)
                        createChip(20, 22, 2)
                        createChip(45, 30, 3)
                        createChip(45, 40, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 999, 4)

                        createTrack(listOf(1, 2, 3, 4), 0)

                        createWave(4, 1, .075f, 1f)
                        createWave(10, 1, .075f, 1f)
                        createWave(10, 1, .075f, 1f)
                        createWave(10, 1, .075f, 1f)

                        data.chipsAllowed = setOf(Chip.ChipUpgrades.SUB, Chip.ChipUpgrades.POWERUP)
                    }

                    4 -> {
                        initializeNetwork(50, 50)

                        createChip(1, 1, type = Chip.ChipType.ENTRY)
                        createChip(10, 12, 1)
                        createChip(15, 20, 2)
                        createChip(30, 30, 3)
                        createChip(35, 45, 4)
                        createChip(20, 35, 5)
                        createChip(45, 45, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3, mask = 0x03)
                        createLink(3, 4, 4, mask = 0x03)
                        createLink(2, 5, 5, mask = 0x0C)
                        createLink(5, 4, 6, mask = 0x0C)
                        createLink(4, 999, 7)

                        createTrack(listOf(1, 2, 3, 4, 7), 0)
                        createTrack(listOf(1, 2, 5, 6, 7), 1)

                        createWave(10, 1, .1f, 1f)
                        createWave(10, 2, .1f, 1f)
                        createWave(10, 2, .1f, 1f)
                        createWave(10, 3, .15f, 1f)

                        data.chipsAllowed = setOf(Chip.ChipUpgrades.SUB, Chip.ChipUpgrades.POWERUP)
                    }

                    5 -> {
                        initializeNetwork(50, 50)

                        createChip(1, 15, type = Chip.ChipType.ENTRY)
                        createChip(25, 15, 1)
                        createChip(40, 15, 2)
                        createChip(40, 5, 3)
                        createChip(25, 5, 4)
                        createChip(25, 25, 5)
                        createChip(25, 35, 6)
                        createChip(40, 35, 7)
                        createChip(40, 25, 8)
                        createChip(5, 25, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(4, 1, 5)
                        createLink(1, 5, 6)
                        createLink(5, 6, 7)
                        createLink(6, 7, 8)
                        createLink(7, 8, 9)
                        createLink(8, 5, 10)
                        createLink(5, 999, 11)

                        createTrack((1..11).toList(), 0)

                        createWave(10, 2, .125f, 1.2f)
                        createWave(15, 3, .1250f, 1.1f)
                        createWave(15, 3, .1f, 1.1f)
                        createWave(20, 4, .1f, 1.1f)
                        createWave(20, 7, .050f, 1f)
                        createWave(15, 15, .050f, 1f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                    }

                    6 -> {
                        initializeNetwork(50, 50)

                        createChip(1, 5, type = Chip.ChipType.ENTRY)
                        createChip(25, 15, 1)
                        createChip(40, 15, 2)
                        createChip(40, 5, 3)
                        createChip(25, 5, 4)
                        createChip(25, 25, 5)
                        createChip(25, 35, 6)
                        createChip(40, 35, 7)
                        createChip(40, 25, 8)
                        createChip(5, 35, type = Chip.ChipType.CPU)

                        createLink(0, 4, 1)
                        createLink(4, 3, 2)
                        createLink(4, 1, 3)
                        createLink(3, 2, 4)
                        createLink(1, 2, 5)
                        createLink(1, 5, 6)
                        createLink(2, 8, 7)
                        createLink(5, 8, 8)
                        createLink(5, 6, 9)
                        createLink(8, 7, 10)
                        createLink(6, 7, 11)
                        createLink(6, 999, 12)

                        createTrack(listOf(1, 2, 4, 5, 6, 8, 10, 11, 12), 0)
                        createTrack(listOf(1, 3, 5, 7, 8, 9, 12), 1)

                        createWave(10, 2, .125f, 1.2f, coins = 0)
                        createWave(15, 3, .120f, 1.1f, coins = 0)
                        createWave(15, 3, .110f, 1.1f, coins = 0)
                        createWave(20, 4, .110f, 1.1f)
                        createWave(20, 7, .050f, 1f, coins = 0)
                        createWave(15, 15, .050f, 1f, coins = 0)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    7 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 10, type = Chip.ChipType.ENTRY)
                        createChip(25, 10, 2)
                        createChip(40, 10, 3)
                        createChip(10, 25, 8)
                        createChip(25, 25, type = Chip.ChipType.CPU)
                        createChip(40, 25, 4)
                        createChip(10, 40, 7)
                        createChip(25, 40, 6)
                        createChip(40, 40, 5)


                        createLink(0, 2, 1)
                        createLink(2, 3, 2)
                        createLink(3, 4, 3)
                        createLink(4, 5, 4)
                        createLink(5, 6, 5)
                        createLink(6, 7, 6)
                        createLink(7, 8, 7)
                        createLink(8, 999, 8)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 0), 0)

                        // for testing purposes
                        // createWave(1, 2, .125f, 1.2f)

                        createWave(10, 2, .125f, 1.2f)
                        createWave(15, 3, .120f, 1.1f)
                        createWave(15, 3, .110f, 1.1f)
                        createWave(20, 4, .110f, 1.1f)
                        createWave(20, 7, .050f, 1f)
                        createWave(15, 15, .050f, 1f, coins = 1)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }
                    8 -> {
                        initializeNetwork(50, 50)

                        createChip(40, 45, 0, type = Chip.ChipType.ENTRY)
                        createChip(5, 40, 1)
                        createChip(5, 30, 2)
                        createChip(5, 20, 3)
                        createChip(5, 10, 4)
                        createChip(45, 5, 5)
                        createChip(45, 10, 6)
                        createChip(45, 20, 7)
                        createChip(45, 30, 8)
                        createChip(45, 40, 9)
                        createChip(45, 45, 10, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1, variant = Link.Variant.CONCAVE)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(5, 4, 5, variant = Link.Variant.CONCAVE)
                        createLink(5, 6, 6)
                        createLink(6, 7, 7, variant = Link.Variant.CONCAVE)
                        createLink(7, 8, 8)
                        createLink(8, 9, 9, variant = Link.Variant.CONCAVE)
                        createLink(9,10,10)
                        createLink(2, 8,11,0x06)
                        createLink(3, 7,12,0x06)

                        createTrack(listOf(1, 2, 11, 9, 10), 0)
                        createTrack(listOf(1, 2, 3, 12, 8, 9, 10), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 4)

                        createWave(16, 2, .125f, 1.1f)
                        createWave(20, 3, .120f, 1.3f)
                        createWave(20, 5, .105f, 1.6f)
                        createWave(20, 7, .100f, 1.8f)
                        createWave(20, 11, .090f, 2.0f, coins = 1)
                        createWave(20, 27, .080f, 2.2f, coins = 0)

                        data.chipsAllowed =
                            setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    9 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 12, ident = 0, type = Chip.ChipType.ENTRY)
                        createChip(20, 19, 1)
                        createChip(8, 38, 2)
                        createChip(25, 45, 3)
                        createChip(45, 5, 4, type = Chip.ChipType.ENTRY)
                        createChip(32, 25, 5)
                        createChip(40, 38, 6)
                        createChip(45, 18, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1, mask = 0x07)
                        createLink(1, 2, 2, mask = 0x07)
                        createLink(2, 3, 3, mask = 0x0E)
                        createLink(3, 6, 4, mask = 0x07)
                        createLink(6, 999, 7, mask = 0x07)
                        createLink(4, 5, 5, mask = 0x02)
                        createLink(5, 2, 6, mask = 0x08)

                        createTrack(listOf(1, 2, 3, 4, 7), 0)
                        createTrack(listOf(5, 6, 3, 4, 7), 1)
                        createTrack(listOf(1, 2, 3, 4, 7), 2)

                        createWave(16, 1, .125f, 1.1f)
                        createWave(20, 2, .120f, 1.0f)
                        createWave(20, 3, .110f, 1.0f)
                        createWave(20, 4, .110f, 1.0f)
                        createWave(20, 7, .050f, 1f, coins = 0)
                        createWave(15, 15, .050f, 1f, coins = 0)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SELL,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    10 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 45, type = Chip.ChipType.ENTRY)
                        createChip(10, 35, 1)
                        createChip(20, 20, 2)
                        createChip(30, 5, 3)
                        createChip(10, 5, 4)
                        createChip(30, 35, 5)
                        createChip(40, 35, 6)
                        createChip(48, 35, type = Chip.ChipType.CPU)


                        createLink(0, 1, 1, mask = 0x0C)
                        createLink(1, 2, 2, mask = 0x0C)
                        createLink(2, 3, 3, mask = 0x03, variant = Link.Variant.CONCAVE)
                        createLink(2, 4, 4, mask = 0x0C)
                        createLink(2, 5, 5, mask = 0x03, variant = Link.Variant.CONCAVE)
                        createLink(3, 4, 7, mask = 0x06)
                        createLink(5, 6, 6, mask = 0x06)
                        createLink(6, 999, 8, mask = 0x06)

                        createTrack(listOf(1, 2, 3, 7, 4, 5, 6, 8), 0)

                        createWave(10, 2, .140f, 1.4f)
                        createWave(20, 3, .130f, 1.3f)
                        createWave(10, 5, .120f, 1.2f)
                        createWave(20, 7, .110f, 1.1f)
                        createWave(10, 8, .110f, 1.0f)
                        createWave(20, 12, .110f, 1.0f)
                        createWaveHex(20, 18, .100f, 1.0f)
                        createWaveHex(20, 28, .100f, 1.1f, coins = 1)
                        createWaveHex(20, 40, .100f, 1.2f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SELL,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    11 -> {
                        initializeNetwork(50, 50)

                        createChip(23, 3, 50, type = Chip.ChipType.ENTRY)
                        createChip(25, 5, 51, type = Chip.ChipType.ENTRY)
                        createChip(27, 3, 52, type = Chip.ChipType.ENTRY)
                        createChip(10, 5, 1)
                        createChip(40, 5, 2)
                        createChip(10, 20, 3)
                        createChip(25, 10, 4)
                        createChip(25, 30, 9)
                        createChip(40, 20, 5)
                        createChip(10, 35, 6)
                        createChip(25, 35, 7, type = Chip.ChipType.SUB)
                        createChip(40, 35, 8)
                        createChip(25, 42, type = Chip.ChipType.CPU)
                        chips[7]?.setType(Chip.ChipType.SUB)

                        createLink(50, 1, 1, mask = 0x0E, variant = Link.Variant.CONCAVE)
                        createLink(52, 2, 2, mask = 0x07)
                        createLink(1, 3, 3, mask = 0x0E)
                        createLink(51, 4, 4, mask = 0x07)
                        createLink(2, 5, 5, mask = 0x07)
                        createLink(3, 6, 6, mask = 0x0E)
                        createLink(4, 9, 7, mask = 0x07)
                        createLink(9, 7, 12, mask = 0x07)
                        createLink(5, 8, 8, mask = 0x07)
                        createLink(6, 999, 9, mask = 0x0E, variant = Link.Variant.CONCAVE)
                        createLink(7, 999, 10, mask = 0x07)
                        createLink(8, 999, 11, mask = 0x07)

                        createTrack(listOf(1, 3, 6, 9), 0)
                        createTrack(listOf(4, 7, 12, 10), 1)
                        createTrack(listOf(2, 5, 8, 11), 2)

                        createWave(12, 1, .125f, 1.2f)
                        createWave(12, 1, .125f, 1.1f)
                        createWave(16, 2, .110f, 1.0f)
                        createWave(20, 3, .110f, 1.0f)
                        createWave(20, 4, .100f, 1.0f)
                        createWave(20, 5, .125f, 1.0f)
                        createWave(20, 6, .05f, 1f, coins = 1)
                        createWave(15, 8, .05f, 1f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SELL,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3

                    }

                    12 -> {
                        initializeNetwork(50, 50)

                        createChip(45, 1, type = Chip.ChipType.ENTRY)
                        createChip(34, 5, 1)
                        createChip(12, 15, 2)
                        createChip(45, 12, 6)
                        createChip(30, 20, 3)
                        createChip(25, 32, 4)
                        createChip(40, 40, 5)
                        createChip(12, 23, 7)
                        createChip(10, 40, 8)
                        createChip(20, 45, 9)
                        createChip(45, 30, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1, mask = 0x0C)
                        createLink(1, 2, 2, mask = 0x0C)
                        createLink(2, 3, 3, mask = 0x0C)
                        createLink(3, 4, 4, mask = 0x03)
                        createLink(4, 5, 5, mask = 0x0C)
                        createLink(0, 6, 7, mask = 0x03)
                        createLink(6, 3, 8, mask = 0x03)
                        createLink(3, 7, 9, mask = 0x0C)
                        createLink(7, 8, 10, mask = 0x0C)
                        createLink(8, 9, 11, mask = 0x0C)
                        createLink(9, 5, 12, mask = 0x0C)
                        createLink(5, 999, 6, mask = 0x03)

                        createTrack(listOf(1, 2, 3, 4, 5, 6), 0)
                        createTrack(listOf(7, 8, 9, 10, 11, 12, 6), 1)

                        createWave(16, 2, .09f, 2f)
                        createWave(12, 3, .075f, 1f)
                        createWave(12, 5, .075f, 1f)
                        createWave(10, 12, .075f, 1f)
                        createWave(10, 16, .075f, 1f)
                        createWave(10, 20, .075f, 1f)
                        createWave(10, 30, .075f, 1f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SELL,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    13 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 45, 0, type = Chip.ChipType.ENTRY)
                        createChip(40, 5, 1, type = Chip.ChipType.ENTRY)
                        createChip(10, 35, 2)
                        createChip(10, 25, 3)
                        createChip(10, 15, 4)
                        createChip(25, 15, 5)
                        createChip(40, 15, 6)
                        createChip(40, 25, 7)
                        createChip(40, 35, 8)
                        createChip(25, 35, 9)
                        createChip(25, 25, type = Chip.ChipType.CPU)

                        createLink(0, 2, 1)
                        createLink(2, 3, 2)
                        createLink(3, 4, 3)
                        createLink(4, 5, 4)
                        createLink(5, 6, 5)
                        createLink(6, 7, 6)
                        createLink(7, 999, 7)
                        createLink(1, 6, 8)
                        createLink(7, 8, 9)
                        createLink(8, 9, 10)
                        createLink(9, 2, 11)
                        createLink(3, 999, 12)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 0)
                        createTrack(listOf(8, 6, 9, 10, 11, 2, 12), 1)

                        createWave(12, 3, .07f, 1.1f)
                        createWave(12, 4, .07f, 1.2f)
                        createWave(12, 6, .06f, 1.2f)
                        createWave(16, 10, .06f, 1.2f)
                        createWave(12, 12, .05f, 1.2f)
                        createWave(16, 15, .05f, 1.2f)
                        createWaveHex(12, 20, .05f, 1.2f)
                        createWaveHex(12, 32, .05f, 1.2f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SELL,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    14 -> {
                        initializeNetwork(50, 55)

                        createChip(8, 3, ident = 0, type = Chip.ChipType.ENTRY)
                        createChip(15, 22, ident = 1, type = Chip.ChipType.ENTRY)
                        createChip(20, 6, 2)
                        createChip(32, 10, 3)
                        createChip(46, 20, 4)
                        createChip(22, 30, 5)
                        createChip(8, 30, 6)
                        createChip(3, 42, 7)
                        createChip(21, 45, 8)
                        createChip(32, 40, 9).setType(Chip.ChipType.MEM)
                        createChip(43, 35, type = Chip.ChipType.CPU)

                        createLink(0, 2, 1)
                        createLink(2, 3, 2)
                        createLink(3, 4, 3)
                        createLink(4, 5, 4)
                        createLink(5, 6, 5)
                        createLink(6, 7, 6)
                        createLink(7, 8, 7)
                        createLink(8, 9, 8)
                        createLink(9, 999, 9)
                        createLink(1, 6, 10)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), 0)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9), 3)
                        createTrack(listOf(10, 6, 7, 8, 9), 4)

                        createWave(12, 3, .05f, 1.0f)
                        createWave(12, 5, .06f, 1.1f)
                        createWave(12, 7, .07f, 1.1f)
                        createWave(10, 12, .08f, 1.1f)
                        createWave(10, 20, .08f, 1.2f)
                        createWave(10, 40, .09f, 1.2f)
                        createWaveHex(15, 50, .09f, 1f, coins = 0)
                        createWaveHex(20, 80, .10f, 1f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    15 -> {
                        initializeNetwork(50, 55)

                        createChip(5, 3, type = Chip.ChipType.ENTRY)
                        createChip(10, 12, 1)
                        createChip(20, 15, 2)
                        createChip(20, 40, 3)
                        createChip(30, 45, 4)
                        createChip(40, 42, 5)
                        createChip(45, 30, 6)
                        createChip(45, 20, 61)
                        createChip(5, 30, 7)
                        createChip(17, 30, 8, type = Chip.ChipType.ENTRY)
                        createChip(23, 30, 9, type = Chip.ChipType.ENTRY)
                        createChip(32, 30, 10)
                        createChip(40, 10, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(4, 5, 5)
                        createLink(5, 6, 6)
                        createLink(6, 61, 61)
                        createLink(61, 999, 7)
                        createLink(1, 7, 8)
                        createLink(7, 8, 9)
                        createLink(9, 10, 10)
                        createLink(10, 6, 11)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 61, 7), 0)
                        createTrack(listOf(1, 8, 9, 10, 11, 61, 7), 1)

                        createWave(12, 3, .05f, 1.0f)
                        createWave(12, 4, .06f, 1.1f)
                        createWave(12, 6, .07f, 1.1f)
                        createWave(12, 10, .08f, 1.1f)
                        createWave(12, 16, .08f, 1.1f)
                        createWave(10, 28, .08f, 1.2f)
                        createWave(10, 12, .05f, 2.0f)
                        createWave(10, 50, .09f, 1.2f)
                        createWave(15, 70, .09f, 1f, coins = 0)
                        createWave(15, 80, .09f, 1f, coins = 0)
                        createWaveHex(20, 100, .10f, 1f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    16 -> {
                        initializeNetwork(50, 55)

                        createChip(40, 7, type = Chip.ChipType.ENTRY)
                        createChip(20, 5, 1)
                        createChip(20, 10, 2)
                        createChip(10, 20, 3)
                        createChip(30, 20, 4)
                        createChip(20, 30, 5)
                        createChip(40, 30, 6)
                        createChip(10, 40, 7)
                        createChip(30, 40, 8)
                        createChip(20, 50, 9)
                        createChip(40, 48, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3, mask = 0x0C)
                        createLink(2, 4, 4, mask = 0x03)
                        createLink(3, 5, 5, mask = 0x0C)
                        createLink(4, 5, 6, mask = 0x02)
                        createLink(4, 6, 7, mask = 0x01)
                        createLink(5, 7, 8, mask = 0x0C)
                        createLink(5, 8, 9, mask = 0x02)
                        createLink(6, 8, 10, mask = 0x01)
                        createLink(7, 9, 12, mask = 0x0C)
                        createLink(8, 9, 13, mask = 0x06)
                        createLink(9, 999, 14)

                        createTrack(listOf(1, 2, 3, 5, 8, 12, 14), 0)
                        createTrack(listOf(1, 2, 4, 6, 8, 12, 14), 1)
                        createTrack(listOf(1, 2, 4, 6, 9, 13, 14), 2)
                        createTrack(listOf(1, 2, 4, 7, 10, 13, 14), 3)

                        createWave(12, 3, .05f, 1.0f)
                        createWave(12, 5, .06f, 1.1f)
                        createWave(12, 7, .07f, 1.1f)
                        createWave(10, 12, .08f, 1.1f)
                        createWave(10, 20, .08f, 1.2f)
                        createWave(10, 40, .09f, 1.2f)
                        createWaveHex(15, 50, .09f, 1f, coins = 0)
                        createWaveHex(20, 80, .10f, 1f, coins = 1)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    17 -> {
                        initializeNetwork(45, 55)

                        createChip(40, 2, type = Chip.ChipType.ENTRY)
                        createChip(20, 5, 1)
                        createChip(3, 11, 2)
                        createChip(18, 20, 3)
                        createChip(35, 20, 4)
                        createChip(35, 30, 5)
                        createChip(38, 46, 6)
                        createChip(8, 30, 7)
                        createChip(8, 50, 8)
                        createChip(25, 42, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2, mask = 0x0C)
                        createLink(2, 3, 3, mask = 0x0C)
                        createLink(3, 4, 4, mask = 0x0c)
                        createLink(4, 5, 5, mask = 0x03)
                        createLink(5, 6, 6, mask = 0x03)
                        createLink(6, 999, 7, mask = 0x0C)
                        createLink(4, 7, 8, mask = 0x0C)
                        createLink(3, 7, 9, mask = 0x0C)
                        createLink(7, 999, 10, mask = 0x0C)
                        createLink(7, 8, 11, mask = 0x0E)
                        createLink(8, 999, 12, mask = 0x06)

                        createTrack(listOf(1, 2, 3, 4, 8, 10), 0)
                        createTrack(listOf(1, 2, 3, 9, 11, 12), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 2)
                        createTrack(listOf(1, 2, 3, 9, 8, 5, 6, 7), 3)
                        createTrack(listOf(1, 2, 3, 4, 8, 11, 12), 4)

                        createWave(12, 3, .05f, 1.0f)
                        createWave(16, 5, .06f, 1.1f)
                        createWave(16, 7, .07f, 1.1f)
                        createWave(12, 12, .08f, 1.1f)
                        createWave(12, 20, .08f, 1.2f)
                        createWave(12, 40, .09f, 1.2f)
                        createWaveHex(16, 50, .09f, 1f, coins = 0)
                        createWaveHex(24, 80, .10f, 1f, coins = 1)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    18 -> {
                        initializeNetwork(50, 50)

                        createChip(30, 45, type = Chip.ChipType.ENTRY)
                        createChip(15, 45, 1)
                        createChip(5, 30, 2)
                        createChip(15, 10, 3)
                        createChip(25, 10, 4)
                        createChip(30, 20, 5)
                        createChip(30, 25, 6)
                        createChip(20, 32, ident = 901, type = Chip.ChipType.CPU)
                        createChip(40, 10, 7)
                        createChip(45, 20, 8)
                        createChip(45, 25, 9)
                        createChip(35, 32, ident = 902, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(4, 5, 5, mask = 0x03)
                        createLink(5, 6, 6, mask = 0x03)
                        createLink(6, 901, 901, mask = 0x0C)
                        createLink(4, 7, 7, mask = 0x03)
                        createLink(7, 8, 8, mask = 0x03)
                        createLink(8, 9, 9, mask = 0x03)
                        createLink(9, 902, 902, mask = 0x0C)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 901), 0)
                        createTrack(listOf(1, 2, 3, 4, 7, 8, 9, 902), 1)

                        createWave(16, 5, .06f, 1.1f)
                        createWave(16, 7, .07f, 1.1f)
                        createWave(12, 12, .08f, 1.2f)
                        createWave(12, 20, .08f, 1.2f)
                        createWave(12, 40, .09f, 1.4f)
                        createWaveHex(16, 50, .10f, 1.4f, coins = 0)
                        createWaveHex(24, 80, .11f, 1.4f, coins = 1)
                        createWaveHex(24, 127, .12f, 1.4f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    19 -> {
                        initializeNetwork(65, 70)

                        createChip(20, 0, ident = 10, type = Chip.ChipType.ENTRY)
                        createChip(20, 10, 11)
                        createChip(20, 20, 12)
                        createChip(30, 30, 3)
                        createChip(40, 40, 14)
                        createChip(30, 50, 5)
                        createChip(20, 60, 16)
                        createChip(10, 50, 17)
                        createChip(10, 30, 18)
                        createChip(10, 10, 919, Chip.ChipType.CPU)

                        createChip(40, 0, ident = 20, type = Chip.ChipType.ENTRY)
                        createChip(40, 10, 21)
                        createChip(40, 20, 22)
                        createChip(20, 40, 24)
                        createChip(40, 60, 26)
                        createChip(50, 50, 27)
                        createChip(50, 30, 28)
                        createChip(50, 10, 929, Chip.ChipType.CPU)

                        createLink(10, 11, 11, 0x0c)
                        createLink(11, 12, 12, 0x0c)
                        createLink(12, 3, 13, 0x0c)
                        createLink(3, 14, 14, 0x03)
                        createLink(14, 5, 15, 0x03)
                        createLink(5, 16, 16, 0x0c)
                        createLink(16, 17, 17, 0x0c)
                        createLink(17, 18, 18, 0x0c)
                        createLink(18, 919, 19, 0x0c)

                        createLink(20, 21, 21, 0x03)
                        createLink(21, 22, 22, 0x03)
                        createLink(22, 3, 23, 0x03)
                        createLink(3, 24, 24, 0x0c)
                        createLink(24, 5, 25, 0x0c)
                        createLink(5, 26, 26, 0x03)
                        createLink(26, 27, 27, 0x03)
                        createLink(27, 28, 28, 0x03)
                        createLink(28, 929, 29, 0x03)

                        createTrack(listOf(11, 12, 13, 14, 15, 16, 17, 18, 19), 0)
                        createTrack(listOf(21, 22, 23, 24, 25, 26, 27, 28, 29), 1)

                        createWave(16, 5, .06f, 1.1f)
                        createWave(16, 7, .07f, 1.1f)
                        createWave(12, 12, .08f, 1.2f)
                        createWave(12, 20, .08f, 1.2f)
                        createWave(12, 40, .09f, 1.4f)
                        createWaveHex(16, 50, .10f, 1.4f, coins = 0)
                        createWaveHex(24, 80, .11f, 1.4f, coins = 1)
                        createWaveHex(24, 127, .12f, 1.4f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM
                        )
                        rewardCoins = 3
                    }

                    20 -> {
                        initializeNetwork(50, 50)

                        createChip(48, 12, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(8, 2, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(2, 40, ident = 103, type = Chip.ChipType.ENTRY)
                        createChip(32, 12, 1)
                        createChip(8, 20, 2)
                        createChip(22, 40, 3)
                        createChip(40, 42, 4)
                        createChip(45, 32, 5)
                        createChip(35, 26, 6)
                        createChip(45, 20, 999, Chip.ChipType.CPU)

                        createLink(101, 1, 1, 0x07)
                        createLink(1, 2, 2, 0x07)
                        createLink(2, 3, 3, 0x07)
                        createLink(3, 4, 4, 0x0e)
                        createLink(4, 5, 5, 0x03)
                        createLink(5, 6, 6, 0x0c)
                        createLink(6, 999, 7, 0x0c)
                        createLink(102, 2, 102, 0x0c)
                        createLink(103, 3, 103, 0x03)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 0)
                        createTrack(listOf(102, 3, 4, 5, 6, 7), 1)
                        createTrack(listOf(103, 4, 5, 6, 7), 2)

                        createWave(16, 3, .08f, 1.1f)
                        createWave(16, 5, .09f, 1.2f)
                        createWave(12, 7, .10f, 1.3f)
                        createWave(12, 9, .11f, 1.3f)
                        createWave(12, 20, .12f, 1.3f)
                        createWaveHex(16, 40, .12f, 1.4f, coins = 0)
                        createWaveHex(24, 60, .12f, 1.4f, coins = 1)
                        createWaveHex(24, 80, .12f, 1.4f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC
                        )
                        rewardCoins = 3
                    }

                    21 -> {
                        initializeNetwork(70, 60)

                        createChip(5, 10, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(5, 20, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(5, 30, ident = 103, type = Chip.ChipType.ENTRY)
                        createChip(20, 10, 1)
                        createChip(40, 10, 2)
                        createChip(60, 10, 3)
                        createChip(70, 15, 4)
                        createChip(10, 20, 5)
                        createChip(30, 20, 6)
                        createChip(50, 20, 7)
                        createChip(20, 30, 8)
                        createChip(40, 30, 9)
                        createChip(60, 30, 10)
                        createChip(70, 30, 11)
                        createChip(30, 40, 12)
                        createChip(50, 40, 13)
                        createChip(10, 45, 14)
                        createChip(20, 50, 15)
                        createChip(40, 50, 16)
                        createChip(60, 50, ident = 999, type = Chip.ChipType.CPU)

                        createLink(101, 1, 1, 0x0f)
                        createLink(1, 2, 2, 0x0f)
                        createLink(2, 3, 3, 0x0c)
                        createLink(3, 4, 4, 0x03)
                        createLink(102, 5, 5, 0x07)
                        createLink(5, 6, 6, 0x07)
                        createLink(6, 7, 7, 0x07)
                        createLink(2, 7, 8, 0x03)
                        createLink(7, 11, 9, 0x0e)
                        createLink(4, 11, 10, 0x07)
                        createLink(103, 8, 11, 0x0f)
                        createLink(8, 9, 12, 0x0f)
                        createLink(9, 10, 13, 0x06)
                        createLink(10, 11, 14, 0x06)
                        createLink(9, 13, 15, 0x06)
                        createLink(12, 14, 16, 0x0c)
                        createLink(13, 12, 17, 0x03)
                        createLink(11, 13, 18, 0x03)
                        createLink(14, 15, 19, 0x0c)
                        createLink(15, 16, 20, 0x0e)
                        createLink(16, 999, 21, 0x0e)

                        createTrack(listOf(1, 2, 3, 4, 10, 18, 17, 16, 19, 20, 21), 0)
                        createTrack(listOf(1, 2, 8, 9, 18, 17, 16, 19, 20, 21), 1)
                        createTrack(listOf(5, 6, 7, 9, 18, 17, 16, 19, 20, 21), 2)
                        createTrack(listOf(5, 6, 7, 9, 18, 17, 16, 19, 20, 21), 3)
                        createTrack(listOf(11, 12, 15, 17, 16, 19, 20, 21), 4)
                        createTrack(listOf(11, 12, 13, 14, 18, 17, 16, 19, 20, 21), 5)

                        createWave(16, 3, .08f, 1.1f)
                        createWave(16, 5, .09f, 1.2f)
                        createWave(12, 7, .10f, 1.3f)
                        createWave(12, 9, .11f, 1.3f)
                        createWave(12, 20, .12f, 1.3f)
                        createWaveHex(16, 40, .12f, 1.4f, coins = 0)
                        createWaveHex(24, 60, .12f, 1.4f, coins = 0)
                        createWaveHex(24, 80, .12f, 1.4f, coins = 1)
                        createWaveHex(24, 100, .12f, 1.4f, coins = 0)
                        createWaveHex(24, 120, .12f, 1.4f, coins = 1)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC
                        )
                        rewardCoins = 3
                    }

                    22 -> {
                        initializeNetwork(55, 50)

                        createChip(1, 25, type = Chip.ChipType.ENTRY)
                        createChip(27, 15, 1)
                        createChip(42, 15, 2)
                        createChip(27, 25, 3)
                        createChip(42, 25, 4)
                        createChip(27, 35, 5)
                        createChip(42, 35, 6)
                        createChip(50, 25, type = Chip.ChipType.CPU)


                        createLink(0, 3, 1)
                        createLink(3, 1, 2)
                        createLink(1, 2, 3)
                        createLink(2, 4, 4)
                        createLink(3, 4, 5)
                        createLink(3, 5, 6)
                        createLink(5, 6, 7)
                        createLink(6, 4, 8)
                        createLink(4, 999, 9)

                        createTrack(listOf(1, 5, 4, 3, 2, 5, 9), 0)
                        createTrack(listOf(1, 5, 8, 7, 6, 5, 9), 1)
                        createTrack(listOf(1, 5, 4, 3, 2, 5, 9), 2)
                        createTrack(listOf(1, 5, 8, 7, 6, 5, 9), 3)
                        createTrack(listOf(1, 5, 9), 4)

                        createWave(10, 2, .120f, 1.2f)
                        createWave(16, 2, .150f, 1.4f)
                        createWave(16, 2, .160f, 1.5f)
                        createWave(20, 2, .180f, 1.7f)
                        createWave(25, 2, .220f, 1.9f)
                        createWave(20, 2, .300f, 2.1f)
                        createWave(20, 2, .340f, 2.2f)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    23 -> {
                        initializeNetwork(50, 55)

                        createChip(10, 5, ident = 100, type = Chip.ChipType.ENTRY)
                        createChip(30, 5, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(40, 5, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(35, 50, ident = 103, type = Chip.ChipType.ENTRY)
                        createChip(10, 10, 1)
                        createChip(25, 10, 2)
                        createChip(40, 10, 3).setType(Chip.ChipType.SHL)
                        createChip(10, 20, 4)
                        createChip(30, 20, 5)
                        createChip(40, 20, 6)
                        createChip(12, 30, 7)
                        createChip(35, 30, 8)
                        createChip(12, 40, 9)
                        createChip(35, 40, 10).setType(Chip.ChipType.ADD)
                        createChip(20, 20, 11)
                        createChip(12, 50, ident = 900, type = Chip.ChipType.CPU)

                        createLink(100, 1, 1)
                        createLink(1, 4, 2)
                        createLink(4, 7, 3, 0x0c)
                        createLink(7, 9, 4)
                        createLink(9, 900, 5)
                        createLink(101, 2, 6, 0x06)
                        createLink(2, 5, 7, 0x06)
                        createLink(11, 7, 8, 0x07)
                        createLink(102, 3, 9)
                        createLink(3, 6, 10)
                        createLink(6, 8, 11, 0x03)
                        createLink(8, 5, 12, 0x0c)
                        createLink(103, 10, 13, 0x06)
                        createLink(10, 8, 14, 0x06)
                        createLink(11, 5, 15, 0x0e)

                        createTrack(listOf(1, 2, 3, 4, 5), 0)
                        createTrack(listOf(6, 7, 15, 8, 4, 5), 1)
                        createTrack(listOf(9, 10, 11, 12, 15, 8, 4, 5), 2)
                        createTrack(listOf(13, 14, 12, 15, 8, 4, 5), 3)

                        createWave(16, 3, .08f, 1.1f)
                        createWave(16, 5, .08f, 1.2f)
                        createWave(16, 7, .09f, 1.3f)
                        createWave(16, 11, .09f, 1.3f)
                        createWave(12, 24, .10f, 1.3f)
                        createWave(16, 48, .11f, 1.4f, coins = 0)
                        createWave(24, 64, .12f, 1.4f, coins = 0)
                        createWave(24, 92, .12f, 1.4f, coins = 1)
                        createWave(24, 128, .12f, 1.4f, coins = 0)
                        createWave(24, 160, .12f, 1.4f, coins = 0)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    24 -> {
                        initializeNetwork(50, 55)

                        createChip(25, 0, ident = 100, type = Chip.ChipType.ENTRY)
                        createChip(25, 12, 1)
                        createChip(25, 20, 2)
                        createChip(25, 28, 3).setType(Chip.ChipType.NOP)
                        createChip(25, 36, 4)
                        createChip(25, 44, 5)
                        createChip(25, 52, ident = 900, type = Chip.ChipType.CPU)
                        createChip(12, 22, 6)
                        createChip(38, 30, 7)

                        createLink(100, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(4, 5, 5)
                        createLink(5, 900, 6)
                        createLink(1, 6, 7, 0x0c)
                        createLink(6, 4, 8, 0x0c)
                        createLink(2, 7, 9, 0x03)
                        createLink(7, 5, 10, 0x03)

                        createTrack(listOf(1, 7, 8, 5, 6), 0)
                        createTrack(listOf(1, 2, 9, 10, 6), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 3)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 4)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 5)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 6)
                        createTrack(listOf(1, 2, 3, 4, 5, 6), 7)

                        createWave(10, 1, .11f, 1.2f)
                        createWave(10, 3, .11f, 1.2f)
                        createWave(10, 6, .11f, 1.4f)
                        createWave(10, 8, .12f, 1.5f)
                        createWave(10, 16, .12f, 1.5f)
                        createWave(10, 30, .14f, 1.5f)
                        createWave(10, 40, .16f, 1.5f)
                        createWave(10, 50, .16f, 1.8f, coins = 1)
                        createWave(10, 80, .16f, 2.0f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    25 -> {
                        initializeNetwork(50, 50)

                        createChip(5, 5, ident = 100, type = Chip.ChipType.ENTRY)
                        createChip(25, 5 + Random.nextInt(5), 1)
                        createChip(40 + Random.nextInt(5), 5 + Random.nextInt(5), 2)
                        createChip(40 + Random.nextInt(5), 25, 3)
                        createChip(25, 25, 4)
                        createChip(5 + Random.nextInt(5), 25, 5)
                        createChip(5 + Random.nextInt(5), 40 + Random.nextInt(5), 6)
                        createChip(25, 40 + Random.nextInt(5), 7)
                        createChip(
                                40 + Random.nextInt(5),
                                40 + Random.nextInt(5),
                                ident = 900,
                                type = Chip.ChipType.CPU
                        )

                        createLink(100, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(4, 5, 5)
                        createLink(5, 6, 6)
                        createLink(6, 7, 7)
                        createLink(7, 900, 8)
                        createLink(1, 4, 9, 0x0A)
                        createLink(4, 7, 10, 0x05)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8), 0)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8), 1)
                        createTrack(listOf(1, 9, 5, 6, 7, 8), 2)
                        createTrack(listOf(1, 2, 3, 4, 10, 8), 3)
                        createTrack(listOf(1, 9, 10, 8), 4)

                        createWave(16, 3, .10f, 1.0f)
                        createWave(16, 5, .10f, 1.0f)
                        createWave(16, 8, .10f, 1.1f)
                        createWave(16, 12, .12f, 1.1f)
                        createWave(16, 16, .12f, 1.1f)
                        createWave(16, 30, .14f, 1.1f)
                        createWave(16, 40, .16f, 1.1f, coins = 1)
                        createWave(16, 50, .16f, 1.2f)
                        createWave(16, 80, .16f, 1.2f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    26 -> {
                        initializeNetwork(50, 50)

                        createChip(5, 35, ident = 100, type = Chip.ChipType.ENTRY)
                        createChip(5, 45, 1)
                        createChip(25, 40 + Random.nextInt(5), 2)
                        createChip(30 + Random.nextInt(5), 32, 3)
                        createChip(20, 20, 4)
                        createChip(20, 12 + Random.nextInt(5), 5)
                        createChip(10, 12 + Random.nextInt(5), 6)
                        createChip(5, 5, 7)
                        createChip(20, 5, 8)
                        createChip(31, 12 + Random.nextInt(2), 9)
                        createChip(42, 5, 10)
                        createChip(45, 20, 11)
                        createChip(40, 32, 12)
                        createChip(
                                40 + Random.nextInt(5),
                                40 + Random.nextInt(5),
                                ident = 900,
                                type = Chip.ChipType.CPU
                        )

                        createLink(100, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3, variant = Link.Variant.CONCAVE)
                        createLink(3, 4, 4, variant = Link.Variant.CONCAVE)
                        createLink(4, 5, 5)
                        createLink(5, 6, 6)
                        createLink(6, 7, 7)
                        createLink(7, 8, 8)
                        createLink(8, 9, 9, variant = Link.Variant.CONCAVE)
                        createLink(9, 10, 10, variant = Link.Variant.CONCAVE)
                        createLink(10, 11, 11, variant = Link.Variant.CONCAVE)
                        createLink(11, 12, 12, variant = Link.Variant.CONCAVE)
                        createLink(12, 900, 13)
                        createLink(3, 12, 14, 0x02)

                        createTrack(listOf(1, 2, 3, 14, 13), 0)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), 3)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), 4)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), 5)

                        createWaveHex(16, 16, .12f, 1.1f)
                        createWaveHex(16, 64, .16f, 1.1f, coins = 1)
                        createWaveHex(16, 128, .16f, 1.2f)
                        createWaveHex(16, 256, .16f, 1.2f)
                        createWaveHex(16, 512, .16f, 1.2f)
                        createWaveHex(16, 1024, .16f, 1.2f)
                        createWaveHex(16, 2048, .16f, 1.6f)
                        createWaveHex(16, 4096, .16f, 1.6f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    27 -> {
                        initializeNetwork(50, 60)
                        createChip(0, 30, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(25, 5, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(50, 30, ident = 103, type = Chip.ChipType.ENTRY)
                        createChip(25, 55, ident = 104, type = Chip.ChipType.ENTRY)
                        createChip(18, 30, 1)
                        createChip(25, 10, 2)
                        createChip(25, 20, 3)
                        createChip(40, 30, 4)
                        createChip(25, 40, 5)
                        createChip(25, 50, 6).setType(Chip.ChipType.CLK)
                        createChip(25, 30, ident = 900, type = Chip.ChipType.CPU)

                        createLink(101, 1, 1)
                        createLink(102, 2, 2)
                        createLink(2, 3, 3)
                        createLink(103, 4, 4)
                        createLink(104, 6, 5)
                        createLink(6, 5, 6)
                        createLink(1, 3, 7, mask = 0x0C)
                        createLink(3, 4, 8, mask = 0x03)
                        createLink(1, 5, 9, mask = 0x0C)
                        createLink(4, 5, 10, mask = 0x03)
                        createLink(3, 900, 11, mask = 0x04)
                        createLink(5, 900, 12, mask = 0x04)
                        createLink(1, 900, 13, mask = 0x04)
                        createLink(4, 900, 14, mask = 0x04)

                        createTrack(listOf(1, 7, 8, 10, 9, 13), 0)
                        createTrack(listOf(2, 3, 7, 9, 10, 8, 11), 1)
                        createTrack(listOf(4, 10, 9, 7, 8, 14), 2)
                        createTrack(listOf(5, 6, 10, 8, 7, 9, 12), 3)

                        createWaveHex(16, 8, .09f, 1.0f)
                        createWaveHex(16, 16, .10f, 1.0f)
                        createWaveHex(16, 32, .11f, 1.2f)
                        createWaveHex(16, 32, .10f, 1.0f)
                        createWaveHex(16, 32, .10f, 1.4f)
                        createWaveHex(16, 64, .11f, 1.2f)
                        createWaveHex(16, 128, .11f, 1.2f)
                        createWaveHex(16, 256, .10f, 1.2f, coins = 1)

                        rewardCoins = 3
                    }

                    28 -> {
                        initializeNetwork(50, 60)
                        createChip(10, 5, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(10, 12, 4)
                        createChip(10, 25, 3)
                        createChip(10, 35, 2)
                        createChip(10, 45, 1)
                        createChip(10, 55, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(20, 45, 5)
                        createChip(20, 34, 6)
                        createChip(20, 25, 7)
                        createChip(20, 15, 8)
                        createChip(30, 15, 11)
                        createChip(30, 32, 10)
                        createChip(30, 43, 9)
                        createChip(40, 15, ident = 901, type = Chip.ChipType.CPU)
                        createChip(40, 40, ident = 902, type = Chip.ChipType.CPU)

                        createLink(102, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(101, 4, 5)
                        createLink(1, 5, 6, mask = 0x06)
                        createLink(2, 6, 7, mask = 0x06)
                        createLink(3, 7, 8, mask = 0x06)
                        createLink(4, 8, 9, mask = 0x03)
                        createLink(5, 6, 10)
                        createLink(6, 7, 11)
                        createLink(7, 8, 12)
                        createLink(5, 9, 14, mask = 0x06)
                        createLink(6, 10, 15, mask = 0x08)
                        createLink(8, 11, 16, mask = 0x06)
                        createLink(9, 10, 17)
                        createLink(10, 11, 18)
                        createLink(9, 902, 19, mask = 0x07)
                        createLink(11, 901, 20, mask = 0x07)

                        createTrack(listOf(5, 4, 3, 7, 11, 12, 16, 18, 17, 19), 0)
                        createTrack(listOf(5, 4, 3, 2, 6, 10, 15, 18, 20), 1)
                        createTrack(listOf(1, 2, 3, 8, 11, 10, 14, 17, 18, 20), 2)
                        createTrack(listOf(1, 2, 3, 4, 9, 12, 11, 15, 17, 19), 3)

                        createWaveHex(16, 16, .12f, 1.0f)
                        createWaveHex(16, 32, .11f, 1.2f)
                        createWaveHex(16, 32, .12f, 1.0f)
                        createWaveHex(16, 32, .10f, 1.4f)
                        createWaveHex(16, 64, .12f, 1.2f)
                        createWaveHex(16, 128, .14f, 1.2f)
                        createWaveHex(16, 256, .12f, 1.4f)
                        createWaveHex(16, 512, .14f, 1.2f)
                        createWaveHex(16, 1024, .12f, 1.3f, coins = 1)
                        createWaveHex(16, 4096, .14f, 1.2f)

                        rewardCoins = 3
                    }

                    29 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 45, ident = 100, type = Chip.ChipType.ENTRY)
                        createChip(11, 40, 1)
                        createChip(8, 22, 2)
                        createChip(11, 10, 3)
                        createChip(20, 10, ident = 900, type = Chip.ChipType.CPU)

                        createChip(30, 45, ident = 101, type = Chip.ChipType.ENTRY)
                        createChip(30, 40, 4)
                        createChip(21, 40, 5)
                        createChip(18, 22, 6)
                        createChip(30, 25, ident = 901, type = Chip.ChipType.CPU)

                        createChip(30, 5, ident = 102, type = Chip.ChipType.ENTRY)
                        createChip(30, 10, 7)
                        createChip(40, 10, 8)
                        createChip(43, 22, 9)
                        createChip(40, 40, ident = 902, type = Chip.ChipType.CPU)

                        createLink(100, 1, 1)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 900, 4)
                        createLink(101, 4, 5)
                        createLink(4, 5, 6)
                        createLink(5, 6, 7)
                        createLink(6, 901, 8)
                        createLink(102, 7, 9)
                        createLink(7, 8, 10)
                        createLink(8, 9, 11)
                        createLink(9, 902, 12)

                        createTrack(listOf(1, 2, 3, 4), 0)
                        createTrack(listOf(5, 6, 7, 8), 1)
                        createTrack(listOf(9, 10, 11, 12), 2)

                        createWave(12, 2, .10f, 1.0f)
                        createWave(12, 3, .09f, 1.0f)
                        createWave(12, 4, .08f, 1.0f)
                        createWave(12, 5, .08f, 1.0f)
                        createWave(12, 6, .09f, 0.9f)
                        createWave(12, 8, .09f, 0.9f)
                        createWave(12, 12, .09f, 0.9f)
                        createWave(12, 16, .10f, 0.9f)
                        createWave(12, 24, .11f, 0.8f)
                        createWave(12, 28, .12f, 0.8f)
                        createWave(12, 32, .14f, 0.9f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.CLK,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    30 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 10, 1, type = Chip.ChipType.ENTRY)
                        createChip(10, 20, 2)
                        createChip(10, 30, 3)
                        createChip(10, 40, 4)
                        createChip(10, 50, 900, type = Chip.ChipType.CPU)
                        createChip(25, 10, 5)
                        createChip(25, 20, 6).setType(Chip.ChipType.DUP)
                        createChip(25, 30, 7)
                        createChip(25, 40, 8)
                        createChip(25, 50, 901, type = Chip.ChipType.CPU)
                        createChip(40, 10, 9)
                        createChip(40, 20, 10).setType(Chip.ChipType.SPLT)
                        createChip(40, 30, 11)
                        createChip(40, 40, 12)
                        createChip(40, 50, 902, type = Chip.ChipType.CPU)

                        createLink(1, 2, 1, mask = 0x09)
                        createLink(2, 3, 2)
                        createLink(3, 4, 3)
                        createLink(4, 900, 4)
                        createLink(1, 5, 5, mask = 0x09)
                        createLink(5, 6, 6)
                        createLink(6, 7, 7)
                        createLink(7, 8, 8)
                        createLink(8, 901, 9)
                        createLink(5, 9, 10)
                        createLink(9, 10, 11)
                        createLink(10, 11, 12)
                        createLink(11, 12, 13)
                        createLink(12, 902, 14)
                        createLink(3, 7, 15, mask = 0x09)
                        createLink(7, 11, 16, mask = 0x09)
                        createLink(4, 8, 17, mask = 0x09)
                        createLink(8, 12, 18, mask = 0x09)

                        createTrack(listOf(1, 2, 3, 4), 0)
                        createTrack(listOf(5, 6, 7, 8, 9), 1)
                        createTrack(listOf(5, 10, 11, 12, 13, 14), 2)

                        createWave(16, 4, .12f, 1.0f)
                        createWave(16, 8, .11f, 1.0f)
                        createWaveHex(16, 16, .12f, 1.0f)
                        createWaveHex(16, 32, .10f, 1.0f)
                        createWaveHex(16, 64, .12f, 1.1f)
                        createWaveHex(16, 128, .14f, 1.1f)
                        createWaveHex(16, 256, .12f, 1.2f)
                        createWaveHex(16, 512, .14f, 1.2f)
                        createWaveHex(16, 1024, .12f, 1.2f, coins = 1)
                        createWaveHex(16, 4096, .14f, 1.2f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.CLK,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    31 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 10, 1, type = Chip.ChipType.ENTRY)
                        createChip(10, 20, 2)
                        createChip(10, 30, 3)
                        createChip(10, 40, 4).setType(Chip.ChipType.DUP)
                        createChip(25, 40, 5).setType(Chip.ChipType.SPLT)
                        createChip(40, 30, 6)
                        createChip(30, 20, 7)
                        createChip(25, 10, 900, type = Chip.ChipType.CPU)

                        createLink(1, 2, 1)
                        createLink(2, 3, 2)
                        createLink(3, 4, 3)
                        createLink(4, 5, 4)
                        createLink(5, 6, 5)
                        createLink(6, 7, 6)
                        createLink(7, 900, 7)
                        createLink(2, 7, 8, mask = 0x00)
                        createLink(3, 7, 9, mask = 0x00)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 0)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 3)
                        createTrack(listOf(1, 8, 7), 4)
                        createTrack(listOf(1, 2, 9, 7), 5)

                        createWave(16, 4, .12f, 1.2f)
                        createWave(16, 8, .11f, 1.2f)
                        createWave(16, 9, .12f, 1.3f)
                        createWave(16, 12, .10f, 1.3f)
                        createWave(16, 16, .12f, 1.3f)
                        createWave(16, 32, .14f, 1.3f)
                        createWave(16, 64, .12f, 1.2f)
                        createWave(16, 100, .14f, 1.2f)
                        createWave(16, 128, .12f, 1.2f, coins = 1)
                        createWave(16, 200, .14f, 1.2f)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.CLK,
                                Chip.ChipUpgrades.REDUCE
                        )
                        rewardCoins = 3
                    }

                    32 -> {
                        initializeNetwork(50, 50)

                        createChip(10, 10, 1)
                        createChip(25, 10, 2, type = Chip.ChipType.CPU)
                        createChip(40, 10, 3)
                        createChip(10, 20, 4)
                        createChip(25, 20, 5)
                        createChip(40, 20, 6)
                        createChip(10, 30, 7)
                        createChip(25, 30, 8, type = Chip.ChipType.ENTRY)
                        createChip(40, 30, 9)
                        createChip(10, 40, 10)
                        createChip(25, 40, 11)
                        createChip(40, 40, 12)

                        createLink(1, 2, 1)
                        createLink(2, 3, 2)
                        createLink(1, 4, 3)
                        createLink(3, 6, 4)
                        createLink(4, 5, 5)
                        createLink(5, 6, 6)
                        createLink(4, 7, 7)
                        createLink(8, 5, 8)
                        createLink(6, 9, 9)
                        createLink(8, 7, 10)
                        createLink(8, 9, 11)
                        createLink(7, 10, 12)
                        createLink(8, 11, 13)
                        createLink(9, 12, 14)
                        createLink(10, 11, 15)
                        createLink(11, 12, 16)

                        createTrack(listOf(8, 5, 3, 1), 0)
                        createTrack(listOf(8, 6, 4, 2), 1)
                        createTrack(listOf(10, 7, 3, 1), 2)
                        createTrack(listOf(11, 9, 4, 2), 3)
                        createTrack(listOf(13, 15, 12, 7, 3, 1), 4)
                        createTrack(listOf(13, 16, 14, 9, 4, 2), 5)

                        createWave(16, 4, .12f, 1.2f)
                        createWave(16, 8, .11f, 1.4f)
                        createWave(16, 9, .12f, 1.5f)
                        createWave(16, 12, .10f, 1.6f)
                        createWave(16, 16, .12f, 1.6f)
                        createWave(16, 32, .14f, 1.8f)
                        createWave(16, 64, .12f, 2.0f)
                        createWave(16, 128, .14f, 2.4f)
                        createWave(16, 200, .12f, 2.6f)
                        createWave(16, 240, .14f, 2.6f, coins = 1)

                        data.chipsAllowed = setOf(
                                Chip.ChipUpgrades.SUB,
                                Chip.ChipUpgrades.POWERUP,
                                Chip.ChipUpgrades.SELL,
                                Chip.ChipUpgrades.SHR,
                                Chip.ChipUpgrades.MEM,
                                Chip.ChipUpgrades.ACC,
                                Chip.ChipUpgrades.CLK,
                                Chip.ChipUpgrades.RES,
                                Chip.ChipUpgrades.REDUCE
                        )
                        data.type = Stage.Type.FINAL
                        rewardCoins = 3
                    }
                    /**
                     * Template for the creation of new stage data.
                     * Copy and uncomment this when adding your own levels.

                    22 ->  // number of the level. Must be in increasing order.
                    {
                    // Define the size of the virtual grid where all elements (nodes) will be placed.
                    // All chip coordinates will refer to this grid.
                    // A standard size is 50x50. Bigger grids are allowed, they will require scrolling.
                    initializeNetwork(70, 60)

                    // Define the chips (nodes) and their positions on the virtual grid.
                    // Each chip must have a unique ident.
                    // The type may be ENTRY, CPU or empty for standard slots.
                    // You should have at least one entry and at least one CPU.
                    // Multiple entries and multiple CPUs are allowed.
                    createChip(5, 10, ident = 101, type = Chip.ChipType.ENTRY)
                    createChip(20, 10, 1)
                    createChip(40, 20, 2)
                    createChip(40, 50, ident = 999, type = Chip.ChipType.CPU)

                    // Define the connections between the chips.
                    // 'from' and 'to' refer to the chip idents defined above.
                    // Each link must have its own unique ident.
                    //
                    // By default, connections have 4 'copper lanes'. The 'mask' parameter can be
                    // used to hide one or more of them to avoid visual crossing of the lanes
                    // in order to prettify the layout and make it more circuit-like.
                    // It has no influence on gameplay.
                    // The mask is applied by bitwise AND: 0x0f shows all lanes, 0x0e suppresses the
                    // right-most, 0x07 the left-most, and so forth.
                    createLink(101, 1, 1 )
                    createLink(1, 2, 2 )
                    createLink(2, 999, 3, 0x0c)

                    // Define the track(s) that are used by the attackers.
                    // A track is just a sequence of connections. The first link should start on
                    // an ENTRY node, the last one should end on a CPU node.
                    // You must ensure that the links  are connected to each other,
                    // i.e. that the track is logically possible
                    // in your grid layout. Otherwise, you will see attackers appearing out of nowhere.
                    // It is not possible to include the same link twice in a track, although you
                    // can pass through the same node more than once using different links.
                    //
                    // An arbitrary number of tracks can be defined, and they must have idents
                    // 0, 1, 2, ... in continuous order. An attacker will chose one of the tracks with
                    // equal probability. If you want a higher probability for one of the tracks, just
                    // copy it multiple times.
                    createTrack(listOf(1, 2, 3), 0)
                    createTrack(listOf(1, 2, 3), 1)

                    // Define the waves of attackers. The waves are executed subsequently in the order
                    // given. Preferably they should increase in strength.
                    //
                    // There will be attackerCount attackers in the wave. They will have a random
                    // strength ranging from 0 to attackerStrength. Higher attackerFrequency and
                    // attackerSpeed will make the wave considerably more dangerous.
                    // Optionally, a number of travelling coins can be defined that will come with the
                    // wave; use this sparsely to avoid coin inflation (not more than 1 per level).
                    //
                    // Attackers are shown in binary as long as their number is not too big, otherwise
                    // in hexadecimal. The alternative method createWaveHex only creates hex attackers,
                    // even for small numbers.
                    createWave(16, 3, .08f, 1.1f)
                    createWaveHex(24, 80, .12f, 1.4f, coins = 1)

                    // You can limit the types of chips that can be bought by the player.
                    // If none is given, all chip types are allowed, even future ones.
                    data.chipsAllowed = setOf(
                    Chip.ChipUpgrades.SUB, Chip.ChipUpgrades.POWERUP, Chip.ChipUpgrades.SELL,
                    Chip.ChipUpgrades.SHR, Chip.ChipUpgrades.MEM, Chip.ChipUpgrades.ACC
                    )

                    // Number of coins that are rewarded for the completion of a level. A good number is 3.
                    rewardCoins = 3

                    // Marker for the last level in the current implementation. Remove this when adding
                    // more levels.
                    // Keep in mind that you must change the value of Game.maxLevelAvailable, too.
                    type = Type.FINAL
                    }
                     */
                }
                data.maxWaves = waves.size
            }
        }

        private fun createSpecialStageWithoutObstacles(
            stage: Stage, level: Stage.Identifier,
            special: GameMechanics.Params.Season
        ) {
            stage.data.ident = level
            stage.waves.clear()
            stage.data.type = Stage.Type.REGULAR
            stage.data.chipsAllowed =
                setOf(
                        Chip.ChipUpgrades.ACC,
                        Chip.ChipUpgrades.SUB,
                        Chip.ChipUpgrades.SHR,
                        Chip.ChipUpgrades.MEM,
                        Chip.ChipUpgrades.CLK,
                        Chip.ChipUpgrades.POWERUP,
                        Chip.ChipUpgrades.REDUCE,
                        Chip.ChipUpgrades.SELL
                )
            with(stage)
            {
                when (special) {
                    GameMechanics.Params.Season.EASTER -> {
                        initializeNetwork(50, 50)

                        createChip(23, 45, 0, type = Chip.ChipType.ENTRY)
                        createChip(12, 40, 1)
                        createChip(5, 30, 2)
                        createChip(5, 20, 3)
                        createChip(12, 10, 4)
                        createChip(25, 5, 5)
                        createChip(38, 10, 6)
                        createChip(45, 20, 7)
                        createChip(45, 30, 8)
                        createChip(38, 40, 9)
                        createChip(28, 45, 10, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1, variant = Link.Variant.CONCAVE)
                        createLink(1, 2, 2)
                        createLink(2, 3, 3)
                        createLink(3, 4, 4)
                        createLink(5, 4, 5, variant = Link.Variant.CONCAVE)
                        createLink(5, 6, 6)
                        createLink(6, 7, 7, variant = Link.Variant.CONCAVE)
                        createLink(7, 8, 8)
                        createLink(8, 9, 9, variant = Link.Variant.CONCAVE)
                        createLink(9, 10, 10)
                        createLink(2, 8, 11, 0x06)
                        createLink(3, 7, 12, 0x06)

                        createTrack(listOf(1, 2, 11, 9, 10), 0)
                        createTrack(listOf(1, 2, 3, 12, 8, 9, 10), 1)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 2)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 3)
                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), 4)

                        createWave(20, 2, .125f, 1.1f)
                        createWave(20, 3, .120f, 1.2f)
                        createWave(20, 4, .105f, 1.4f)
                        createWave(20, 6, .100f, 1.6f)
                        createWave(20, 9, .090f, 1.6f, coins = 1)
                        createWave(20, 27, .080f, 1.8f, coins = 0)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    GameMechanics.Params.Season.CHRISTMAS -> {
                        initializeNetwork(50, 50)

                        createChip(25, 48, 0, type = Chip.ChipType.ENTRY)
                        createChip(25, 38, 1)
                        createChip(5, 38, 2)
                        createChip(45, 38, 12)
                        createChip(25, 26, 3)
                        createChip(10, 26, 4)
                        createChip(40, 26, 14)
                        createChip(25, 15, 5)
                        createChip(15, 15, 6)
                        createChip(35, 15, 16)
                        createChip(25, 5, 999, type = Chip.ChipType.CPU)

                        createLink(0, 1, 1)
                        createLink(1, 2, 2, mask = 0x03)
                        createLink(2, 3, 3, mask = 0x0C, variant = Link.Variant.CONVEX)
                        createLink(3, 4, 4, 0x03)
                        createLink(5, 4, 5, 0x0C)
                        createLink(5, 6, 6, 0x06, variant = Link.Variant.CONVEX)
                        createLink(6, 999, 7, 0x06, variant = Link.Variant.CONVEX)
                        createLink(1, 12, 8, 0x03)
                        createLink(12, 3, 9, mask = 0x03, variant = Link.Variant.CONCAVE)
                        createLink(3, 14, 10, 0x03)
                        createLink(14, 5, 11, mask = 0x03, variant = Link.Variant.CONCAVE)
                        createLink(5, 16, 12, 0x06)
                        createLink(16, 999, 13, 0x06)

                        createTrack(listOf(1, 2, 3, 4, 5, 6, 7), 0)
                        createTrack(listOf(1, 8, 9, 10, 11, 12, 13), 1)

                        createWave(16, 2, .125f, 1.1f)
                        createWave(20, 3, .120f, 1.4f)
                        createWave(20, 5, .105f, 1.8f)
                        createWave(20, 7, .100f, 2.0f)
                        createWave(20, 11, .090f, 2.2f, coins = 1)
                        createWave(20, 27, .080f, 2.4f, coins = 0)

                        data.chipsAllowed =
                            setOf(
                                    Chip.ChipUpgrades.SUB,
                                    Chip.ChipUpgrades.POWERUP,
                                    Chip.ChipUpgrades.SHR
                            )
                        rewardCoins = 3
                    }

                    else -> {
                        createStageWithoutObstacles(stage, level)
                    } // should not happen
                }
                data.maxWaves = waves.size
            }
        }
    }
}
