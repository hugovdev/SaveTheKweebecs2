package me.hugo.savethekweebecs.arena

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapPoint
import me.hugo.savethekweebecs.clickableitems.ItemSetManager
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.music.SoundManager
import me.hugo.savethekweebecs.task.GameControllerTask
import me.hugo.savethekweebecs.util.menus.Icon
import me.hugo.savethekweebecs.util.menus.PaginatedMenu
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Single
class GameManager : KoinComponent {

    private val main: SaveTheKweebecs = SaveTheKweebecs.getInstance()
    private val itemManager: ItemSetManager by inject()
    private val languageManager: LanguageManager by inject()
    private val soundManager: SoundManager by inject()

    private val hubLocation: Location? = Bukkit.getWorld("world")
        ?.let { MapPoint.deserializeFromConfig("hubLocation")?.toLocation(it) }

    val maps: Map<String, ArenaMap>
    val arenas: ConcurrentMap<UUID, Arena> = ConcurrentHashMap()

    // lang -> menu
    val arenaMenus: MutableMap<String, PaginatedMenu> = mutableMapOf()

    init {
        val config = main.config

        val mapKeys = config.getConfigurationSection("maps")?.getKeys(false)
        maps = mapKeys?.associateWith { ArenaMap(it) } ?: mapOf()

        languageManager.availableLanguages.forEach {
            arenaMenus[it] = PaginatedMenu(
                9 * 4, "menu.arenas.title", PaginatedMenu.PageFormat.TWO_ROWS_TRIMMED.format,
                ItemStack(Material.ENDER_EYE)
                    .name("menu.arenas.icon.name", it)
                    .putLore("menu.arenas.icon.lore", it), null, it
            )
        }

        GameControllerTask().runTaskTimer(main, 0L, 20L)
    }

    fun openArenasMenu(player: Player) {
        (arenaMenus[player.playerDataOrCreate().locale] ?: arenaMenus[LanguageManager.DEFAULT_LANGUAGE]!!).open(player)
    }

    fun refreshArenaIcon(arena: Arena) {
        arenaMenus.forEach {
            arena.lastIcon[it.key]?.let { lastIcon ->
                it.value.replaceFirst(lastIcon, Icon(arena.getCurrentIcon(it.key)).addClickAction { player, _ ->
                    arena.joinArena(player)
                })
            }
        }
    }

    fun registerArena(arena: Arena) {
        arenas[arena.arenaUUID] = arena

        arenaMenus.forEach {
            it.value.addItem(Icon(arena.getCurrentIcon(it.key)).addClickAction { player, _ ->
                arena.joinArena(player)
            })
        }
    }

    fun sendToHub(player: Player) {
        if (!player.isOnline) return

        removeScoreboardEntries(player)

        hubLocation?.let { player.teleport(it) }
        player.reset(GameMode.ADVENTURE)

        val playerData = player.playerData() ?: return

        playerData.setLobbyBoard(player)

        playerData.kills = 0
        playerData.deaths = 0
        playerData.resetCoins()

        soundManager.stopTrack(player)

        itemManager.getSet("lobby")?.forEach { it.give(player) }
    }

    private fun removeScoreboardEntries(player: Player) {
        val scoreboard = player.scoreboard

        scoreboard.getTeam("own")?.let { team ->
            team.removeEntries(team.entries)
            team.unregister()
        }

        scoreboard.getTeam("enemy")?.let { team ->
            team.removeEntries(team.entries)
            team.unregister()
        }

        val serverPlayer = (player as CraftPlayer).handle
        val healthObjective = player.playerData()?.healthObjective

        if (healthObjective != null) {
            serverPlayer.connection.send(
                ClientboundSetObjectivePacket(
                    healthObjective,
                    ClientboundSetObjectivePacket.METHOD_REMOVE
                )
            )
        }
    }
}