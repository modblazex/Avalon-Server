package com.rs.kotlin.game.player.interfaces

import com.rs.core.tasks.WorldTask
import com.rs.core.tasks.WorldTasksManager
import com.rs.java.game.player.Player
import com.rs.java.game.player.TickManager
import com.rs.kotlin.Rscm
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

class TimerOverlay {

    enum class TimerType(
        val display: String,
        val spriteId: Int
    ) {
        OVERLOAD("Overload",           Rscm.lookup("sprite.overload")),
        VENGEANCE("Vengeance",         Rscm.lookup("sprite.vengeance")),
        RENEWAL("Prayer renewal",      Rscm.lookup("sprite.prayer_renwal")),
        ANTIFIRE("Antifire",           Rscm.lookup("sprite.fire_wave")),
        SUPER_ANTIFIRE("Super Antifire",           Rscm.lookup("sprite.fire_wave")),
        ENTANGLE("Entangle",           Rscm.lookup("sprite.entangle")),
        FREEZE("Frozen",               Rscm.lookup("sprite.ice_barrage")),
        TELEBLOCK("TeleportBlock",     Rscm.lookup("sprite.teleport_block")),
        ANTIPOISON("Antipoison",       Rscm.lookup("sprite.green_plusheart")),
        LOCKED("Player Lock",       940),//940 //1436 //1767
    }

    data class LiveTimer(
        val type: TimerType,
        var endTick: Int
    ) {
        fun remainingTicks(currentTick: Int) = max(0, endTick - currentTick)
        fun isExpired(currentTick: Int) = remainingTicks(currentTick) == 0
    }

    private data class RowDef(val textId: Int, val iconId: Int)

    private val ROW_DEFS = listOf(
        RowDef(textId = 3,  iconId = 2),
        RowDef(textId = 5,  iconId = 4),
        RowDef(textId = 7,  iconId = 6),
        RowDef(textId = 9,  iconId = 8),
        RowDef(textId = 11, iconId = 10),
        RowDef(textId = 13, iconId = 12),
        RowDef(textId = 15, iconId = 14),
        RowDef(textId = 17, iconId = 16),
    )

    private data class State(
        val timers: MutableMap<TimerType, LiveTimer> = ConcurrentHashMap(),
        var ticking: Boolean = false
    )

    private fun state(player: Player): State {
        val key = "timer_overlay_state"
        val existing = player.temporaryAttribute()[key] as? State
        if (existing != null) return existing
        val created = State()
        player.temporaryAttribute()[key] = created
        return created
    }

    private fun getTimerOverlayTab(player: Player): Int {
        return if (player.interfaceManager.isResizableScreen) Rscm.lookup("tab.timer_tab_resizeable") else Rscm.lookup("tab.timer_tab")
    }

    fun startTimer(
        player: Player,
        type: TimerType,
        durationTicks: Int,
        extendIfLonger: Boolean = false
    ) {
        val st = state(player)
        val now = player.gameTicks
        val existing = st.timers[type]
        val newEnd = now + durationTicks

        if (existing == null) {
            println("[TimerOverlay] ${player.displayName}: starting new timer ${type.display} for $durationTicks ticks (ends at $newEnd, now=$now)")
            st.timers[type] = LiveTimer(type, newEnd)
        } else {
            println("[TimerOverlay] ${player.displayName}: refreshing timer ${type.display}. Old end=${existing.endTick}, newEnd=$newEnd")
            existing.endTick = if (extendIfLonger) {
                max(existing.endTick, newEnd)
            } else {
                newEnd
            }
        }

        openIfNeeded(player)
        ensureTicking(player)
        render(player)
    }

    fun stopTimer(player: Player, type: TimerType) {
        val st = state(player)
        if (st.timers.remove(type) != null) {
            println("[TimerOverlay] ${player.displayName}: manually stopped timer ${type.display}")
        }
        render(player)
        closeIfEmpty(player)
    }

    fun clearAll(player: Player) {
        println("[TimerOverlay] ${player.displayName}: clearing all timers")
        val st = state(player)
        st.timers.clear()
        render(player)
        closeIfEmpty(player)
    }

    private fun openIfNeeded(player: Player) {
        val tab = getTimerOverlayTab(player)
        if (!player.interfaceManager.containsInterface(3047)) {
            println("[TimerOverlay] ${player.displayName}: opening 3047 on tab $tab")
            player.interfaceManager.sendTab(tab, 3047);
        }
    }

    private fun closeIfEmpty(player: Player) {
        val st = state(player)
        if (st.timers.isEmpty()) {
            println("[TimerOverlay] ${player.displayName}: no timers left, closing overlay")
            if (player.interfaceManager.containsInterface(3047)) {
                player.interfaceManager.closeTab(player.interfaceManager.isResizableScreen, getTimerOverlayTab(player))
            }
            st.ticking = false
        }
    }

    object TimerOverlayTools {
        fun isGoneInTickManager(player: Player, type: TimerType): Boolean {
            val key = TickManager.OVERLAY_TO_TICK[type] ?: return false
            return !player.tickManager.isActive(key)
        }
    }

    private fun ensureTicking(player: Player) {
        val st = state(player)
        if (st.ticking) return
        st.ticking = true
        println("[TimerOverlay] ${player.displayName}: scheduling tick task for timers")

        WorldTasksManager.schedule(object : WorldTask() {
            override fun run() {
                val now = player.gameTicks
                if (player.isDead) {
                    println("[TimerOverlay] ${player.displayName}: stopping task (player dead)")
                    st.ticking = false
                    stop()
                    return
                }
                /*if (!player.interfaceManager.containsTab(getTimerOverlayTab(player))) {
                    println("[TimerOverlay] ${player.displayName}: stopping task (overlay tab closed)")
                    st.ticking = false
                    stop()
                    return
                }*/

                val before = st.timers.size
                st.timers.values.removeIf { timer ->
                    timer.isExpired(now) ||
                            TimerOverlayTools.isGoneInTickManager(player, timer.type)
                }
                val after = st.timers.size
                if (before != after) {
                    println("[TimerOverlay] ${player.displayName}: removed ${before - after} expired timers at tick=$now")
                }

                render(player)

                if (st.timers.isEmpty()) {
                    println("[TimerOverlay] ${player.displayName}: all timers expired, stopping task")
                    st.ticking = false
                    closeIfEmpty(player)
                    stop()
                }
            }
        }, 0, 1)
    }

    private fun render(player: Player) {
        val st = state(player)
        val now = player.gameTicks
        val live = st.timers.values.sortedBy { it.type.ordinal }.take(ROW_DEFS.size)

        live.forEach { t ->
            println("[TimerOverlay] ${player.displayName}: render ${t.type.display} -> ${t.remainingTicks(now)} ticks remaining")
        }

        ROW_DEFS.forEachIndexed { idx, row ->
            if (idx < live.size) {
                val t = live[idx]
                val remain = t.remainingTicks(now)
                val text = formatTicks(remain)

                player.packets.sendHideIComponent(3047, row.iconId, false)
                player.packets.sendHideIComponent(3047, row.textId, false)
                player.packets.sendIComponentSprite(3047, row.iconId, t.type.spriteId)
                player.packets.sendTextOnComponent(3047, row.textId, text)
            } else {
                player.packets.sendHideIComponent(3047, row.iconId, true)
                player.packets.sendHideIComponent(3047, row.textId, true)
                player.packets.sendTextOnComponent(3047, row.textId, "")
            }
        }
    }

    private fun formatTicks(ticks: Int): String {
        if (ticks >= 10000) {
            return "∞"
        }
        val totalSeconds = (ticks * 0.6).toInt()
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return if (m > 0) String.format("%dm %02ds", m, s) else String.format("%ds", s)
    }
}
