package com.rs.kotlin.game.world.pvp

import com.rs.java.game.Entity
import com.rs.java.game.World
import com.rs.java.game.item.Item
import com.rs.java.game.npc.NPC
import com.rs.java.game.player.Player
import com.rs.java.game.player.TickManager
import com.rs.java.game.player.controlers.WildernessControler
import com.rs.java.utils.HexColours
import com.rs.kotlin.Rscm
import com.rs.kotlin.game.world.util.Msg
import java.util.*
import kotlin.math.abs

object PvpManager {

    private const val INTERFACE_ID = 3043
    private const val COMP_LEVEL_RANGE = 3
    private const val COMP_EP = 4
    private const val COMP_SPRITE = 2
    private const val VARBIT_COMBAT_LEVEL = 1000
    private const val SPRITE_DANGER = 1577
    private const val SPRITE_SAFE = 1576

    var levelRange: Int = 15
    var enabled: Boolean = true

    private const val GRACE_MS = 10_000L
    private const val RECENT_HIT_WINDOW_MS = 10_000L
    private val lastPvpHitAt = WeakHashMap<Entity, Long>()
    private val safeGraceUntil = WeakHashMap<Player, Long>()
    private val wasInRawSafe = WeakHashMap<Player, Boolean>()

    private fun now() = System.currentTimeMillis()
    private fun isRawSafe(p: Player) = SafeZoneService.isAtSafezone(p)
    private fun hasGrace(p: Player) = now() < (safeGraceUntil[p] ?: 0L)
    private fun isEffectivelySafeForSelf(p: Player): Boolean = isRawSafe(p) && !hasGrace(p)

    @JvmStatic
    fun isInDangerous(player: Player): Boolean {
        return !isEffectivelySafeForSelf(player)
    }

    @JvmStatic
    fun onPlayerDamagedByPlayer(victim: Player, attacker: Player) { // NEW
        val t = now()
        lastPvpHitAt[victim] = t
        if (!attacker.attackedBy.containsKey(victim))
            attacker.setWildernessSkull()
    }

    @JvmStatic
    fun onPlayerDamagedByNpc(victim: Player) { // NEW
        val t = now()
        lastPvpHitAt[victim] = t
    }

    @JvmStatic
    fun onDeath(deadPlayer: Player, killer: Player) {
        lastPvpHitAt.remove(deadPlayer)
        lastPvpHitAt.remove(killer)

        val ep = killer.ep
        val chance = ep / 100.0
        if (Math.random() > chance) return
        val drops = EpTable.main.rollDrops(killer)
        for (drop in drops) {
            val item = Item(drop.itemId, drop.amount)
            World.addGroundItem(item, killer, killer, true, 60)
            killer.message("You received ${item.amount} x ${item.name} as a PvP drop!")

            val price = item.definitions.tipitPrice
            when {
                price in 1_000_000 .. 9_999_999 -> Msg.world(Msg.RED, "${killer.displayName} has received ${item.name} from a PvP kill!")
                price >= 10_000_000 -> Msg.world(Msg.PURPLE, "${killer.displayName} has received ${item.name} from a PvP kill!")
            }
        }
        killer.ep = 0
        refreshAll(killer)
    }

    private fun onEnterRawSafezone(player: Player) { // NEW
        val lastHit = lastPvpHitAt[player] ?: 0L
        if (now() - lastHit <= RECENT_HIT_WINDOW_MS) {
            val until = now() + GRACE_MS
            val prev = safeGraceUntil[player] ?: 0L
            if (until > prev) safeGraceUntil[player] = until
            val secs = ((until - now() + 999) / 1000).toInt()
            player.message("You're under attack! You can fight in the safe zone for $secs seconds.")
        }
    }

    private fun inPvpGame(player: Player): Boolean {
        return player.inTournament()
    }

    @JvmStatic
    fun onLogin(player: Player) {
        if (!enabled) return
        if (inPvpGame(player)) {
            closeInterface(player)
            return
        }
        ensureInterfaceOpen(player)
        refreshAll(player)
        onMoved(player)
    }

    @JvmStatic
    fun onLogout(player: Player) {
        closeInterface(player)
    }

    @JvmStatic
    fun onMoved(player: Player) {
        if (!enabled) return
        if (inPvpGame(player)) {
            closeInterface(player)
            return
        }
        val inRaw = isRawSafe(player)
        val was = wasInRawSafe[player] ?: false
        if (!was && inRaw) onEnterRawSafezone(player)
        wasInRawSafe[player] = inRaw

        val safeEffectiveForSelf = isEffectivelySafeForSelf(player)
        val canPvp = !safeEffectiveForSelf

        if (player.isCanPvp != canPvp) {
            player.setCanPvp(canPvp)
            player.getAppearence().generateAppearenceData()
        }
        if (canPvp) {
            val combatAndSummon = player.skills.combatLevel + player.skills.summoningCombatLevel
            player.packets.sendGlobalVar(VARBIT_COMBAT_LEVEL, combatAndSummon)
        } else {
            player.packets.sendGlobalVar(VARBIT_COMBAT_LEVEL, 0)
        }
        sendPvpInterface(player, safeEffectiveForSelf)
        refreshAttackOption(player, canPvp)
    }

    @JvmStatic
    fun canPlayerAttack(attacker: Player, target: Entity): Boolean {
        if (!enabled) return false
        if (target is NPC) return true
        if (target !is Player) return true

        if (!canFightAcrossSafezones(attacker, target)) {
            attacker.message("You can't attack here.")
            return false
        }

        val diff = abs(attacker.skills.combatLevel - target.skills.combatLevel)
        if (diff > levelRange) {
            attacker.message("You can't attack ${target.displayName} - your level difference is too great.")
            return false
        }
        return true
    }

    private const val EP_IDLE_TICK_CHANCE = 0.003   // 1% chance per tick just for being in danger
    private const val EP_COMBAT_TICK_CHANCE = 0.02 // 5% chance per combat tick
    private const val EP_GAIN_MIN = 1
    private const val EP_GAIN_MAX = 3
    private const val EP_MAX = 100

    @JvmStatic
    fun onEpTick(player: Player) {
        if (!enabled) return
        if (player.isDead || !player.isCanPvp) return
        if (!isInDangerous(player)) return

        val baseChance = EP_IDLE_TICK_CHANCE
        val combatBonus = if (isInCombat(player) && isAttacking(player)) EP_COMBAT_TICK_CHANCE else 0.0
        val chance = baseChance + combatBonus

        if (Math.random() < chance) {
            val gain = (EP_GAIN_MIN..EP_GAIN_MAX).random()
            increaseEp(player, gain)
        }
    }

    private fun isAttacking(player: Player): Boolean {
        return player.tickManager.isActive(TickManager.TickKeys.LAST_ATTACK_TICK)
    }

    private fun isInCombat(player: Player): Boolean {
        val lastHit = lastPvpHitAt[player] ?: return false
        return now() - lastHit <= RECENT_HIT_WINDOW_MS
    }

    private fun increaseEp(player: Player, amount: Int) {
        val oldEp = player.ep
        val newEp = (oldEp + amount).coerceAtMost(EP_MAX)
        if (newEp > oldEp) {
            player.ep = newEp
            sendPvpInterface(player, isEffectivelySafeForSelf(player)) // refresh UI
        }
    }

    private fun canFightAcrossSafezones(a: Player, b: Player): Boolean { // NEW
        val aInSafe = isRawSafe(a)
        val bInSafe = isRawSafe(b)
        if (!(aInSafe || bInSafe)) return true

        val aGrace = hasGrace(a)
        val bGrace = hasGrace(b)
        return aGrace || bGrace
    }

    fun canHit(attacker: Player, target: Entity): Boolean {
        if (target !is Player) return true
        val diff = abs(attacker.skills.combatLevel - target.skills.combatLevel)
        return diff <= levelRange
    }

    fun onFirstHitPlayer(attacker: Player) {
        if (attacker.isDead) return
        if (!isEffectivelySafeForSelf(attacker)) {
            attacker.setWildernessSkull()
        }
    }

    private fun sendPvpInterface(player: Player, safeForSelf: Boolean) {
        ensureInterfaceOpen(player)

        if (hasGrace(player) && isRawSafe(player)) {
            val remainingMs = (safeGraceUntil[player] ?: 0L) - now()
            val secs = ((remainingMs + 999) / 1000).coerceAtLeast(0) // round up, min 0
            val text = "$secs seconds"
            player.packets.sendIComponentSprite(INTERFACE_ID, COMP_SPRITE, SPRITE_DANGER)
            player.packets.sendTextOnComponent(INTERFACE_ID, COMP_LEVEL_RANGE, text)
            player.packets.sendTextOnComponent(INTERFACE_ID, COMP_EP, "")

        } else {
            val levelRangeText = buildLevelRangeText(player)
            player.packets.sendTextOnComponent(INTERFACE_ID, COMP_LEVEL_RANGE, levelRangeText)

            val ep = player.ep
            val colour = when {
                ep >= 70 -> HexColours.Colour.GREEN.hex
                ep in 25..69 -> HexColours.Colour.YELLOW.hex
                else -> HexColours.Colour.RED.hex
            }
            player.packets.sendTextOnComponent(INTERFACE_ID, COMP_EP, "EP: $colour${ep}%")
        }

        player.packets.sendSpriteOnIComponent(
            INTERFACE_ID,
            COMP_SPRITE,
            if (safeForSelf && !hasGrace(player) && isRawSafe(player) ) SPRITE_SAFE else SPRITE_DANGER
        )

        showKDRInter(player)
    }


    private fun showKDRInter(player: Player) {
        if (player.toggles("KDRINTER", false)) {
            val kills = player.killCount
            val deaths = player.deathCount
            val ratioText = if (deaths == 0) kills.toString() else String.format("%.2f", kills.toDouble() / deaths)
            if (!player.interfaceManager.containsInterface("interface.kdr_interface")) player.interfaceManager.sendTab("tab.kdr_tab", "interface.kdr_interface")
            player.packets.sendTextOnComponent("interface.kdr_interface", 2, "Kills: $kills")
            player.packets.sendTextOnComponent("interface.kdr_interface", 3, "Deaths: $deaths")
            player.packets.sendTextOnComponent("interface.kdr_interface", 4, "Ratio: $ratioText")
        } else {
            if (player.interfaceManager.containsInterface("interface.kdr_interface")) {
                player.interfaceManager.closeTab("tab.kdr_tab")
            }
        }
    }

    @JvmStatic
    fun refreshAll(player: Player) {
        if (inPvpGame(player)) {
            closeInterface(player)
            return
        }

        val safeForSelf = isEffectivelySafeForSelf(player)
        sendPvpInterface(player, safeForSelf)
        refreshAttackOption(player, canAttackHere = !safeForSelf)
    }

    private fun ensureInterfaceOpen(player: Player) {
        val tabId = if (player.interfaceManager.isResizableScreen) Rscm.lookup("tab.pvp_tab_resizeable") else Rscm.lookup("tab.pvp_tab")
        if (!player.interfaceManager.containsInterface(INTERFACE_ID))
            player.interfaceManager.sendTab(tabId, INTERFACE_ID)
    }

    private fun closeInterface(player: Player) {
        val tabId = if (player.interfaceManager.isResizableScreen) Rscm.lookup("tab.pvp_tab_resizeable") else Rscm.lookup("tab.pvp_tab")
        player.interfaceManager.closeTab(tabId)
    }

    private fun buildLevelRangeText(player: Player): String {
        val levelRange = levelRange
        val wildyLevel = if (player.controlerManager.controler is WildernessControler) WildernessControler.getWildLevel(player) else 0
        val c = player.skills.combatLevel
        val minus = c - levelRange - wildyLevel
        val plus = c + levelRange + wildyLevel
        return when {
            minus < 4 -> "3 - $plus"
            plus > 137 -> "$minus - 138"
            else -> "$minus - $plus"
        }
    }

    private fun refreshAttackOption(player: Player, canAttackHere: Boolean) {
        player.packets.sendPlayerOption(if (canAttackHere) "Attack" else "null", 1, false)
    }
}
