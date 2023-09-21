package me.hugo.savethekweebecs.arena

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapPoint
import me.hugo.savethekweebecs.ext.playerData
import me.hugo.savethekweebecs.ext.reset
import me.hugo.savethekweebecs.task.GameControllerTask
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scoreboard.DisplaySlot
import org.koin.core.annotation.Single
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

@Single
class GameManager {

    private val main: SaveTheKweebecs = SaveTheKweebecs.getInstance()

    private val hubLocation: Location? =
        Bukkit.getWorld("world")?.let { MapPoint.deserializeFromConfig("hubLocation")?.toLocation(it) }
    val maps: Map<String, ArenaMap>
    val arenas: ConcurrentMap<UUID, Arena> = ConcurrentHashMap()

    init {
        val mapKeys = main.config.getConfigurationSection("maps")?.getKeys(false)
        maps = mapKeys?.associateWith { ArenaMap(it) } ?: mapOf()

        GameControllerTask().runTaskTimer(main, 0L, 20L)
    }

    fun sendToHub(player: Player) {
        removeScoreboardEntries(player)

        hubLocation?.let { player.teleport(it) }
        player.reset(GameMode.ADVENTURE)

        val playerData = player.playerData() ?: return

        playerData.setLobbyBoard(player)

        playerData.kills = 0
        playerData.deaths = 0
        playerData.coins =0
    }

    private fun removeScoreboardEntries(player: Player) {
        val scoreboard = player.scoreboard

        scoreboard.getTeam("own")?.let { team -> team.removeEntries(team.entries) }
        scoreboard.getTeam("enemy")?.let { team -> team.removeEntries(team.entries) }

        scoreboard.clearSlot(DisplaySlot.BELOW_NAME)
    }
}