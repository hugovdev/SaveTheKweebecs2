package me.hugo.savethekweebecs.arena

import com.infernalsuite.aswm.api.SlimePlugin
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.Map
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.ext.*
import me.hugo.savethekweebecs.player.PlayerManager
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

class Arena(val map: Map, val displayName: String) : KoinComponent {

    private val main = SaveTheKweebecs.getInstance()
    private val slimePlugin: SlimePlugin by inject()
    private val playerManager: PlayerManager by inject()

    private val gameUUID: UUID = UUID.randomUUID()
    private var world: World? = null

    private var arenaState: ArenaState = ArenaState.WAITING
        set(state) {
            field = state
            println("hi")
            // refresh icon
        }

    private var arenaTime: Int = map.defaultCountdown

    private val playersPerTeam: MutableMap<Team, MutableList<UUID>> = mutableMapOf()
    private val spectators: MutableList<UUID> = mutableListOf()

    init {
        main.logger.info("Creating game with map ${map.mapName} with display name $displayName...")
        loadMap()
        main.logger.info("$displayName is now available!")
    }

    fun joinArena(player: Player) {
        if (arenaState != ArenaState.WAITING && arenaState != ArenaState.IN_GAME) {
            player.sendTranslation("arena.join.started")
            return
        }

        if (playersPerTeam.values.flatten().size >= map.maxPlayers) {
            player.sendTranslation("arena.join.full")
            return
        }

        val lobbyLocation = map.getLocation(MapLocation.LOBBY, world) ?: return

        player.reset(GameMode.ADVENTURE)
        player.teleport(lobbyLocation)

        player.playerDataOrCreate().currentArena = this
        addPlayerTo(player, playersPerTeam.keys.minBy { playersPerTeam[it]?.size ?: 0 })

        announceTranslation(
            "arena.join.global",
            mapOf(
                Pair("playerName", player.name),
                Pair("currentPlayers", arenaPlayers().size.toString()),
                Pair("maxPlayers", map.maxPlayers.toString()),
            )
        )
    }

    private fun loadMap() {
        if (!map.isValid) {
            main.logger.info("Map ${map.mapName} is not valid!")
            return
        }

        val slimeWorld = map.slimeWorld!!.clone(gameUUID.toString())
        slimePlugin.loadWorld(slimeWorld)

        val newWorld = Bukkit.getWorld(gameUUID.toString()) ?: return

        newWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
        newWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)

        world = newWorld

        resetGameValues()
    }

    private fun resetGameValues() {
        arenaState = ArenaState.WAITING
        arenaTime = map.defaultCountdown

        // TOOD: Reset saved NPCs
    }

    fun arenaPlayers(): List<UUID> {
        return playersPerTeam.values.flatten().plus(spectators)
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