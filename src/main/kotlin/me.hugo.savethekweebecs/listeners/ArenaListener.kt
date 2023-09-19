package me.hugo.savethekweebecs.listeners

import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import me.hugo.savethekweebecs.ext.isInGame
import me.hugo.savethekweebecs.ext.playerDataOrCreate
import me.hugo.savethekweebecs.player.PlayerData
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.weather.WeatherChangeEvent
import org.koin.core.component.KoinComponent
import java.util.*

class ArenaListener : KoinComponent, Listener {

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val playerData = player.playerDataOrCreate()

        val currentArena = playerData.currentArena

        if (currentArena?.isInGame() == true) {
            if (playerData.currentTeam == currentArena.arenaMap.attackerTeam && event.block.type == Material.IRON_BARS) return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onFoodLevelChange(event: FoodLevelChangeEvent) {
        val player = event.entity as Player
        val playerData = player.playerDataOrCreate()

        if (playerData.currentArena?.isInGame() == true) return

        event.isCancelled = true
    }

    @EventHandler
    fun onDamage(event: EntityDamageEvent) {
        val player = event.entity
        if (player !is Player) return

        val playerData = player.playerDataOrCreate()

        if (playerData.currentArena?.isInGame() == true) {
            if (event is EntityDamageByEntityEvent) {
                val attacker = event.damager

                val playerSource: UUID? = if (attacker is Player) attacker.uniqueId
                else if (attacker is Projectile) {
                    val shooter = attacker.shooter
                    if (shooter is Player) shooter.uniqueId
                    else null
                } else null

                playerSource?.let { playerData.lastAttacker = PlayerData.PlayerAttack(it) }
            }

            if (player.health - event.finalDamage <= 0) {
                event.isCancelled = true
                // TODO: Handle death and respawn here.
            }
            return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onItemDrop(event: PlayerDropItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onItemPickup(event: EntityPickupItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onExpPickup(event: PlayerPickupExperienceEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun onWeatherChange(event: WeatherChangeEvent) {
        event.isCancelled = true
    }
}