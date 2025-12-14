package com.rs.kotlin.game.player.combat

import com.rs.core.tasks.WorldTask
import com.rs.core.tasks.WorldTasksManager
import com.rs.java.game.Entity
import com.rs.java.game.npc.NPC
import com.rs.java.game.player.Player
import com.rs.java.game.player.TickManager
import com.rs.java.game.player.actions.combat.QueuedInstantCombat
import com.rs.java.utils.Utils
import com.rs.kotlin.game.player.action.NewAction
import com.rs.kotlin.game.player.combat.magic.MagicStyle
import com.rs.kotlin.game.player.combat.magic.special.GreaterRunicStaffWeapon
import com.rs.kotlin.game.player.combat.magic.special.NightmareStaff
import com.rs.kotlin.game.player.combat.magic.special.ObliterationWeapon
import com.rs.kotlin.game.player.combat.magic.special.PolyporeStaff
import com.rs.kotlin.game.player.combat.melee.MeleeStyle
import com.rs.kotlin.game.player.combat.range.RangeData
import com.rs.kotlin.game.player.combat.range.RangedStyle
import com.rs.kotlin.game.player.combat.special.SpecialAttack
import com.rs.kotlin.game.player.interfaces.HealthOverlay
import com.rs.kotlin.game.world.pvp.PvpManager
import kotlin.math.abs

class CombatAction(
    private val target: Entity
) : NewAction() {

    companion object {
        @JvmStatic
        fun getCombatStyle(player: Player, target: Entity): CombatStyle {
            val spellId = player.getCombatDefinitions().spellId
            return when {
                spellId != 0 -> MagicStyle(player, target)
                NightmareStaff.hasWeapon(player) && player.combatDefinitions.isUsingSpecialAttack -> MagicStyle(player, target)
                GreaterRunicStaffWeapon.hasWeapon(player) && GreaterRunicStaffWeapon.getSpellId(player) != -1 -> MagicStyle(player, target)
                PolyporeStaff.hasWeapon(player) -> MagicStyle(player, target)
                ObliterationWeapon.hasWeapon(player) && player.combatDefinitions.isUsingSpecialAttack -> MagicStyle(player, target);
                isRangedWeapon(player) -> RangedStyle(player, target)
                else -> MeleeStyle(player, target)
            }
        }

        private fun isRangedWeapon(player: Player): Boolean {
            val weaponId = player.equipment.getWeaponId()
            val ranged = RangeData.getWeaponByItemId(weaponId)
            return ranged != null
        }
    }

    enum class CombatPhase {
        HIT
    }

    private var lastTargetX = -1
    private var lastTargetY = -1

    private var followTask: WorldTask? = null

    private var phase = CombatPhase.HIT
    private var ticksUntilNextPhase = 0
    private lateinit var style: CombatStyle

    override fun start(player: Player): Boolean {
        if (target.isDead || target.hasFinished() || player.isLocked) {
            stop(player, true)
            return false
        }
        player.faceEntity(target)
        player.setNextFaceEntity(target);
        player.resetWalkSteps()
        val spellId = player.getCombatDefinitions().spellId
        style = getCombatStyle(player, target)
        player.combatStyle = style
        player.tickManager.addTicks(TickManager.TickKeys.LAST_INTERACTION_TARGET, 10)
        player.temporaryTarget = target;
        player.healthOverlay.sendOverlay(player, target)
        val requiredDistance = getAdjustedFollowDistance(target);
        if (player.isOutOfRange(target, requiredDistance)) {
            player.calcFollow(target, if (player.run) 2 else 1, true, true)
        }
        if (player.isDiagonalMeleeBlocked(target, style)) {
            player.calcFollow(target, if (player.run) 2 else 1, true, true)
        }
        if (player.isCollidingWithTarget(target)) {
            if (player.isFrozen) {
                player.packets.sendGameMessage("A magical force prevents you from moving.")
                return true
            }
            handleCollisionMovement(player, target, player.size)
        }
        ensureFollowTask(player)
        return true
    }

    override fun process(player: Player): Boolean {
        if (target.isDead || target.hasFinished() || player.isLocked) {
            stop(player, true)
            return false
        }
        player.healthOverlay.updateHealthOverlay(player, target, true)
        style = getCombatStyle(player, target)
        player.temporaryTarget = target;

        if (player.isCollidingWithTarget(target)) {
            if (player.isFrozen) {
                player.packets.sendGameMessage("A magical force prevents you from moving.")
                return true
            }
            handleCollisionMovement(player, target, player.size)
        }


        val toExecute = mutableListOf<QueuedInstantCombat<out SpecialAttack>>()
        var totalEnergyCost = 0

        for (queued in player.queuedInstantCombats.toList()) {
            val queuedStyle = queued.context.combat

            if (!player.isOutOfRange(target, queuedStyle.getAttackDistance()) &&
                !shouldAdjustDiagonal(player, target)
            ) {
                val cost = queued.special.energyCost
                if (player.combatDefinitions.specialAttackPercentage < totalEnergyCost + cost) {
                    player.queuedInstantCombats.clear()
                    break
                }
                totalEnergyCost += cost
                toExecute.add(queued)
            }
        }

        if (toExecute.isNotEmpty()) {
            for (queued in toExecute) {
                if (queued.special is SpecialAttack.InstantCombat) {
                    if (!check(player, target))
                        return false
                    player.faceEntity(queued.context.defender)
                    player.combatDefinitions.decreaseSpecialAttack(queued.special.energyCost)
                    queued.special.execute(queued.context)
                    player.queuedInstantCombats.remove(queued)
                }
            }
            player.stopAll(false, true, true)
        }

        val activeInstantSpecial = player.activeInstantSpecial
        if (activeInstantSpecial != null) {
            player.clearActiveInstantSpecial()
            val special = activeInstantSpecial.special
            if (special is SpecialAttack.InstantRangeCombat) {
                if (!check(player, target))
                    return false
                player.faceEntity(activeInstantSpecial.context.defender)
                special.execute(activeInstantSpecial.context)
                player.stopAll(false, true, true)
                return true
            }
        }

        player.combatStyle = style
        val requiredDistance = getAdjustedFollowDistance(target);
        if (player.isOutOfRange(target, requiredDistance)) {
            player.resetWalkSteps()
            player.calcFollow(target, if (player.run) 2 else 1, true, true)
        }
        if (player.isDiagonalMeleeBlocked(target, style)) {
            player.resetWalkSteps()
            player.calcFollow(target, if (player.run) 2 else 1, true, true)
        }
        if (player.isOutOfRegion(target)) {
            return false
        }
        return true
    }

    override fun processWithDelay(player: Player): Int {
        if (!process(player)) {
            return -1
        }
        val requiredDistance = getAdjustedAttackRange(player, target)
        if (player.isOutOfRange(target, requiredDistance)) {
            return 0
        }
        if (player.isCollidingWithTarget(target)) {
            return 0
        }
        if (player.isDiagonalMeleeBlocked(target, style)) {
            return 0
        }
        if (player.familiarAutoAttack) {
            if (player.familiar != null && !player.familiar.combat.hasTarget() && player.isAtMultiArea && target.isAtMultiArea) {
                player.familiar.setTarget(target)
            }
        }

        return when (phase) {
            CombatPhase.HIT -> {
                if (!check(player, target))
                    return -1
                if (validateAttack(player, target)) {
                    player.tickManager.addTicks(TickManager.TickKeys.LAST_ATTACK_TICK, 10)
                    target.tickManager.addTicks(TickManager.TickKeys.LAST_ATTACKED_TICK, 10)
                    target.tickManager.addTicks(TickManager.TickKeys.PJ_TIMER, 12);
                    target.attackedBy = player
                    if (target is Player) {
                        PvpManager.onPlayerDamagedByPlayer(target, player)
                    }
                    style.attack()
                }
                phase = CombatPhase.HIT
                val foodLock = player.getFoodLockTicks()
                style.getAttackSpeed() + foodLock - 1
            }
        }
    }

    private fun check(player: Player, target: Entity): Boolean {
        if (target.isDead || target.hasFinished())
            return false
        if (!style.canAttack(player, target)) {
            return false
        }
        if (!player.controlerManager.canHit(target) || !player.controlerManager.keepCombating(target) || !player.controlerManager.canAttack(target))
            return false
        if (!PvpManager.canPlayerAttack(player, target))
            return false
        if (player.isAtMultiArea && !target.isAtMultiArea) {
            if (target.attackedBy != player && target.tickManager.isActive(TickManager.TickKeys.PJ_TIMER)) {
                player.message("That " + (if (player.getAttackedBy() is Player) "player" else "npc") + " is already in combat.")
                return false
            }
        }
        if (!target.isAtMultiArea && !player.isAtMultiArea) {
            if (player.getAttackedBy() !== target && player.tickManager.isActive(TickManager.TickKeys.PJ_TIMER)) {
                player.message("You are already in combat.")
                return false
            }
            if (target.attackedBy !== player && target.tickManager.isActive(TickManager.TickKeys.PJ_TIMER)) {
                if (target is NPC) {
                    if (target.id == 4474 || target.id == 7891)
                        return true
                }
                player.message(
                    ("That " + (if (player.getAttackedBy() is Player) "player" else "npc") + " is already in combat.")
                )
                return false
            }
        }
        return true;
    }

    private fun ensureFollowTask(player: Player) {
        /*if (followTask != null) {
            player.message("cant assign new followTask");
            return
        }*/

        val requiredDistance = getAdjustedFollowDistance(target);
        val size = player.size

        followTask = object : WorldTask() {
            override fun run() {
                if (player.isDead || player.isLocked || target.isDead || target.hasFinished() || !player.isActive) {
                    stop()
                    return
                }
                if (player.isOutOfRegion(target)) {
                    stop()
                    return
                }
                if (player.newActionManager.getCurrentAction() == null) {
                    stop()
                    return
                }
                if (player.newActionManager.getCurrentAction() != this@CombatAction) {
                    stop()
                    return
                }

                if (player.isCollidingWithTarget(target)) {
                    if (player.isFrozen) {
                        player.packets.sendGameMessage("A magical force prevents you from moving.")
                        stopFollowTask(player)
                        return
                    }
                    if (handleCollisionMovement(player, target, size)) {
                        stop()
                    }
                    return
                }
                if (shouldAdjustDiagonal(player, target)) {
                    val targetShouldAdjustDiagonal = if (target is Player) shouldAdjustDiagonal(target, player) else false
                    val targetHasPriority = hasMovementPriority(target as? Player ?: player, player)

                    if (targetShouldAdjustDiagonal && targetHasPriority) {
                        player.resetWalkSteps()
                        return
                    }
                    if (player.isFrozen) {
                        player.packets.sendGameMessage("A magical force prevents you from moving.")
                        stopFollowTask(player)
                        return
                    }
                    player.calcFollow(target, if (player.run) 2 else 1, true, true)
                    return
                }
                player.resetWalkSteps()
                if (player.isOutOfRange(target, requiredDistance)) {
                    val moved = lastTargetX != target.x || lastTargetY != target.y
                    lastTargetX = target.x
                    lastTargetY = target.y
                    player.resetWalkSteps()
                    if (moved || (!player.hasWalkSteps() && player.newActionManager.getCurrentAction() != null)) {
                        player.calcFollow(target, if (player.run) 2 else 1, true, true)
                        return
                    }
                }
            }
        }
        WorldTasksManager.schedule(followTask, 0, 0)
    }

    private fun stopFollowTask(player: Player) {
        player.resetWalkSteps()
        followTask?.stop()
        followTask = null
    }

    private fun validateAttack(player: Player, target: Entity): Boolean {
        return !target.isDead && player.isActive
    }

    fun reset() {
        phase = CombatPhase.HIT
        ticksUntilNextPhase = 0
    }

    override fun stop(player: Player, interrupted: Boolean) {
        stopFollowTask(player)
        //player.resetWalkSteps();
        player.setNextFaceEntity(null)
        if (::style.isInitialized) {
            style.onStop(interrupted)
        }
    }

    override fun getPriority(): ActionPriority {
        println("[CombatAction] getPriority(): Returning COMBAT priority")
        return ActionPriority.COMBAT
    }

    private fun getAdjustedFollowDistance(target: Entity): Int {
        var baseDistance = style.getAttackDistance()
        if ((style is MagicStyle || style is RangedStyle) && target.hasWalkSteps()) {
            baseDistance = (baseDistance - 1).coerceAtLeast(0)
        }
        return baseDistance
    }

    private fun getAdjustedAttackRange(player: Player, target: Entity): Int {
        var baseDistance = style.getAttackDistance()
        if (style is MeleeStyle && target.hasWalkSteps() && player.hasWalkSteps()) {
            if (!Utils.isOnRange(player.x, player.y, player.size, target.x, target.y, target.size, baseDistance)) {
                baseDistance += 1
            }
        }
        return baseDistance
    }

    private fun isRangedWeapon(player: Player): Boolean {
        val weaponId = player.equipment.getWeaponId()
        val ranged = RangeData.getWeaponByItemId(weaponId);
        return ranged != null
    }

    private fun handleCollisionMovement(player: Player, target: Entity, size: Int): Boolean {
        if (player.isFrozen) {
            player.message("A magical force prevents you from moving.")
            return false
        }

        player.resetWalkSteps()
        return listOf(
            { player.addWalkSteps(target.x + target.size, player.y) },
            { player.addWalkSteps(target.x - size, player.y) },
            { player.addWalkSteps(player.x, target.y + target.size) },
            { player.addWalkSteps(player.x, target.y - size) }
        ).any { it() }
    }

    private fun Player.isCollidingWithTarget(target: Entity): Boolean {
        return Utils.colides(this.x, this.y, this.size, target.x, target.y, target.size) &&
                !target.hasWalkSteps()
    }

    private fun Player.isDiagonalMeleeBlocked(target: Entity, style: CombatStyle): Boolean {
        val dx = abs(this.x - target.x)
        val dy = abs(this.y - target.y)
        val attackDistance = style.getAttackDistance()

        return this.combatDefinitions.spellId <= 0 &&
                this.equipment.weaponId != 24203 &&
                target.size == 1 &&
                dx == 1 && dy == 1 &&
                attackDistance < 1
    }

    private fun shouldAdjustDiagonal(target: Player, player: Player): Boolean {
        val attackDistance = style.getAttackDistance()
        return player.size == 1 &&
                abs(target.x - player.x) == 1 &&
                abs(target.y - player.y) == 1 &&
                !player.hasWalkSteps() &&
                attackDistance < 1
    }

    private fun shouldAdjustDiagonal(player: Player, target: Entity): Boolean {
        val attackDistance = style.getAttackDistance()
        return target.size == 1 &&
                abs(player.x - target.x) == 1 &&
                abs(player.y - target.y) == 1 &&
                !target.hasWalkSteps() &&
                attackDistance < 1
    }

    private fun hasMovementPriority(player: Player, target: Entity): Boolean {
        if (target !is Player) return true // NPCs always yield to players
        return player.index < target.index
    }
}