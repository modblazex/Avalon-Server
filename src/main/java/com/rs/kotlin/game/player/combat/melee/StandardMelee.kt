package com.rs.kotlin.game.player.combat.melee

import com.rs.java.game.Animation
import com.rs.java.game.Hit
import com.rs.java.game.item.Item
import com.rs.java.game.npc.NPC
import com.rs.java.game.player.Player
import com.rs.java.game.player.Skills
import com.rs.java.game.player.TickManager
import com.rs.java.utils.Utils
import com.rs.kotlin.Rscm
import com.rs.kotlin.game.player.combat.*
import com.rs.kotlin.game.player.combat.special.*

object StandardMelee : MeleeData() {

    fun getDefaultWeapon(): MeleeWeapon = UNARMED
    fun getGoliathWeapon(): MeleeWeapon = GOLIATH_GLOVES

    private val UNARMED = MeleeWeapon(
        itemId = listOf(-1),
        name = "Unarmed",
        weaponStyle = WeaponStyle.UNARMED,
        attackSpeed = 4,
        animations = mapOf(
            StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.punch"),
            StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.kick"),
            StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.punch"),
        )
    )
    private val GOLIATH_GLOVES = MeleeWeapon(
        itemId = listOf(-2),
        name = "Goliath gloves",//anims 14307 && 14393 && effect = 14417
        weaponStyle = WeaponStyle.UNARMED,
        attackSpeed = 4,
        animations = mapOf(
            StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.goliath_gloves_punch"),
            StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.goliath_gloves_uppcut"),
            StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.goliath_gloves_punch"),
        ),
        effect = SpecialEffect(
            execute = { context ->
                if (Utils.roll(1, 3)) {
                    context.defender.addFreezeDelay(16, false);
                    context.defender.gfx("graphic.entangle", 100)
                    context.forcedHit(delay = 1)
                } else {
                    context.meleeHit()
                }
                true
            }
        )
    )
    override val weapons = listOf(
        MeleeWeapon(
            itemId = Weapon.itemIds("item.dragon_claws", "item.dragon_claws_2", "item.lucky_dragon_claws"),
            name = "Dragon claws",
            weaponStyle = WeaponStyle.CLAWS,
            blockAnimationId = Animation.getId("animation.claws_block"),
            soundId = Rscm.lookup("sound.claw_attack"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.claws_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.claws_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.claws_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.claws_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                execute = { context ->
                    context.attacker.animate("animation.dragon_claws_special")
                    context.attacker.gfx("graphic.dragon_claws_special")

                    listOf(15, 30, 35, 45).forEach { delay ->
                        context.attacker.playSound("sound.claw_attack", delay, 1)
                    }

                    val dragonClawsHits = context.getDragonClawsHits(4)
                    val firstHitIndex = dragonClawsHits.indexOfFirst { it.damage > 9 }
                    context.hits {
                        dragonClawsHits.forEachIndexed { i, hit ->
                            val delay = if (firstHitIndex == -1) 0 else i / 2
                            addHit(hit = hit.copy(), delay = delay)
                        }
                    }
                }
            )
        ),
        MeleeWeapon(//player.animate(new Animation(10502));
            itemId = Weapon.itemIds("item.vesta_s_longsword", "item.vesta_s_longsword_deg"),
            name = "Vesta's longsword",
            weaponStyle = WeaponStyle.SCIMITAR,
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.sword_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.sword_slash"),
            ),
            blockAnimationId = Animation.getId("animation.scimitar_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.longsword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.longsword_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.longsword_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.longsword_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 25,
                accuracyMultiplier = 1.75,
                execute = { context ->
                    context.attacker.animate("animation.vestas_longsword_special")
                    context.attacker.playSound("sound.dragon_longsword_special", 1)
                    val maxHit = CombatCalculations.calculateMeleeMaxHit(context.attacker, context.defender).maxHit
                    val roll = context.rollMelee();
                    val damage = ((0.2 * maxHit).toInt()..(1.2 * maxHit).toInt()).random()
                    context.hits {
                        val hit = Hit(context.attacker, damage, Hit.HitLook.MELEE_DAMAGE);
                        if (roll.damage > 0) {
                            addHit(context.defender, hit)
                        } else {
                            addHit(context.defender, Hit(context.attacker, 0, Hit.HitLook.MELEE_DAMAGE));
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Weapon.itemIds("item.korasi_sword"),
            name = "Korasi's sword",
            weaponStyle = WeaponStyle.SCIMITAR,
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.sword_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.sword_slash"),
            ),
            blockAnimationId = Animation.getId("animation.scimitar_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.scimitar_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.scimitar_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 60,
                execute = { context ->
                    context.attacker.animate("animation.korasi_sword_special")
                    context.attacker.gfx("graphic.korasi_special_attack_start")
                    context.attacker.playSound("sound.saradomin_sword_special", 1)
                    context.attacker.playSound("sound.armadyl_godsword_special", 1)
                    val maxHit = CombatCalculations.calculateMeleeMaxHit(context.attacker, context.defender).maxHit

                    val isMultiCombat = context.defender.isAtMultiArea

                    val firstHitDamage = if (isMultiCombat) {
                        (0..((1.5 * maxHit).toInt())).random()
                    } else {
                        ((0.5 * maxHit).toInt()..(1.5 * maxHit).toInt()).random()
                    }

                    val firstHit = Hit(context.attacker, firstHitDamage, Hit.HitLook.MAGIC_DAMAGE)
                    if (firstHit.checkCritical(firstHitDamage, maxHit))
                        firstHit.critical = true
                    context.hits {
                        addHit(context.defender, firstHit)
                        context.defender.gfx("graphic.korasi_special_attack_end")
                        if (isMultiCombat) {
                            val extraTargets = context.getMultiAttackTargets(maxDistance = 1, maxTargets = 2)
                            val damages = listOf(firstHitDamage / 2, firstHitDamage / 4)

                            for ((index, target) in extraTargets.withIndex()) {
                                if (index >= damages.size) break
                                target.gfx("graphic.korasi_special_attack_end")
                                addHit(target, firstHit.copyWithDamage(damages[index]), delay = 0)
                            }
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Weapon.itemIds(
                "item.abyssal_whip", "item.abyssal_whip_2",
                "item.abyssal_whip_3", "item.abyssal_whip_4",
                "item.abyssal_whip_5", "item.abyssal_whip_6",
                "item.abyssal_whip_7", "item.abyssal_whip_8",
                "item.abyssal_vine_whip", "item.abyssal_vine_whip_2",
                "item.abyssal_vine_whip_3", "item.abyssal_vine_whip_4",
                "item.abyssal_vine_whip_5"
            ),
            name = "Abyssal whip",
            weaponStyle = WeaponStyle.WHIP,
            blockAnimationId = Animation.getId("animation.abyssal_whip_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.abyssal_whip_attack"),
                StyleKey(AttackStyle.CONTROLLED, 1) to Animation.getId("animation.abyssal_whip_attack2"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.abyssal_whip_attack3"),
            ),
            soundId = Rscm.lookup("sound.whip_attack"),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 1.25,
                execute = { context ->  //TODO USING WHIP AS A TEST WEAPON ATM
                    context.attacker.animate("animation.abyssal_whip_special")
                    context.defender.gfx("graphic.abyssal_whip_special", 100)
                    context.attacker.playSound("sound.whip_special", 1)
                    //TODO GET SOUND
                    val hit = context.meleeHit()
                    if (hit[0].damage > 0) {
                        if (context.defender is Player) {
                            context.attacker.transferRunEnergy(context.defender)
                            context.defender.message("You feel drained!")
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Weapon.itemIds("item.dragon_scimitar"),
            name = "Dragon scimitar",
            weaponStyle = WeaponStyle.SCIMITAR,
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.scimitar_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.scimitar_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.scimitar_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.scimitar_slash"),
            ),
            blockAnimationId = Animation.getId("animation.scimitar_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.scimitar_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.scimitar_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 55,
                accuracyMultiplier = 1.25,
                execute = { context ->
                    context.attacker.animate("animation.dragon_scimitar_special")
                    context.defender.gfx("graphic.dragon_scimitar_special", 100)
                    context.attacker.playSound("sound.dragon_scimitar_special", 1)
                    val hit = context.meleeHit()
                    if (hit[0].damage > 0) {
                        if (context.defender is Player) {
                            context.defender.prayer.closeProtectionPrayers()
                            context.defender.tickManager.addTicks(
                                TickManager.TickKeys.DISABLED_PROTECTION_PRAYER_TICK,
                                8
                            )
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.dragon_dagger", "item.dragon_dagger_p",
                "item.dragon_dagger_p+", "item.dragon_dagger_p++"
            ),
            name = "Dragon dagger",
            weaponStyle = WeaponStyle.DAGGER,
            attackSpeed = 4,
            blockAnimationId = Animation.getId("animation.dragon_dagger_block"),
            soundId = Rscm.lookup("sound.dagger_stab"),
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.dagger_stab"),
            ),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.dragon_dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.dragon_dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.dragon_dagger_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.dragon_dagger_stab"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 25,
                accuracyMultiplier = 1.15,
                damageMultiplier = 1.15,
                execute = { context ->
                    context.attacker.animate("animation.dragon_dagger_special")
                    context.attacker.gfx("graphic.dragon_dagger_special", 100)
                    context.attacker.playSound("sound.dragon_dagger_special", 0, 1)
                    context.attacker.playSound("sound.dragon_dagger_special", 15, 1)
                    context.meleeHit()
                    context.meleeHit(delay = if (context.defender is NPC) 1 else 0)
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_mace", "item.iron_mace",
                "item.steel_mace", "item.black_mace",
                "item.mithril_mace", "item.adamant_mace",
                "item.rune_mace", "item.void_knight_mace",
            ),
            name = "Maces",
            weaponStyle = WeaponStyle.MACE,
            blockAnimationId = Animation.getId("animation.mace_block"),
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.mace_crush"),
            ),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.mace_crush"),
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.dragon_mace"
            ),
            name = "Dragon mace",
            weaponStyle = WeaponStyle.MACE,
            blockAnimationId = Animation.getId("animation.mace_block"),
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.mace_crush"),
            ),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.mace_crush"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 25,
                accuracyMultiplier = 1.25,
                damageMultiplier = 1.50,
                execute = { context ->
                    context.attacker.animate("animation.dragon_mace_special")
                    context.attacker.gfx("graphic.dragon_mace_special", 100)
                    context.attacker.playSound("sound.dragon_mace_special", 1)
                    context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.annihilation"
            ),
            attackSpeed = 5,
            name = "Annihilation",
            weaponStyle = WeaponStyle.MACE,
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Rscm.lookup("sound.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Rscm.lookup("sound.mace_crush"),
            ),
            blockAnimationId = Animation.getId("animation.mace_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.mace_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.mace_crush"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 1.50,
                damageMultiplier = 1.20,
                execute = { context ->
                    context.attacker.animate("animation.dragon_mace_special")
                    context.attacker.gfx("graphic.dragon_mace_special", 100)
                    context.attacker.playSound("sound.dragon_mace_special", 1)
                    context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.granite_maul"
            ),
            name = "Granite maul",
            weaponStyle = WeaponStyle.HAMMER,
            blockAnimationId = Animation.getId("animation.granite_maul_block"),
            soundId = Rscm.lookup("sound.granite_maul_attack"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.granite_maul_attack"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.granite_maul_attack"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.granite_maul_attack"),
            ),
            special = SpecialAttack.InstantCombat(
                energyCost = 50,
                execute = { context ->
                    context.attacker.animate("animation.granite_maul_special_attack")
                    context.attacker.gfx("graphic.granite_maul_special", 100)
                    context.attacker.playSound("sound.granite_maul_special", 1)
                    context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.torag_s_hammers", "item.torag_s_hammers_100",
                "item.torag_s_hammers_75", "item.torag_s_hammers_50",
                "item.torag_s_hammers_25", "item.torag_s_hammers_0"
            ),
            name = "Torag's hammers",
            weaponStyle = WeaponStyle.HAMMER,
            effect = SpecialEffect(
                execute = { context ->
                    context.attacker.animate(Animation("animation.torag_hammer_attack"))
                    context.attacker.playSound("sound.hammer", 1)
                    context.attacker.playSound("sound.hammer", 10, 1)

                    val maxHit = CombatCalculations.calculateMeleeMaxHit(context.attacker, context.defender).maxHit
                    val maxHit1 = (maxHit + 1) / 2
                    val maxHit2 = (maxHit / 2)
                    val firstHit = context.rollMelee()
                    val secondHit = context.rollMelee()
                    context.hits {
                        nextHit(baseHit = firstHit, maxHit = maxHit1)
                        nextHit(baseHit = secondHit, maxHit = maxHit2)
                    }
                    true
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.chaotic_staff", "item.chaotic_staff_broken"),
            name = "Chaotic staff",
            weaponStyle = WeaponStyle.STAFF,
            blockAnimationId = Animation.getId("animation.staff_of_light_block"),
            soundId = Rscm.lookup("sound.hammer"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.mace_crush"),
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.obliteration"),
            name = "Obliteration",
            weaponStyle = WeaponStyle.STAFF,
            blockAnimationId = Animation.getId("animation.staff_of_light_block"),
            soundId = Rscm.lookup("sound.hammer"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.mace_crush"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 100,
                execute = { context ->
                    context.attacker.animate(10518)
                    context.attacker.gfx(1853)
                    context.hits {
                        val magicHit = magic(
                            spellId = 39,
                            delay = context.combat.getHitDelay(),
                            damageMultiplier = 1.5,
                            accuracyMultiplier = 1.5
                        )

                    }

                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.volatile_nightmare_staff"),
            name = "Volatile",
            weaponStyle = WeaponStyle.STAFF,
            blockAnimationId = Animation.getId("animation.staff_block"),
            soundId = Rscm.lookup("sound.staff_bash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.staff_bash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.staff_bash"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.staff_bash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 100,
                execute = { context ->
                    context.attacker.animate(10518)
                    context.attacker.gfx(1853)
                    context.hits {
                        val magicHit = magic(
                            baseDamage = 39,
                            delay = context.combat.getHitDelay(),
                            damageMultiplier = 1.5,
                            accuracyMultiplier = 1.5
                        )

                    }

                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.ancient_staff",
                "item.ancient_staff_2",
                "item.ancient_staff_3",
                "item.ancient_staff_4",
                "item.ancient_staff_5",
                "item.ancient_staff_6",
                "item.ancient_staff_7",
                "item.ancient_staff_8",
                "item.ancient_staff_9",
                "item.ancient_staff_10",
                "item.staff",
                "item.staff_of_air",
                "item.staff_of_water",
                "item.staff_of_earth",
                "item.staff_of_fire",
                "item.battlestaff",
                "item.air_battlestaff",
                "item.water_battlestaff",
                "item.earth_battlestaff",
                "item.fire_battlestaff",
                "item.lava_battlestaff",
                "item.mud_battlestaff",
                "item.mystic_air_staff",
                "item.mystic_water_staff",
                "item.mystic_earth_staff",
                "item.mystic_fire_staff",
                "item.mystic_lava_staff",
                "item.mystic_mud_staff",
                "item.staff_of_armadyl",
                "item.ahrim_s_staff",
                "item.ahrim_s_staff_100",
                "item.ahrim_s_staff_75",
                "item.ahrim_s_staff_50",
                "item.ahrim_s_staff_25",
                "item.ahrim_s_staff_0",
                "item.gravite_staff",
                "item.zuriel_s_staff",
                "item.zuriel_s_staff_deg",
                "item.corrupt_zuriel_s_staff",
                "item.corrupt_zuriel_s_staff_deg",
                "item.staff_of_armadyl",
                "item.armadyl_battlestaff",
                "item.beginner_wand",
                "item.apprentice_wand",
                "item.master_wand",
                "item.kodai_wand",
                "item.iban_s_staff",
                "item.slayer_s_staff",
                "item.guthix_staff",
                "item.zamorak_staff",
                "item.saradomin_staff",
                "item.necromancer_s_air_staff",
                "item.necromancer_s_water_staff",
                "item.necromancer_s_earth_staff",
                "item.necromancer_s_fire_staff",
                "item.necromancer_s_mud_staff",
                "item.necromancer_s_lava_staff",
                "item.necromancer_s_steam_staff",
                "item.skeletal_staff_of_air",
                "item.skeletal_staff_of_water",
                "item.skeletal_staff_of_earth",
                "item.skeletal_staff_of_fire",
                "item.skeletal_battlestaff_of_air",
                "item.skeletal_battlestaff_of_water",
                "item.skeletal_battlestaff_of_earth",
                "item.skeletal_battlestaff_of_fire",
                "item.skeletal_lava_battlestaff",
                "item.skeletal_mud_battlestaff",
                "item.skeletal_steam_battlestaff",
                "item.lunar_staff"
            ),
            name = "Staff & Wand",
            weaponStyle = WeaponStyle.STAFF,
            blockAnimationId = Animation.getId("animation.staff_block"),
            soundId = Rscm.lookup("sound.staff_bash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.staff_bash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.staff_bash"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.staff_bash"),
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.staff_of_light", "item.staff_of_light_lended",
                "item.staff_of_light_red", "item.staff_of_light_gold",
                "item.staff_of_light_blue", "item.staff_of_light_green"
            ),
            name = "Staff of light",
            weaponStyle = WeaponStyle.STAFF_OF_LIGHT,
            blockAnimationId = Animation.getId("animation.staff_of_light_block"),
            sounds = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Rscm.lookup("sound.sword_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Rscm.lookup("sound.sword_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Rscm.lookup("sound.staff_bash"),
            ),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.staff_of_light_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.staff_of_light_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.staff_crush"),
            ),
            special = SpecialAttack.Instant(
                energyCost = 100,
                execute = { attacker ->
                    attacker.animate("animation.staff_of_light_special")
                    attacker.gfx("graphic.staff_of_light_special")
                    attacker.tickManager.addMinutes(TickManager.TickKeys.STAFF_OF_LIGHT_EFFECT, 1) {
                        attacker.message("The power of the light fades. Your resistance to melee attacks return to normal.")
                    }

                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.bronze_halberd", "item.iron_halberd",
                "item.steel_halberd", "item.black_halberd",
                "item.mithril_halberd", "item.adamant_halberd",
                "item.rune_halberd", "item.noxious_halberd"),
            name = "Halberd",
            weaponStyle = WeaponStyle.HALBERD,
            attackRange = 1,
            blockAnimationId = Animation.getId("animation.halberd_block"),
            animations = mapOf(
                StyleKey(AttackStyle.CONTROLLED, 0) to Animation.getId("animation.halberd_jab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.halberd_swipe"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.halberd_fend"),
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.dragon_halberd"),
            name = "Dragon halberd",
            weaponStyle = WeaponStyle.HALBERD,
            attackRange = 1,
            blockAnimationId = Animation.getId("animation.halberd_block"),
            animations = mapOf(
                StyleKey(AttackStyle.CONTROLLED, 0) to Animation.getId("animation.halberd_jab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.halberd_swipe"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.halberd_fend"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 30,
                accuracyMultiplier = 1.1,
                damageMultiplier = 1.1,
                execute = { context ->
                    context.attacker.animate("animation.dragon_halberd_special")
                    context.attacker.playSound("sound.dragon_halberd_special", 1)
                    context.meleeHit()
                    if (context.defender.size > 1)
                        context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = listOf(
                Rscm.lookup("item.saradomin_sword"),
                Rscm.lookup("item.saradomin_sword_2"),
                Rscm.lookup("item.lucky_saradomin_sword")
            ),
            name = "Saradomin sword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            blockAnimationId = Animation.getId("animation.two_handed_defend"),
            soundId = Rscm.lookup("sound.godsword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.two_handed_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.two_handed_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.two_handed_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.two_handed_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 100,
                damageMultiplier = 1.1,
                execute = { context ->
                    context.attacker.animate("animation.saradomin_sword_special")
                    context.attacker.gfx("graphic.saradomin_sword_special_start")
                    context.attacker.playSound("sound.saradomin_sword_special", 1)
                    context.hits {
                        val meleeHit = melee(delay = 0)
                        context.defender.gfx("graphic.saradomin_sword_special_end")
                        var randomHit = if (meleeHit.damage > 0) (50..150).random() else 0
                        if (context.defender is Player) {
                            if (context.defender.prayer.isMageProtecting) {
                                randomHit = 0
                            }
                        }
                        val magicHit = Hit(context.attacker, randomHit, Hit.HitLook.MAGIC_DAMAGE)
                        magicHit.critical = true
                        addHit(context.defender, magicHit)
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = listOf(
                Rscm.lookup("item.armadyl_godsword"),
                Rscm.lookup("item.armadyl_godsword_2"),
                Rscm.lookup("item.lucky_armadyl_godsword")
            ),
            name = "Armadyl godsword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            soundId = Rscm.lookup("sound.godsword_slash"),
            blockAnimationId = Animation.getId("animation.godsword_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.godsword_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.godsword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.godsword_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.godsword_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.375,
                execute = { context ->
                    context.attacker.animate("animation.armadyl_godsword_special")
                    context.attacker.gfx("graphic.armadyl_godsword_special")
                    context.attacker.playSound("sound.armadyl_godsword_special", 1)
                    context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = listOf(
                Rscm.lookup("item.saradomin_godsword"),
                Rscm.lookup("item.saradomin_godsword_2"),
                Rscm.lookup("item.lucky_saradomin_godsword")
            ),
            name = "Saradomin godsword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            soundId = Rscm.lookup("sound.godsword_slash"),
            blockAnimationId = Animation.getId("animation.godsword_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.godsword_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.godsword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.godsword_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.godsword_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.10,
                execute = { context ->
                    context.attacker.animate("animation.saradomin_godsword_special")
                    context.attacker.gfx("graphic.saradomin_godsword_special")
                    context.hits {
                        val hit = melee()

                        if (hit.damage > 0) {
                            val potentialDmg = hit.damage

                            val hpRestore: Int
                            val prayerRestore: Int

                            if (potentialDmg < 22) {
                                hpRestore = 10
                                prayerRestore = 5
                            } else {
                                hpRestore = ((potentialDmg * 0.5).toInt() + 1) and -2
                                prayerRestore = ((potentialDmg * 0.25).toInt() + 1) and -2
                            }

                            val player = context.attacker
                            player.heal(hpRestore)
                            player.prayer.restorePrayer(prayerRestore)
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bandos_godsword", "item.bandos_godsword_2", "item.lucky_bandos_godsword",
            ),
            name = "Bandos godsword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            soundId = Rscm.lookup("sound.godsword_slash"),
            blockAnimationId = Animation.getId("animation.godsword_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.godsword_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.godsword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.godsword_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.godsword_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.21,
                execute = { context ->
                    context.attacker.animate("animation.bandos_godsword_special")
                    context.attacker.gfx("graphic.bandos_godsword_special")
                    context.hits {
                        val hit = melee()
                        val defender = context.defender
                        if (hit.damage > 0) {
                            val drainOrderPlayers = listOf(
                                Skills.DEFENCE, Skills.STRENGTH,
                                Skills.ATTACK, Skills.MAGIC, Skills.RANGE
                            )
                            val drainOrderNpcs = listOf(
                                "defence", "strength", "prayer",
                                "attack", "magic", "ranged"
                            )

                            var remainingDrain = hit.damage / 10

                            if (defender is Player) {
                                for (skill in drainOrderPlayers) {
                                    if (remainingDrain <= 0) break
                                    val current = defender.skills.getLevel(skill)
                                    if (current <= 0) continue

                                    val drainAmount = minOf(current, remainingDrain)
                                    defender.skills.drainLevel(skill, drainAmount)
                                    remainingDrain -= drainAmount
                                }
                            } else if (defender is NPC) {
                                for (skill in drainOrderNpcs) {
                                    if (remainingDrain <= 0) break
                                    val current = defender.combatData.getCurrentStat(skill)
                                    if (current <= 0) continue

                                    val drainAmount = minOf(current, remainingDrain)
                                    defender.combatData.drain(skill, drainAmount)
                                    remainingDrain -= drainAmount
                                }
                            }
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = listOf(
                Rscm.lookup("item.zamorak_godsword"),
                Rscm.lookup("item.zamorak_godsword_2"),
                Rscm.lookup("item.lucky_zamorak_godsword")
            ),
            name = "Zamorak godsword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            soundId = Rscm.lookup("sound.godsword_slash"),
            blockAnimationId = Animation.getId("animation.godsword_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.godsword_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.godsword_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.godsword_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.godsword_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.1,
                execute = { context ->
                    context.attacker.animate("animation.zamorak_godsword_special")
                    context.attacker.gfx("graphic.zamorak_godsword_start")
                    context.hits {
                        val hit = melee()
                        if (hit.damage > 0) {
                            context.defender.gfx("graphic.zamorak_godsword_target")
                            context.defender.tickManager.addTicks(TickManager.TickKeys.FREEZE_IMMUNE_TICKS, 37)
                            context.defender.tickManager.addTicks(TickManager.TickKeys.FREEZE_TICKS, 32) {
                                if (context.defender is Player)
                                    context.defender.message("Your staff of light effect fades.")
                                context.defender.gfx("graphic.zamorak_godsword_end")
                            }
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.dharok_s_greataxe", "item.dharok_s_greataxe_100",
                "item.dharok_s_greataxe_75", "item.dharok_s_greataxe_50",
                "item.dharok_s_greataxe_25", "item.dharok_s_greataxe_0"
            ),
            name = "Dharok's greataxe",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            blockAnimationId = Animation.getId("animation.dharok_greataxe_block"),

            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.dharok_greataxe_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.dharok_greataxe_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.dharok_greataxe_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.dharok_greataxe_slash"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_scimitar", "item.iron_scimitar",
                "item.steel_scimitar", "item.black_scimitar",
                "item.mithril_scimitar", "item.adamant_scimitar",
                "item.rune_scimitar", "item.corrupt_dragon_scimitar",
                "item.c_dragon_scimitar_deg", "item.sacred_clay_scimitar"
            ),
            name = "Scimitar",
            weaponStyle = WeaponStyle.SCIMITAR,
            blockAnimationId = Animation.getId("animation.scimitar_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.scimitar_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.scimitar_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.scimitar_slash"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.golden_katana", "item.ornate_katana",
                "item.auspicious_katana"
            ),
            name = "Katana",
            weaponStyle = WeaponStyle.KATANA,
            blockAnimationId = Animation.getId("animation.katana_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.katana_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.katana_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.katana_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.katana_slash"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_spear", "item.iron_spear",
                "item.steel_spear", "item.black_spear",
                "item.mithril_spear", "item.adamant_spear",
                "item.rune_spear", "item.corrupt_dragon_spear",
                "item.novite_spear", "item.novite_spear_b",
                "item.bathus_spear", "item.bathus_spear_b",
                "item.marmaros_spear", "item.marmaros_spear_b",
                "item.kratonite_spear", "item.kratonite_spear_b",
                "item.fractite_spear", "item.fractite_spear_b",
                "item.zephyrium_spear", "item.zephyrium_spear_b",
                "item.argonite_spear", "item.argonite_spear_b",
                "item.katagon_spear", "item.katagon_spear_b",
                "item.gorgonite_spear", "item.gorgonite_spear_b",
                "item.promethium_spear", "item.promethium_spear_b",
                "item.primal_spear", "item.primal_spear_b"
            ),
            name = "Spear",
            weaponStyle = WeaponStyle.SPEAR,
            blockAnimationId = Animation.getId("animation.new_spear_block"),
            animations = mapOf(
                StyleKey(AttackStyle.CONTROLLED, 0) to Animation.getId("animation.new_spear_stab"),
                StyleKey(AttackStyle.CONTROLLED, 1) to Animation.getId("animation.new_spear_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.new_spear_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.new_spear_stab"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.zamorakian_spear", "item.dragon_spear"
            ),
            name = "Dragon & zamorakian spear",
            weaponStyle = WeaponStyle.SPEAR,
            blockAnimationId = Animation.getId("animation.new_spear_block"),
            animations = mapOf(
                StyleKey(AttackStyle.CONTROLLED, 0) to Animation.getId("animation.new_spear_stab"),
                StyleKey(AttackStyle.CONTROLLED, 1) to Animation.getId("animation.new_spear_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.new_spear_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.new_spear_stab"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 25,
                execute = { context ->
                    val defender = context.defender
                    val attacker = context.attacker
                    attacker.animate("animation.dragon_spear_special")
                    defender.gfx("graphic.stun")
                    defender.resetWalkSteps();
                    val dx = defender.x - attacker.x
                    val dy = defender.y - attacker.y

                    val pushX = dx.coerceIn(-1, 1)
                    val pushY = dy.coerceIn(-1, 1)

                    val destX = defender.x + pushX
                    val destY = defender.y + pushY

                    if (defender is Player) {
                        defender.stopAll()
                    }
                    var moved = defender.addWalkSteps(destX, destY, 1)
                    if (!moved) {
                        val swapX = attacker.x
                        val swapY = attacker.y
                        moved = defender.addWalkSteps(swapX, swapY, 1)
                    }
                    defender.lock(5)
                    attacker.setNextFaceEntity(defender)
                    defender.setNextFaceEntity(null)
                    defender.direction = attacker.direction

                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_sword", "item.iron_sword",
                "item.steel_sword", "item.black_sword",
                "item.mithril_sword", "item.adamant_sword",
                "item.rune_sword",
            ),
            name = "Dagger",
            weaponStyle = WeaponStyle.DAGGER,
            blockAnimationId = Animation.getId("animation.sword_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.dagger_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.dagger_stab"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.dragon_hunter_lance",
            ),
            name = "Lance",
            weaponStyle = WeaponStyle.SPEAR,
            blockAnimationId = Animation.getId("animation.battleaxe_block"),
            soundId = Rscm.lookup("sound.sword_stab"),
            animations = mapOf(
                StyleKey(AttackStyle.CONTROLLED, 0) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.CONTROLLED, 1) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.staff_bash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_stab"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_dagger", "item.iron_dagger",
                "item.steel_dagger", "item.black_dagger",
                "item.mithril_dagger", "item.adamant_dagger",
                "item.rune_dagger", "item.corrupt_dragon_dagger",
                "item.c_dragon_dagger_deg",
                "item.novite_dagger", "item.novite_dagger_b",
                "item.bathus_dagger", "item.bathus_dagger_b",
                "item.marmaros_dagger", "item.marmaros_dagger_b",
                "item.kratonite_dagger", "item.kratonite_dagger_b",
                "item.fractite_dagger", "item.fractite_dagger_b",
                "item.zephyrium_dagger", "item.zephyrium_dagger_b",
                "item.argonite_dagger", "item.argonite_dagger_b",
                "item.hailstorm_dagger", "item.hailstorm_dagger_b",
                "item.katagon_dagger", "item.katagon_dagger_b",
                "item.gorgonite_dagger", "item.gorgonite_dagger_b",
                "item.promethium_dagger", "item.promethium_dagger_b",
                "item.primal_dagger", "item.primal_dagger_b",
            ),
            name = "Dagger",
            weaponStyle = WeaponStyle.DAGGER,
            blockAnimationId = Animation.getId("animation.dagger_block"),
            soundId = Rscm.lookup("sound.dagger_stab"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.dagger_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.dagger_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.dagger_stab"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_battleaxe", "item.iron_battleaxe",
                "item.steel_battleaxe", "item.black_battleaxe",
                "item.mithril_battleaxe", "item.adamant_battleaxe",
                "item.rune_battleaxe", "item.corrupt_dragon_battleaxe",
                "item.c_dragon_battleaxe_deg",
                "item.novite_battleaxe", "item.novite_battleaxe_b",
                "item.bathus_battleaxe", "item.bathus_battleaxe_b",
                "item.marmaros_battleaxe", "item.marmaros_battleaxe_b",
                "item.kratonite_battleaxe", "item.kratonite_battleaxe_b",
                "item.fractite_battleaxe", "item.fractite_battleaxe_b",
                "item.zephyrium_battleaxe", "item.zephyrium_battleaxe_b",
                "item.argonite_battleaxe", "item.argonite_battleaxe_b",
                "item.katagon_battleaxe", "item.katagon_battleaxe_b",
                "item.gorgonite_battleaxe", "item.gorgonite_battleaxe_b",
                "item.promethium_battleaxe", "item.promethium_battleaxe_b",
                "item.primal_battleaxe", "item.primal_battleaxe_b",
            ),
            name = "Battleaxe",
            weaponStyle = WeaponStyle.BATTLEAXE,
            blockAnimationId = Animation.getId("animation.battleaxe_block"),
            soundId = Rscm.lookup("sound.battleaxe"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.battleaxe_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.battleaxe_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.battleaxe_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.battleaxe_slash"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_warhammer", "item.iron_warhammer",
                "item.steel_warhammer", "item.black_warhammer",
                "item.mithril_warhammer", "item.adamant_warhammer",
                "item.rune_warhammer", "item.corrupt_statius_s_warhammer", "item.c_statius_s_warhammer_deg",
                "item.novite_warhammer", "item.novite_warhammer_b",
                "item.bathus_warhammer", "item.bathus_warhammer_b",
                "item.marmaros_warhammer", "item.marmaros_warhammer_b",
                "item.kratonite_warhammer", "item.kratonite_warhammer_b",
                "item.fractite_warhammer", "item.fractite_warhammer_b",
                "item.zephyrium_warhammer", "item.zephyrium_warhammer_b",
                "item.argonite_warhammer", "item.argonite_warhammer_b",
                "item.katagon_warhammer", "item.katagon_warhammer_b",
                "item.gorgonite_warhammer", "item.gorgonite_warhammer_b",
                "item.promethium_warhammer", "item.promethium_warhammer_b",
                "item.primal_warhammer", "item.primal_warhammer_b",
            ),
            name = "Warhammer",
            weaponStyle = WeaponStyle.HAMMER,
            blockAnimationId = Animation.getId("animation.warhammer_block"),
            soundId = Rscm.lookup("sound.hammer"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.warhammer_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.warhammer_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.warhammer_crush"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.darklight",
            ),
            name = "Longsword",
            weaponStyle = WeaponStyle.SCIMITAR,
            blockAnimationId = Animation.getId("animation.chaotic_rapier_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.0,
                execute = { context ->
                    context.attacker.animate("animation.darklight_special")
                    context.attacker.gfx("graphic.darklight_special")
                    context.attacker.playSound("sound.sword_slash", 1)
                    context.hits {
                        val hit = melee()
                        if (hit.damage > 0) {
                            val defender = context.defender
                            if (defender is NPC) {
                                val isDemon = defender.name.contains("demon", ignoreCase = true)
                                val drainPercent = if (isDemon) 0.10 else 0.05

                                listOf("attack", "strength", "defence").forEach { skill ->
                                    val baseLevel = defender.combatData.getBaseStat(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.combatData.drain(skill, drainAmount)
                                }
                            } else if (defender is Player) {
                                val drainPercent = 0.05
                                listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE).forEach { skill ->
                                    val baseLevel = defender.skills.getLevelForXp(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.skills.drainLevel(skill, drainAmount)
                                }
                            }
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.arclight", "item.emberlight",
            ),
            name = "Longsword",
            weaponStyle = WeaponStyle.SCIMITAR,
            blockAnimationId = Animation.getId("animation.chaotic_rapier_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 2.0,
                damageMultiplier = 1.0,
                execute = { context ->
                    context.attacker.animate("animation.darklight_special")
                    context.attacker.gfx("graphic.darklight_special")
                    context.attacker.playSound("sound.sword_slash", 1)
                    context.hits {
                        val hit = melee()
                        if (hit.damage > 0) {
                            val defender = context.defender
                            if (defender is NPC) {
                                val isDemon = defender.name.contains("demon", ignoreCase = true)
                                val drainPercent = if (isDemon) 0.15 else 0.05

                                listOf("attack", "strength", "defence").forEach { skill ->
                                    val baseLevel = defender.combatData.getBaseStat(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.combatData.drain(skill, drainAmount)
                                }
                            } else if (defender is Player) {
                                val drainPercent = 0.05
                                listOf(Skills.ATTACK, Skills.STRENGTH, Skills.DEFENCE).forEach { skill ->
                                    val baseLevel = defender.skills.getLevelForXp(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.skills.drainLevel(skill, drainAmount)
                                }
                            }
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.statius_s_warhammer", "item.statius_warhammer_deg",
            ),
            name = "Statius' Warhammer",
            weaponStyle = WeaponStyle.HAMMER,
            blockAnimationId = Animation.getId("animation.mace_block"),
            soundId = Rscm.lookup("sound.hammer"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.mace_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.mace_crush"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 50,
                accuracyMultiplier = 1.0,
                damageMultiplier = 1.0,
                execute = { context ->
                    context.attacker.animate("animation.statius_warhammer_special")
                    context.attacker.gfx("graphic.statius_warhammer_special")
                    context.attacker.playSound("sound.hammer", 1)
                    val maxHit = CombatCalculations.calculateMeleeMaxHit(context.attacker, context.defender).maxHit
                    val roll = context.rollMelee();
                    val damage = ((0.25 * maxHit).toInt()..(1.25 * maxHit).toInt()).random()
                    context.hits {
                        val hit = Hit(context.attacker, damage, Hit.HitLook.MELEE_DAMAGE);
                        if (roll.damage > 0) {
                            val defender = context.defender
                            addHit(context.defender, hit)
                            if (defender is NPC) {
                                val drainPercent = 0.30

                                listOf("defence").forEach { skill ->
                                    val baseLevel = defender.combatData.getBaseStat(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.combatData.drain(skill, drainAmount)
                                }
                            } else if (defender is Player) {
                                val drainPercent = 0.30
                                listOf(Skills.DEFENCE).forEach { skill ->
                                    val baseLevel = defender.skills.getLevelForXp(skill)
                                    val drainAmount = (baseLevel * drainPercent).toInt() + 1
                                    defender.skills.drainLevel(skill, drainAmount)
                                }
                            }
                        } else {
                            addHit(context.defender, Hit(context.attacker, 0, Hit.HitLook.MELEE_DAMAGE));
                        }
                    }
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_longsword",
                "item.iron_longsword",
                "item.steel_longsword",
                "item.black_longsword",
                "item.mithril_longsword",
                "item.adamant_longsword",
                "item.rune_longsword",
                "item.corrupt_dragon_longsword",
                "item.c_dragon_longsword_deg",
                "item.corrupt_vesta_s_longsword",
                "item.c_vesta_s_longsword_deg",
                "item.chaotic_longsword",
                "item.chaotic_longsword_broken",

                "item.novite_longsword", "item.novite_longsword_b",
                "item.bathus_longsword", "item.bathus_longsword_b",
                "item.marmaros_longsword", "item.marmaros_longsword_b",
                "item.kratonite_longsword", "item.kratonite_longsword_b",
                "item.fractite_longsword", "item.fractite_longsword_b",
                "item.zephyrium_longsword", "item.zephyrium_longsword_b",
                "item.argonite_longsword", "item.argonite_longsword_b",
                "item.katagon_longsword", "item.katagon_longsword_b",
                "item.gorgonite_longsword", "item.gorgonite_longsword_b",
                "item.promethium_longsword", "item.promethium_longsword_b",
                "item.primal_longsword", "item.primal_longsword_b", "item.silverlight",
            ),
            name = "Longsword",
            weaponStyle = WeaponStyle.SCIMITAR,
            blockAnimationId = Animation.getId("animation.chaotic_rapier_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_slash"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.dragon_longsword", "item.dragon_longsword_2",
            ),
            name = "Longsword",
            weaponStyle = WeaponStyle.SCIMITAR,
            blockAnimationId = Animation.getId("animation.chaotic_rapier_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_slash"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 25,
                accuracyMultiplier = 1.25,
                damageMultiplier = 1.25,
                execute = { context ->
                    context.attacker.animate("animation.dragon_longsword_special")
                    context.attacker.gfx("graphic.dragon_longsword_special", 100)
                    //context.attacker.playSound("sound.dragon_longsword_special", 1)
                    context.meleeHit()
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.chaotic_rapier", "item.chaotic_rapier_broken", "item.brackish_blade",

                "item.novite_rapier", "item.novite_rapier_b",
                "item.bathus_rapier", "item.bathus_rapier_b",
                "item.marmaros_rapier", "item.marmaros_rapier_b",
                "item.kratonite_rapier", "item.kratonite_rapier_b",
                "item.fractite_rapier", "item.fractite_rapier_b",
                "item.zephyrium_rapier", "item.zephyrium_rapier_b",
                "item.argonite_rapier", "item.argonite_rapier_b",
                "item.katagon_rapier", "item.katagon_rapier_b",
                "item.gorgonite_rapier", "item.gorgonite_rapier_b",
                "item.promethium_rapier", "item.promethium_rapier_b",
                "item.primal_rapier", "item.primal_rapier_b",
            ),
            name = "Rapier",
            weaponStyle = WeaponStyle.RAPIER,
            blockAnimationId = Animation.getId("animation.chaotic_rapier_block"),
            soundId = Rscm.lookup("sound.sword_slash"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_stab"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.chaotic_slash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.chaotic_stab"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.chaotic_maul", "item.chaotic_maul_broken",
                "item.novite_maul", "item.novite_maul_b",
                "item.bathus_maul", "item.bathus_maul_b",
                "item.marmaros_maul", "item.marmaros_maul_b",
                "item.kratonite_maul", "item.kratonite_maul_b",
                "item.fractite_maul", "item.fractite_maul_b",
                "item.zephyrium_maul", "item.zephyrium_maul_b",
                "item.argonite_maul", "item.argonite_maul_b",
                "item.katagon_maul", "item.katagon_maul_b",
                "item.gorgonite_maul", "item.gorgonite_maul_b",
                "item.promethium_maul", "item.promethium_maul_b",
                "item.primal_maul", "item.primal_maul_b", "item.elder_maul",
            ),
            name = "Maul",
            weaponStyle = WeaponStyle.MAUL,
            attackDelay = 1,
            blockAnimationId = Animation.getId("animation.chaotic_maul_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.chaotic_crush"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.chaotic_crush"),
                StyleKey(AttackStyle.DEFENSIVE, 2) to Animation.getId("animation.chaotic_crush"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.scythe_of_vitur",
            ),
            name = "Maul",
            weaponStyle = WeaponStyle.SCYTHE,
            attackDelay = 1,
            blockAnimationId = Animation.getId("animation.scythe_block"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.scythe_reap"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.scythe_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 2) to Animation.getId("animation.scythe_jab"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.scythe_reap"),
            ),
            effect = SpecialEffect(
                execute = { context ->
                    val attacker = context.attacker
                    val defender = context.defender
                    attacker.animate(CombatAnimations.getAnimation(context.weaponId, context.attackStyle, attacker.combatDefinitions.attackStyle))
                    val targets = context.getScytheTargets()
                    if (defender.size == 1 && (defender is NPC && !defender.name.contains("dummy", ignoreCase = true))) {
                        for (victim in targets) {
                            context.meleeHit(target = victim)
                        }
                    } else {
                        context.applyScytheHits(defender)
                    }
                    true
                }
            )
        ),
        MeleeWeapon(
            itemId = Item.getIds(
                "item.bronze_2h_sword", "item.iron_2h_sword",
                "item.steel_2h_sword", "item.black_2h_sword",
                "item.mithril_2h_sword", "item.adamant_2h_sword",
                "item.rune_2h_sword", "item.white_2h_sword",
                "item.novite_2h_sword", "item.novite_2h_sword_b",
                "item.bathus_2h_sword", "item.bathus_2h_sword_b",
                "item.marmaros_2h_sword", "item.marmaros_2h_sword_b",
                "item.kratonite_2h_sword", "item.kratonite_2h_sword_b",
                "item.fractite_2h_sword", "item.fractite_2h_sword_b",
                "item.zephyrium_2h_sword", "item.zephyrium_2h_sword_b",
                "item.argonite_2h_sword", "item.argonite_2h_sword_b",
                "item.katagon_2h_sword", "item.katagon_2h_sword_b",
                "item.gorgonite_2h_sword", "item.gorgonite_2h_sword_b",
                "item.promethium_2h_sword", "item.promethium_2h_sword_b",
                "item.primal_2h_sword", "item.primal_2h_sword_b",
            ),
            name = "2h sword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            blockAnimationId = Animation.getId("animation.two_handed_defend"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.two_handed_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.two_handed_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.two_handed_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.two_handed_block"),
            ),
        ),
        MeleeWeapon(
            itemId = Item.getIds("item.dragon_2h_sword", "item.lucky_dragon_2h_sword"),
            name = "Dragon 2h sword",
            weaponStyle = WeaponStyle.TWO_HANDED_SWORD,
            blockAnimationId = Animation.getId("animation.two_handed_defend"),
            animations = mapOf(
                StyleKey(AttackStyle.ACCURATE, 0) to Animation.getId("animation.two_handed_chop"),
                StyleKey(AttackStyle.AGGRESSIVE, 1) to Animation.getId("animation.two_handed_slash"),
                StyleKey(AttackStyle.CONTROLLED, 2) to Animation.getId("animation.two_handed_smash"),
                StyleKey(AttackStyle.DEFENSIVE, 3) to Animation.getId("animation.two_handed_block"),
            ),
            special = SpecialAttack.Combat(
                energyCost = 100,
                accuracyMultiplier = 1.15,
                damageMultiplier = 1.1,
                execute = { context ->
                    context.attacker.animate("animation.dragon_2h_special")
                    context.attacker.gfx("graphic.dragon_2h_special")
                    context.meleeHit()
                    val targets = context.getMultiAttackTargets(1, 8)
                    for (target in targets) {
                        context.meleeHit(target = target, delay = 1)
                    }
                }
            ),
        )
    )
}
