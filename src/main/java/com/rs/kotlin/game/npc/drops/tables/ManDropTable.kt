package com.rs.kotlin.game.npc.drops.tables

import com.rs.java.game.player.Skills
import com.rs.java.game.player.content.treasuretrails.TreasureTrailsManager
import com.rs.kotlin.game.npc.drops.dropTable

object ManDropTable {

    val table = dropTable(rareDropTable = false, rolls = 1) {

        alwaysDrops {
            drop("item.bones")
        }

        mainDrops(128) {
            drop("item.bronze_helm", weight = 2)
            drop("item.iron_dagger", weight = 1)
            drop("item.bronze_bolts", weight = 10)
            drop("item.bronze_arrow", amount = 7, weight = 3)
            drop("item.earth_rune", amount = 4, weight = 2)
            drop("item.fire_rune", amount = 6, weight = 2)
            drop("item.mind_rune", amount = 9, weight = 2)
            drop("item.chaos_rune", amount = 2, weight = 1)
            drop("item.coins", amount = 3, weight = 38)
            drop("item.coins", amount = 10, weight = 23)
            drop("item.coins", amount = 5, weight = 9)
            drop("item.coins", amount = 15, weight = 4)
            drop("item.coins", amount = 25, weight = 1)
            drop("item.fishing_bait", weight = 5)
            drop("item.copper_ore", weight = 2)
            drop("item.earth_talisman", weight = 2)
            drop("item.cabbage", weight = 1)
        }

        tertiaryDrops {
            drop(
                "item.scroll_box_easy",
                numerator = 1,
                denominator = 128,
                condition = { player -> !player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.EASY) }
            )
        }

    }.apply { name = "man" }
}
