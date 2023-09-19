package me.hugo.savethekweebecs.arena

import com.infernalsuite.aswm.api.SlimePlugin
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.arena.map.MapPoint
import me.hugo.savethekweebecs.ext.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.team.TeamManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.HologramTrait
import net.citizensnpcs.trait.LookClose
import net.citizensnpcs.trait.SkinTrait
import org.bukkit.*
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


class Arena(val arenaMap: ArenaMap, val displayName: String) : KoinComponent {

    private val main = SaveTheKweebecs.getInstance()
    private val slimePlugin: SlimePlugin = main.slimePlugin

    private val gameManager: GameManager by inject()
    private val languageManager: LanguageManager by inject()

    val arenaUUID: UUID = UUID.randomUUID()
    var world: World? = null

    var arenaState: ArenaState = ArenaState.RESETTING
        set(state) {
            field = state
            println("Changed game state of $displayName to $state!")
            // refresh icon
        }

    var arenaTime: Int = arenaMap.defaultCountdown

    val playersPerTeam: MutableMap<TeamManager.Team, MutableList<UUID>> = mutableMapOf(
        Pair(arenaMap.defenderTeam, mutableListOf()),
        Pair(arenaMap.attackerTeam, mutableListOf())
    )

    private val spectators: MutableList<UUID> = mutableListOf()

    val remainingNPCs: MutableMap<NPC, Boolean> = mutableMapOf()

    init {
        main.logger.info("Creating game with map ${arenaMap.mapName} with display name $displayName...")
        loadMap()
        main.logger.info("$displayName is now available!")
    }

    fun joinArena(player: Player) {
        if (hasStarted()) {
            player.sendTranslation("arena.join.started", Pair("arenaName", displayName))
            return
        }

        if (teamPlayers().size >= arenaMap.maxPlayers) {
            player.sendTranslation("arena.join.full", Pair("arenaName", displayName))
            return
        }

        val playerData = player.playerDataOrCreate()

        if (playerData.currentArena != null) {
            player.sendTranslation("arena.join.alreadyInArena")
            return
        }

        val lobbyLocation = arenaMap.getLocation(MapLocation.LOBBY, world) ?: return

        player.reset(GameMode.ADVENTURE)
        player.teleport(lobbyLocation)

        playerData.currentArena = this

        val team = playersPerTeam.keys.minBy { playersPerTeam[it]?.size ?: 0 }
        addPlayerTo(player, team)
        playerData.currentTeam = team

        announceTranslation(
            "arena.join.global",
            Pair("playerName", player.name),
            Pair("currentPlayers", arenaPlayers().size.toString()),
            Pair("maxPlayers", arenaMap.maxPlayers.toString()),
        )

        if (teamPlayers().size >= arenaMap.minPlayers) arenaState = ArenaState.STARTING
    }

    fun leave(player: Player) {
        val playerData = player.playerDataOrCreate()

        if (!hasStarted()) {
            playerData.currentTeam?.let { removePlayerFrom(player, it) }

            announceTranslation(
                "arena.leave.global",
                Pair("playerName", player.name),
                Pair("currentPlayers", arenaPlayers().size.toString()),
                Pair("maxPlayers", arenaMap.maxPlayers.toString()),
            )
        } else {
            player.damage(player.health)
            playerData.currentTeam?.let { removePlayerFrom(player, it) }
        }

        playerData.currentArena = null
        playerData.currentTeam = null

        gameManager.sendToHub(player)
    }

    private fun loadMap() {
        if (!arenaMap.isValid) {
            main.logger.info("Map ${arenaMap.mapName} is not valid!")
            return
        }

        val slimeWorld = arenaMap.slimeWorld!!.clone(arenaUUID.toString())
        slimePlugin.loadWorld(slimeWorld)

        val newWorld = Bukkit.getWorld(arenaUUID.toString()) ?: return

        newWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
        newWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        newWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        newWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        newWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false)

        world = newWorld

        arenaMap.kidnapedPoints?.forEach {
            remainingNPCs[createKweebecNPC(it)] = false
        }

        resetGameValues()
    }

    private fun createKweebecNPC(mapPoint: MapPoint): NPC {
        val attackerTeam = arenaMap.attackerTeam
        val npc = CitizensAPI.getNPCRegistry().createNPC(EntityType.PLAYER, "")

        npc.data().setPersistent(NPC.Metadata.SHOULD_SAVE, false)
        npc.data().setPersistent(NPC.Metadata.NAMEPLATE_VISIBLE, false)
        npc.data().setPersistent("arena", arenaUUID.toString())

        val skinTrait = npc.getOrAddTrait(SkinTrait::class.java)
        skinTrait.setSkinPersistent(
            attackerTeam.id,
            attackerTeam.npcSkin.signature,
            attackerTeam.npcSkin.value
        )

        val lookClose = npc.getOrAddTrait(LookClose::class.java)
        lookClose.lookClose(true)

        val hologramTrait = npc.getOrAddTrait(HologramTrait::class.java)
        hologramTrait.lineHeight = -0.28

        hologramTrait.addLine("${ChatColor.RED}" + languageManager.getLangString("arena.npc.name.${attackerTeam.id}"))
        hologramTrait.addLine("${ChatColor.YELLOW}${ChatColor.BOLD}CLICK TO SAVE")

        hologramTrait.setMargin(0, "bottom", 0.25)

        npc.spawn(mapPoint.toLocation(world!!))

        return npc
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

    private fun addPlayerTo(player: Player, team: TeamManager.Team) {
        addPlayerTo(player.uniqueId, team)
    }

    private fun addPlayerTo(uuid: UUID, team: TeamManager.Team) {
        playersPerTeam.computeIfAbsent(team) { mutableListOf() }.add(uuid)
    }

    private fun removePlayerFrom(player: Player, team: TeamManager.Team) {
        removePlayerFrom(player.uniqueId, team)
    }

    private fun removePlayerFrom(uuid: UUID, team: TeamManager.Team) {
        playersPerTeam[team]?.remove(uuid)
    }

}