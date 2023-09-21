package me.hugo.savethekweebecs.listeners

import com.destroystokyo.paper.MaterialSetTag
import com.destroystokyo.paper.MaterialTags
import com.destroystokyo.paper.event.player.PlayerPickupExperienceEvent
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.util.InstantFirework
import net.citizensnpcs.api.event.NPCRightClickEvent
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.*
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
import org.koin.core.component.inject
import java.util.*

class ArenaListener : KoinComponent, Listener {

    private val gameManager: GameManager by inject()

    private companion object {
        private val BREAKABLE_ATTACKER_BLOCKS = MaterialSetTag(NamespacedKey("stk", "kweebec_breakable"))
            .add(MaterialTags.FENCES).add(Material.BAMBOO_PLANKS, Material.IRON_BARS).lock()
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
        val arena: Arena? = player.arena()

        if (arena?.isInGame() == true && player.gameMode != GameMode.SPECTATOR) {
            if (event is EntityDamageByEntityEvent) {
                val attacker = event.damager

                val playerSource: Player? = if (attacker is Player) attacker
                else if (attacker is Projectile) {
                    val shooter = attacker.shooter
                    if (shooter is Player) shooter
                    else null
                } else null

                if (playerSource?.playerData()?.currentTeam == playerData.currentTeam) {
                    event.isCancelled = true
                    return
                }

                playerSource?.let { playerData.lastAttack = PlayerData.PlayerAttack(it.uniqueId) }
            }

            if (player.health - event.finalDamage <= 0) {
                event.isCancelled = true

                val deathLocation = player.location

                playerData.deaths++
                player.gameMode = GameMode.SPECTATOR
                arena.deadPlayers[player] = 5

                deathLocation.world.playEffect(deathLocation, Effect.STEP_SOUND, Material.REDSTONE_BLOCK)
                deathLocation.world.playEffect(
                    deathLocation.clone().add(0.0, 1.0, 0.0),
                    Effect.STEP_SOUND,
                    Material.REDSTONE_BLOCK
                )

                val lastAttack = playerData.lastAttack
                val attacker = lastAttack?.attacker?.player()

                // If there is no last attack, it was too long ago or the attacker has disconnected, player died
                // by themselves!
                if (lastAttack == null || lastAttack.time < System.currentTimeMillis() - (10000) || attacker == null) {
                    arena.announceTranslation("arena.death.self", Placeholder.unparsed("player", player.name))
                } else {
                    val attackerData = attacker.playerDataOrCreate()

                    attackerData.kills++
                    attackerData.coins += 10

                    attacker.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)

                    arena.announceTranslation(
                        "arena.death.player",
                        Placeholder.unparsed("player", player.name),
                        Placeholder.unparsed("killer", attacker.name)
                    )
                }
            }

            return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onNPCClick(event: NPCRightClickEvent) {
        val npc = event.npc

        if (npc.data().has("arena")) {
            val arena = gameManager.arenas[UUID.fromString(npc.data().get("arena"))] ?: return

            if (!arena.isInGame()) return

            val player = event.clicker
            val attackerTeam = arena.arenaMap.attackerTeam
            if (player.playerDataOrCreate().currentTeam != attackerTeam) return

            npc.despawn()
            arena.remainingNPCs[npc] = true

            arena.announceTranslation(
                "arena.${attackerTeam.id}.saved",
                Placeholder.unparsed("player", player.name),
                Placeholder.unparsed("npcs_saved", arena.remainingNPCs.count { it.value }.toString()),
                Placeholder.unparsed("total_npcs", arena.remainingNPCs.size.toString())
            )

            InstantFirework(
                FireworkEffect.builder().withColor(Color.GREEN, Color.ORANGE).flicker(true)
                    .trail(true).withFade(Color.RED).build(), npc.storedLocation
            )

            if (arena.remainingNPCs.all { it.value }) arena.end(attackerTeam)
            else arena.updateBoard("npcs_saved", "total_npcs")
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val playerData = player.playerDataOrCreate()

        val currentArena = playerData.currentArena

        if (currentArena?.isInGame() == true) {
            if (playerData.currentTeam == currentArena.arenaMap.defenderTeam && event.block.type == Material.BAMBOO_PLANKS) return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun onBlockBreak(event: BlockBreakEvent) {
        val player = event.player
        val playerData = player.playerDataOrCreate()

        val currentArena = playerData.currentArena

        if (currentArena?.isInGame() == true) {
            if (playerData.currentTeam == currentArena.arenaMap.attackerTeam &&
                BREAKABLE_ATTACKER_BLOCKS.isTagged(event.block)
            ) {
                event.isDropItems = false
                return
            }
        }

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