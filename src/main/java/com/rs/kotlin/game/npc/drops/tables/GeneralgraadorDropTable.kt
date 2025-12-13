package com.rs.kotlin.game.npc.drops.tables

import com.rs.java.game.player.Skills
import com.rs.java.game.player.content.treasuretrails.TreasureTrailsManager
import com.rs.kotlin.game.npc.drops.dropTable

object GeneralgraadorDropTable {

    val table = dropTable(rareDropTable = false, rolls = 1) {

        alwaysDrops {
            drop("item.big_bones")
        }

        mainDrops(128) {
        }

        tertiaryDrops {
            drop("item.bandos_chestplate", numerator = 1, denominator = 381)
            drop("item.bandos_tassets", numerator = 1, denominator = 381)
            drop("item.bandos_boots", numerator = 1, denominator = 381)
            drop("item.bandos_hilt", numerator = 1, denominator = 508)
            drop("item.godsword_shard_1", numerator = 1, denominator = 762)
            drop("item.godsword_shard_2", numerator = 1, denominator = 762)
            drop("item.godsword_shard_3", numerator = 1, denominator = 762)
            drop("item.rune_longsword", numerator = 8, denominator = 127)
            drop("item.rune_2h_sword", numerator = 8, denominator = 127)
            drop("item.rune_platebody", numerator = 8, denominator = 127)
            drop("item.rune_pickaxe", numerator = 6, denominator = 127)
            drop("item.coins", numerator = 7, denominator = 127)
            drop("item.grimy_snapdragon", numerator = 8, denominator = 127)
            drop("item.snapdragon_seed", numerator = 8, denominator = 127)
            drop("item.super_restore_4", numerator = 8, denominator = 127)
            drop("item.adamantite_ore", numerator = 8, denominator = 127)
            drop("item.coal", numerator = 8, denominator = 127)
            drop("item.magic_logs", numerator = 8, denominator = 127)
            drop("item.nature_rune", numerator = 8, denominator = 127)
            drop("item.coins", numerator = 25, denominator = 127)
            drop(
                "item.scroll_box_elite",
                numerator = 1,
                denominator = 250,
                condition = { player -> !player.treasureTrailsManager.hasClueScrollByLevel(TreasureTrailsManager.ELITE) }
            )
            drop("item.long_bone", numerator = 1, denominator = 400)
            drop("item.curved_bone", numerator = 1, denominator = 5012)
        }

    }.apply { name = "general graador" }
}
