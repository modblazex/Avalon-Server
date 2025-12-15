package com.rs.kotlin.game.player.interfaces

import com.rs.core.tasks.WorldTask
import com.rs.core.tasks.WorldTasksManager
import com.rs.java.game.Entity
import com.rs.java.game.npc.NPC
import com.rs.java.game.player.Player
import com.rs.java.game.player.Skills
import com.rs.java.game.player.TickManager
import com.rs.kotlin.Rscm
import com.rs.kotlin.game.player.combat.CombatAction
import com.rs.kotlin.game.player.combat.Weapon
import com.rs.kotlin.game.player.combat.special.CombatContext
import kotlin.math.ceil
import kotlin.math.roundToInt

class HealthOverlay {

    private data class OverlayState(
        val hpText: String,
        val hitchance: Int,
        val skillRows: List<String>
    )
    //toggles("HITCHANCE_OVERLAY")
    //toggles("LEVEL_STATS_OVERLAY")

    fun sendOverlay(player: Player, target: Entity) {
        //checkCombatLevel(player, target)
        updateHealthOverlay(player, target, false)
        if (player.toggles("HEALTH_OVERLAY", false) &&
            (!player.interfaceManager.containsTab(getHealthOverlayId(player)))
        ) {
            player.interfaceManager.sendTab(getHealthOverlayId(player), 3037)
            val pixels: Int =
                (target.hitpoints.toDouble() / target.getMaxHitpoints() * 126.0).roundToInt()
            player.packets.sendRunScript(6252, pixels)
            player.packets.sendRunScript(6253, 0)
            target.temporaryAttribute()["last_hp_${player.index}"] = target.hitpoints
        }
    }

    fun checkCombatLevel(player: Player, target: Entity) {
        when (target) {
            is NPC -> checkNpcCombatLevel(player, target)
            is Player -> checkPlayerCombatLevel(player, target)
        }
    }

    private fun checkForClose(player: Player): Boolean {
        if (player.isDead || player.temporaryTarget == null || player.temporaryTarget.isDead) {
            return true
        }
        if (!player.toggles("HEALTH_OVERLAY", false)) {
            return true
        }
        if (!player.temporaryTarget.withinDistance(player.temporaryTarget, 32)) {
            return true
        }
        if (!player.tickManager.isActive(TickManager.TickKeys.LAST_INTERACTION_TARGET) && !player.tickManager.isActive(TickManager.TickKeys.LAST_ATTACK_TICK)) {
            return true
        }
        return false
    }

    fun closeOverlay(player: Player) {
        if (player.interfaceManager.containsTab(getHealthOverlayId(player))) {
            if (checkForClose(player) &&
                player.interfaceManager.containsTab(getHealthOverlayId(player))
            ) {
                player.temporaryAttribute().remove("overlay_state")
                player.removeTemporaryTarget()
                player.interfaceManager.closeTab(
                    player.interfaceManager.isResizableScreen,
                    getHealthOverlayId(player)
                )
            }
        }
    }

    fun getHealthOverlayId(player: Player): Int {
        return if (player.interfaceManager.isResizableScreen) Rscm.lookup("tab.health_overlay_resizeable") else Rscm.lookup("tab.health_overlay")
    }

    fun updateHealthOverlay(player: Player, target: Entity, updateScript: Boolean) {
        val name: String
        val hpText: String
        var showLevel = false
        var levelText = ""

        when (target) {
            is Player -> {
                name = buildTargetName(target)
                hpText = buildHpText(player, target.hitpoints, target.maxHitpoints)
                levelText = buildPlayerLevelText(player, target)
                showLevel = true
            }
            is NPC -> {
                name = target.name
                hpText = buildHpText(player, target.hitpoints, target.maxHitpoints)
                val (show, text) = buildNpcLevelText(player, target)
                showLevel = show
                levelText = text
            }
            else -> return
        }

        val hitchance = (getHitchance(player, target) * 100).roundToInt()
        val showStats = player.toggles("LEVELSTATUS_OVERLAY", true)
        val skillRows = if (showStats) buildSkillRows(player) else emptyList()

        val newState = OverlayState(hpText, hitchance, skillRows)
        val lastState = player.temporaryAttribute()["overlay_state"] as? OverlayState

        if (lastState != newState) {
            player.temporaryAttribute()["overlay_state"] = newState

            // Name + HP
            player.packets.sendTextOnComponent(3037, 6, name)
            player.packets.sendTextOnComponent(3037, 7, hpText)

            // Own comps 8/9/10/11 here exclusively
            val showHitch = player.toggles("HITCHANCE_OVERLAY", false)

            if (showHitch) {
                // Show hitchance on 9; show level on 11 if we have it
                player.packets.sendHideIComponent(3037, 8, false)
                player.packets.sendHideIComponent(3037, 9, false)
                player.packets.sendTextOnComponent(
                    3037, 9, "Hit%: ${getHitchanceColour(hitchance)}$hitchance%"
                )

                if (showLevel) {
                    player.packets.sendHideIComponent(3037, 10, false)
                    player.packets.sendHideIComponent(3037, 11, false)
                    player.packets.sendTextOnComponent(3037, 11, levelText)
                } else {
                    player.packets.sendHideIComponent(3037, 10, true)
                    player.packets.sendHideIComponent(3037, 11, true)
                    player.packets.sendTextOnComponent(3037, 11, "")
                }
            } else {
                // No hitchance: put level on 9, hide 10/11 to avoid duplicate/conflicts
                if (showLevel) {
                    player.packets.sendHideIComponent(3037, 8, false)
                    player.packets.sendHideIComponent(3037, 9, false)
                    player.packets.sendTextOnComponent(3037, 9, levelText)
                } else {
                    player.packets.sendHideIComponent(3037, 8, true)
                    player.packets.sendHideIComponent(3037, 9, true)
                    player.packets.sendTextOnComponent(3037, 9, "")
                }
                player.packets.sendHideIComponent(3037, 10, true)
                player.packets.sendHideIComponent(3037, 11, true)
                player.packets.sendTextOnComponent(3037, 11, "")
            }

            // Level stats overlay
            if (showStats) {
                updateSkillRows(player, skillRows)
            } else {
                // Hide all stat components 12..26 (boxes, icons, texts)
                for (compId in 12..26) {
                    player.packets.sendHideIComponent(3037, compId, true)
                    // Only safe to clear text on text components; harmless if not text.
                    player.packets.sendTextOnComponent(3037, compId, "")
                }
            }
        }

        // HP bar animation update stays the same
        if (updateScript) {
            val maxHp = target.getMaxHitpoints().toDouble()
            val currentHp = target.hitpoints
            val lastHp = target.temporaryAttribute()["last_hp_${player.index}"] as? Int ?: currentHp
            val damageTaken = (lastHp - currentHp).coerceAtLeast(0)

            val newPixels = (currentHp.toDouble() / maxHp * 126.0).roundToInt()
            val oldPixels = (lastHp.toDouble() / maxHp * 126.0).roundToInt()
            val pixelLoss = ((damageTaken.toDouble() / maxHp) * 126.0).roundToInt()

            val adjustedPixels = if (damageTaken > 0) {
                (oldPixels - pixelLoss).coerceAtLeast(newPixels).coerceAtLeast(0)
            } else newPixels

            player.packets.sendRunScript(6252, adjustedPixels)
            WorldTasksManager.schedule(object : WorldTask() {
                override fun run() {
                    player.packets.sendRunScript(6253, adjustedPixels)
                    stop()
                }
            }, 0, 1)

            target.temporaryAttribute()["last_hp_${player.index}"] = currentHp
        }
    }


    private fun buildNpcLevelText(player: Player, npc: NPC): Pair<Boolean, String> {
        val playerLevel = getRelevantCombatLevel(player)
        val npcLevel = npc.combatLevel
        if (npcLevel == 0) return false to "" // nothing to show
        val text = "Level: ${getLevelColour(playerLevel, npcLevel)}$npcLevel"
        return true to text
    }

    private fun buildPlayerLevelText(player: Player, target: Player): String {
        val playerLevel = getRelevantCombatLevel(player)
        val targetLevel = target.skills.combatLevel
        val levelDisplay = if (player.inPkingArea() || player.isAtPvP) {
            "$targetLevel+${target.skills.summoningCombatLevel}"
        } else {
            "${target.skills.combatLevelWithSummoning}"
        }
        return "Lvl: ${getLevelColour(playerLevel, targetLevel)}$levelDisplay"
    }

    private fun buildTargetName(target: Player): String {
        return buildString {
            if (target.appearence.title != -1) {
                append(target.appearence.titleName)
            }
            append(target.displayName)
        }
    }

    private fun buildHpText(viewer: Player, currentHp: Int, maxHp: Int): String {
        return if (viewer.varsManager.getBitValue(1485) == 1) {
            "${ceil(currentHp / 10.0).toInt()}/${ceil(maxHp / 10.0).toInt()}"
        } else {
            "$currentHp/$maxHp"
        }
    }

    private fun buildSkillRows(player: Player): List<String> {
        data class SkillRow(
            val name: String,
            val iconSpriteId: Int,
            val current: Int,
            val base: Int
        ) {
            val delta = current - base
            val boosted = delta > 0
            val drained = delta < 0
            val changed = boosted || drained
        }

        val allSkills = listOf(
            SkillRow(
                "Attack", 1478,
                player.skills.getLevel(Skills.ATTACK),
                player.skills.getLevelForXp(Skills.ATTACK)
            ),
            SkillRow(
                "Strength", 1479,
                player.skills.getLevel(Skills.STRENGTH),
                player.skills.getLevelForXp(Skills.STRENGTH)
            ),
            SkillRow(
                "Defence", 1480,
                player.skills.getLevel(Skills.DEFENCE),
                player.skills.getLevelForXp(Skills.DEFENCE)
            ),
            SkillRow(
                "Ranged", 1481,
                player.skills.getLevel(Skills.RANGE),
                player.skills.getLevelForXp(Skills.RANGE)
            ),
            SkillRow(
                "Magic", 1483,
                player.skills.getLevel(Skills.MAGIC),
                player.skills.getLevelForXp(Skills.MAGIC)
            )
        )

        return allSkills
            .filter { it.changed }
            .sortedWith(
                compareByDescending<SkillRow> { it.boosted }
                    .thenBy { it.name }
            )
            .map { row ->
                val (colour, shad) = if (row.boosted) {
                    "00FF00" to "<shad=000000>"
                } else {
                    "FF0000" to "<shad=000000>"
                }
                "$shad<col=$colour>${row.current}/${row.base}</col>|${row.iconSpriteId}"
            }
    }

    private fun updateSkillRows(player: Player, skillRows: List<String>) {
        data class RowDef(val textId: Int, val iconId: Int, val boxId: Int)
        val rows = listOf(
            RowDef(13, 14, 12),
            RowDef(16, 17, 15),
            RowDef(19, 20, 18),
            RowDef(22, 23, 21),
            RowDef(25, 26, 24)
        )

        rows.forEachIndexed { i, row ->
            if (i < skillRows.size) {
                val entry = skillRows[i]
                val parts = entry.split("|")
                val text = parts[0]
                val spriteId = parts[1].toInt()

                player.packets.sendHideIComponent(3037, row.boxId, false)
                player.packets.sendHideIComponent(3037, row.iconId, false)
                player.packets.sendHideIComponent(3037, row.textId, false)

                player.packets.sendIComponentSprite(3037, row.iconId, spriteId)
                player.packets.sendTextOnComponent(3037, row.textId, text)
            } else {
                player.packets.sendHideIComponent(3037, row.boxId, true)
                player.packets.sendHideIComponent(3037, row.iconId, true)
                player.packets.sendHideIComponent(3037, row.textId, true)
                player.packets.sendTextOnComponent(3037, row.textId, "")
            }
        }
    }

    /** Hitchance wrapper */
    private fun getHitchance(player: Player, target: Entity): Double {
        val combatStyle = CombatAction.getCombatStyle(player, target)
        val weapon = Weapon.getWeapon(player.equipment.weaponId)
        val combatContext =
            if (player.combatDefinitions.isUsingSpecialAttack && weapon.special != null) {
                CombatContext(
                    combat = combatStyle,
                    attacker = player,
                    defender = target,
                    weapon = weapon,
                    weaponId = player.equipment.weaponId,
                    attackStyle = weapon.weaponStyle.styleSet.styleAt(player.combatDefinitions.attackStyle)!!,
                    attackBonusType = weapon.weaponStyle.styleSet.bonusAt(player.combatDefinitions.attackStyle)!!,
                    usingSpecial = true
                )
            } else null

        return combatStyle.getHitChance(player, target, 1.0, combatContext)
    }

    private fun checkNpcCombatLevel(player: Player, npc: NPC) {
        val playerLevel = getRelevantCombatLevel(player)
        val npcLevel = npc.combatLevel

        val levelText = "Level: ${getLevelColour(playerLevel, npcLevel)}$npcLevel"
        if (npcLevel != 0 && !player.toggles("HITCHANCE_OVERLAY", false)) {
            player.packets.sendHideIComponent(3037, 8, false)
            player.packets.sendHideIComponent(3037, 9, false)
            player.packets.sendHideIComponent(3037, 10, true)
            player.packets.sendHideIComponent(3037, 11, true)
            player.packets.sendTextOnComponent(3037, 9, levelText)
        } else {
            player.packets.sendHideIComponent(3037, 10, npcLevel == 0)
            player.packets.sendHideIComponent(3037, 11, npcLevel == 0)
            player.packets.sendTextOnComponent(3037, 11, levelText)
        }
    }

    private fun checkPlayerCombatLevel(player: Player, target: Player) {
        val playerLevel = getRelevantCombatLevel(player)
        val targetLevel = target.skills.combatLevel

        player.packets.sendHideIComponent(3037, 10, false)
        player.packets.sendHideIComponent(3037, 11, false)

        val levelDisplay = if (player.inPkingArea() || player.isAtPvP) {
            "$targetLevel+${target.skills.summoningCombatLevel}"
        } else {
            "${target.skills.combatLevelWithSummoning}"
        }

        val levelText = "Lvl: ${getLevelColour(playerLevel, targetLevel)}$levelDisplay"
        player.packets.sendTextOnComponent(3037, 11, levelText)
    }

    private fun getRelevantCombatLevel(player: Player): Int {
        return if (player.inPkingArea()) {
            player.skills.combatLevel
        } else {
            player.skills.combatLevelWithSummoning
        }
    }

    private fun getHitchanceColour(hitchance: Int): String {
        val shad = "<shad=000000>"
        return when {
            hitchance <= 25 -> "$shad<col=FF0000>"
            hitchance in 26..75 -> "$shad<col=FFFF00>"
            hitchance > 75 -> "$shad<col=00FF00>"
            else -> "$shad<col=FFFFFF>"
        }
    }


    private fun getLevelColour(playerLevel: Int, targetLevel: Int): String {
        val shad = "<shad=000000>"
        return when {
            playerLevel > targetLevel -> "$shad<col=00FF00>"
            playerLevel == targetLevel -> "$shad<col=FFFF00>"
            else -> "$shad<col=FF0000>"
        }
    }
}
