package com.rs.kotlin.game.npc.drops.tables

import com.rs.java.game.player.Skills
import com.rs.java.game.player.content.treasuretrails.TreasureTrailsManager
import com.rs.kotlin.game.npc.drops.dropTable

object KingblackdragonDropTable {

    val table = dropTable(rareDropTable = true, rolls = 1) {

        alwaysDrops {
            drop("item.dragon_bones")
            drop("item.black_dragonhide", amount = 2)
        }

        mainDrops(128) {
            drop("item.rune_longsword", weight = 10)
            drop("item.adamant_platebody", weight = 9)
            drop("item.adamant_kiteshield", weight = 3)
            drop("item.dragon_helm", weight = 1)
            drop("item.fire_rune", amount = 300, weight = 5)
            drop("item.air_rune", amount = 300, weight = 10)
            drop("item.iron_arrow", amount = 690, weight = 10)
            drop("item.runite_bolts", amount = 10..20, weight = 10)
            drop("item.law_rune", amount = 30, weight = 5)
            drop("item.blood_rune", amount = 30, weight = 5)
            drop("item.yew_logs", weight = 10)
            drop("item.adamant_bar", amount = 3, weight = 5)
            drop("item.rune_bar", weight = 3)
            drop("item.gold_ore", weight = 2)
            drop("item.amulet_of_power", weight = 7)
            drop("item.dragon_arrowheads", amount = 5..14, weight = 5)
            drop("item.runite_limbs", weight = 4)
            drop("item.shark", amount = 4, weight = 4)
            drop("item.kbd_heads", weight = 1)
        }

        tertiaryDrops {
            drop("item.dragon_pickaxe", numerator = 1, denominator = 1000)
            drop(
                "item.scroll_box_elite",
                numerator = 1,
                denominator = 450,
                condition = { player -> !player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.ELITE) }
            )
            drop("item.draconic_visage", numerator = 1, denominator = 5000)
        }

    }.apply { name = "king black dragon" }
}
