package com.example.cpudefense.upgrades

import com.example.cpudefense.Game

class IncreaseStartingCash(game: Game): Upgrade(game, Upgrade.Type.INCREASE_STARTING_CASH) {
    init {
        setDesc(short = "Starting cash: %d".format(8))
    }
}