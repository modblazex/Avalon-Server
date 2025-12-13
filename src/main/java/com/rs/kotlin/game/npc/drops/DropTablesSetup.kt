package com.rs.kotlin.game.npc.drops

import com.rs.kotlin.Rscm
import com.rs.kotlin.game.npc.drops.DropTableRegistry.registerDropTable
import com.rs.kotlin.game.npc.drops.tables.*

object DropTablesSetup {

    lateinit var rareDropTable: RareDropTableEntry
        private set
    lateinit var gemDropTable: GemTableEntry
        private set
    lateinit var superRareTable: SuperRareTableEntry
        private set
    lateinit var herbDropTable: HerbTableEntry
        private set


    @JvmStatic
    fun setup() {
        rareDropTable = RareDropTableEntry()
        gemDropTable = GemTableEntry()
        superRareTable = SuperRareTableEntry()
        herbDropTable = HerbTableEntry()
        registerDropTable(ManDropTable.table, Rscm.lookupList("npc_group.man"), Rscm.lookupList("npc_group.man_lv2"), Rscm.lookupList("npc_group.man_lv4"))
        registerDropTable(BlackDragonDropTable.blackDragonTable, Rscm.lookupList("npc_group.black_dragon_lv227"))
        registerDropTable(KingblackdragonDropTable.table, Rscm.lookup("npc.king_black_dragon_lv276"))
        registerDropTable(GeneralgraadorDropTable.table, Rscm.lookup("npc.general_graardor_lv624"))
        registerDropTable(GoblinDropTable.table, Rscm.lookupList("npc_group.goblin_lv1"), Rscm.lookupList("npc_group.goblin_lv2"))
        registerDropTable(GoblinLvl5DropTable.table,
            Rscm.lookupList("npc_group.goblin_lv5"),
            Rscm.lookupList("npc_group.goblin_lv11"),
            Rscm.lookupList("npc_group.goblin_lv12"),
            Rscm.lookupList("npc_group.goblin_lv13"),
            Rscm.lookupList("npc_group.goblin_lv15"),
            Rscm.lookupList("npc_group.goblin_lv16"),
            Rscm.lookupList("npc_group.goblin_lv17"),
            Rscm.lookupList("npc_group.goblin_lv25"))
        registerDropTable(RockCrabTable.table, Rscm.lookupList("npc_group.rock_crab_lv13"))
        registerDropTable(DagannothRexTable.table, Rscm.lookup("npc.dagannoth_rex_lv303"))
        registerDropTable(DagannothPrimeTable.table, Rscm.lookup("npc.dagannoth_prime_lv303"))
        registerDropTable(DagannothSupremeTable.table, Rscm.lookup("npc.dagannoth_supreme_lv303"))
        registerDropTable(CrawlingHandTable.table,
            Rscm.lookupList("npc_group.crawling_hand"),
            Rscm.lookupList("npc_group.crawling_hand_lv7"),
            Rscm.lookupList("npc_group.crawling_hand_lv8"),
            Rscm.lookupList("npc_group.crawling_hand_lv11"),
            Rscm.lookupList("npc_group.crawling_hand_lv12"))
        registerDropTable(BansheeTable.table,
            Rscm.lookupList("npc_group.banshee_lv23"),)
        registerDropTable(AbyssalDemonTable.table,
            Rscm.lookupList("npc_group.abyssal_demon"),
            Rscm.lookupList("npc_group.abyssal_demon_lv124"))
        registerDropTable(DarkBeastTable.darkBeasts, Rscm.lookup("npc.dark_beast_lv182"))
        registerDropTable(GargoyleDropTable.table, Rscm.lookupList("npc_group.gargoyle_lv111"))
        registerDropTable(NechryaelDropTable.table, Rscm.lookupList("npc_group.nechryael_lv115"))
        registerDropTable(BloodveldDropTable.table,
            Rscm.lookupList("npc_group.bloodveld_lv76"),
            Rscm.lookupList("npc_group.bloodveld_lv81"))
        registerDropTable(InfernalMageDropTable.table, Rscm.lookupList("npc_group.infernal_mage_lv66"))
        registerDropTable(AberrantSpectreDropTable.table, Rscm.lookupList("npc_group.aberrant_spectre_lv96"))


        DropTableRegistry.logDropTableSizes();
    }
}
