package com.rs.kotlin.game.world.projectile

import kotlinx.coroutines.delay

object ProjectileRegistry {
    private val projectileTypes = mutableMapOf<Projectile, ProjectileType>()

    init {
        register(Projectile.ARROW, ProjectileType(delay = 1.5, speed = 7, angle = 12))
        register(Projectile.DRAGON_ARROW, ProjectileType(delay = 1.5, speed = 5, angle = 15))
        register(Projectile.BOLT, ProjectileType(delay = 1.5, speed = 8, angle = 12))
        register(Projectile.THROWING_KNIFE, ProjectileType(delay = 1.5, speed = 7, angle = 12))
        register(Projectile.DART, ProjectileType(delay = 1.0, speed = 7, angle = 12))
        register(Projectile.SAP, ProjectileType(delay = 1.5, speed = 3, angle = 12, displacement = 64))
        register(Projectile.LEECH, ProjectileType(delay = 1.5, speed = 3, angle = 0, displacement = 64))
        register(Projectile.SOULSPLIT, ProjectileType(startHeight = 4, endHeight = 4, delay = 1.5, speed = 3, angle = 0))
        register(Projectile.CANNON, ProjectileType(startHeight = 42, endHeight = 42, delay = 1.5, speed = 7, angle = 12))
        register(Projectile.ELEMENTAL_SPELL, ProjectileType(delay = 1.5, speed = 3, angle = 12))
        register(Projectile.STANDARD_MAGIC, ProjectileType(delay = 1.5, speed = 3, angle = 12))
        register(Projectile.STANDARD_MAGIC_INSTANT, ProjectileType(delay = 1.5, speed = 3, angle = 12))
        register(Projectile.ANCIENT_SPELL, ProjectileType(delay = 1.5, speed = 3, angle = 12))
        register(Projectile.SLAYER_DART, ProjectileType(delay = 1.5, speed = 3, angle = 12))
        register(Projectile.IBAN_BLAST, ProjectileType(startHeight = 51, delay = 1.5, speed = 3, angle = 12))
        register(Projectile.STORM_OF_ARMADYL, ProjectileType(startHeight = 0, endHeight = 0, delay = 1.5, speed = 2, angle = 12))
        register(Projectile.TELEPORT_BLOCK, ProjectileType(startHeight = 21, delay = 1.5, speed = 3, angle = 12))
        register(Projectile.POLYPORE_STAFF, ProjectileType(startHeight = 51, delay = 1.5, speed = 3, angle = 12))
        register(Projectile.SAGAIE, ProjectileType(delay = 1.5, speed = 7, angle = 12))
    }

    private fun register(projectile: Projectile, type: ProjectileType) {
        projectileTypes[projectile] = type
    }

    fun get(projectile: Projectile): ProjectileType? = projectileTypes[projectile]
}

