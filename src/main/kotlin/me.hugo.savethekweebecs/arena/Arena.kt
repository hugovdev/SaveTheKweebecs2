package me.hugo.savethekweebecs.arena

import com.infernalsuite.aswm.api.SlimePlugin
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.ext.*
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class Arena(val arenaMap: ArenaMap, val displayName: String) : KoinComponent {

    private val main = SaveTheKweebecs.getInstance()
    private val slimePlugin: SlimePlugin = main.slimePlugin

    private val gameUUID: UUID = UUID.randomUUID()
    private var world: World? = null

    var arenaState: ArenaState = ArenaState.RESETTING
        set(state) {
            field = state
            println("Changed game state of $displayName to $state!")
            // refresh icon
        }

    private var arenaTime: Int = arenaMap.defaultCountdown

    private val playersPerTeam: MutableMap<Team, MutableList<UUID>> =
        Team.entries.associateWith { mutableListOf<UUID>() }.toMutableMap()
    private val spectators: MutableList<UUID> = mutableListOf()

    init {
        main.logger.info("Creating game with map ${arenaMap.mapName} with display name $displayName...")
        loadMap()
        main.logger.info("$displayName is now available!")
    }

    fun joinArena(player: Player) {
        if (hasStarted()) {
            player.sendTranslation("arena.join.started")
            return
        }

        if (teamPlayers().size >= arenaMap.maxPlayers) {
            player.sendTranslation("arena.join.full")
            return
        }

        val lobbyLocation = arenaMap.getLocation(MapLocation.LOBBY, world) ?: return

        player.reset(GameMode.ADVENTURE)
        player.teleport(lobbyLocation)

        val playerData = player.playerDataOrCreate()

        playerData.currentArena = this

        val team = playersPerTeam.keys.minBy { playersPerTeam[it]?.size ?: 0 }
        addPlayerTo(player, team)
        playerData.currentTeam = team

        announceTranslation(
            "arena.join.global",
            mapOf(
                Pair("playerName", player.name),
                Pair("currentPlayers", arenaPlayers().size.toString()),
                Pair("maxPlayers", arenaMap.maxPlayers.toString()),
            )
        )
    }

    fun leave(player: Player) {
        playersPerTeam[player.playerDataOrCreate().currentTeam]?.remove(player.uniqueId)

        if (!hasStarted()) {
            announceTranslation(
                "arena.leave.global",
                mapOf(
                    Pair("playerName", player.name),
                    Pair("currentPlayers", arenaPlayers().size.toString()),
                    Pair("maxPlayers", arenaMap.maxPlayers.toString()),
                )
            )
        }
    }

    private fun loadMap() {
        if (!arenaMap.isValid) {
            main.logger.info("Map ${arenaMap.mapName} is not valid!")
            return
        }

        val slimeWorld = arenaMap.slimeWorld!!.clone(gameUUID.toString())
        slimePlugin.loadWorld(slimeWorld)

        val newWorld = Bukkit.getWorld(gameUUID.toString()) ?: return

        newWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
        newWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

        world = newWorld

        resetGameValues()
    }

    private fun resetGameValues() {
        arenaState = ArenaState.WAITING
        arenaTime = arenaMap.defaultCountdown

        // TOOD: Reset saved NPCs
    }

    fun teamPlayers(): List<UUID> {
        return playersPerTeam.values.flatten()
    }

    fun arenaPlayers(): List<UUID> {
        return teamPlayers().plus(spectators)
    }

    private fun addPlayerTo(player: Player, team: Team) {
        addPlayerTo(player.uniqueId, team)
    }

    private fun addPlayerTo(uuid: UUID, team: Team) {
        playersPerTeam.computeIfAbsent(team) { mutableListOf() }.add(uuid)
    }

    private fun removePlayerFrom(player: Player, team: Team) {
        removePlayerFrom(player.uniqueId, team)
    }

    private fun removePlayerFrom(uuid: UUID, team: Team) {
        playersPerTeam[team]?.remove(uuid)
    }

}