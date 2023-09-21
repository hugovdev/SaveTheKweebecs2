package me.hugo.savethekweebecs.arena

import com.infernalsuite.aswm.api.SlimePlugin
import com.infernalsuite.aswm.api.world.SlimeWorld
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.events.ArenaEvent
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.arena.map.MapPoint
import me.hugo.savethekweebecs.ext.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import me.hugo.savethekweebecs.team.TeamManager
import net.citizensnpcs.api.CitizensAPI
import net.citizensnpcs.api.npc.NPC
import net.citizensnpcs.trait.CurrentLocation
import net.citizensnpcs.trait.HologramTrait
import net.citizensnpcs.trait.LookClose
import net.citizensnpcs.trait.SkinTrait
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.GameRule
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap


class Arena(val arenaMap: ArenaMap, val displayName: String) : KoinComponent {

    private val main = SaveTheKweebecs.getInstance()
    private val slimePlugin: SlimePlugin = main.slimePlugin

    private val gameManager: GameManager by inject()
    private val languageManager: LanguageManager by inject()
    private val scoreboardManager: ScoreboardTemplateManager by inject()

    val arenaUUID: UUID = UUID.randomUUID()

    private val slimeWorld: SlimeWorld? = arenaMap.slimeWorld!!.clone(arenaUUID.toString())

    var world: World? = null

    var arenaState: ArenaState = ArenaState.RESETTING
        set(state) {
            field = state
            arenaPlayers().mapNotNull { it.player() }.forEach { setCurrentBoard(it) }

            println("Changed game state of $displayName to $state!")
            // refresh icon
        }

    var arenaTime: Int = arenaMap.defaultCountdown
    var eventIndex: Int = 0
        set(value) {
            field = value
            currentEvent = arenaMap.events.getOrNull(eventIndex)?.first
        }

    var currentEvent: ArenaEvent? = arenaMap.events[eventIndex].first

    val playersPerTeam: MutableMap<TeamManager.Team, MutableList<UUID>> = mutableMapOf(
        Pair(arenaMap.defenderTeam, mutableListOf()),
        Pair(arenaMap.attackerTeam, mutableListOf())
    )

    private val spectators: MutableList<UUID> = mutableListOf()
    val deadPlayers: ConcurrentMap<Player, Int> = ConcurrentHashMap()

    val remainingNPCs: MutableMap<NPC, Boolean> = mutableMapOf()

    init {
        main.logger.info("Creating game with map ${arenaMap.mapName} with display name $displayName...")
        loadMap(true)
        main.logger.info("$displayName is now available!")
    }

    fun joinArena(player: Player) {
        if (hasStarted()) {
            player.sendTranslation("arena.join.started", Placeholder.unparsed("arena_name", displayName))
            return
        }

        if (teamPlayers().size >= arenaMap.maxPlayers) {
            player.sendTranslation("arena.join.full", Placeholder.unparsed("arena_name", displayName))
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

        announceTranslation(
            "arena.join.global",
            Placeholder.unparsed("player_name", player.name),
            Placeholder.unparsed("current_players", arenaPlayers().size.toString()),
            Placeholder.unparsed("max_players", arenaMap.maxPlayers.toString()),
        )

        if (teamPlayers().size >= arenaMap.minPlayers) arenaState = ArenaState.STARTING
        else updateBoard("players", "max_players")

        setCurrentBoard(player)
    }

    private fun setCurrentBoard(player: Player) {
        scoreboardManager.loadedTemplates[arenaState.name.lowercase()]?.printBoard(player)
    }

    fun updateBoard(vararg tags: String) {
        arenaPlayers().mapNotNull { it.player() }.forEach { it.updateBoardTags(*tags) }
    }

    fun leave(player: Player) {
        val playerData = player.playerDataOrCreate()

        if (!hasStarted()) {
            playerData.currentTeam?.let { removePlayerFrom(player, it) }

            announceTranslation(
                "arena.leave.global",
                Placeholder.unparsed("player_name", player.name),
                Placeholder.unparsed("current_players", arenaPlayers().size.toString()),
                Placeholder.unparsed("max_players", arenaMap.maxPlayers.toString()),
            )
        } else {
            player.damage(player.health)
            playerData.currentTeam?.let { removePlayerFrom(player, it) }
            deadPlayers.remove(player)
        }

        playerData.currentArena = null
        gameManager.sendToHub(player)
    }

    fun loadMap(firstTime: Boolean = true) {
        emptyPlayerPools()

        if (!arenaMap.isValid) {
            main.logger.info("Map ${arenaMap.mapName} is not valid!")
            return
        }

        if (!firstTime) Bukkit.unloadWorld(world!!, false)

        slimePlugin.loadWorld(slimeWorld)

        val newWorld = Bukkit.getWorld(arenaUUID.toString()) ?: return

        newWorld.setGameRule(GameRule.SPECTATORS_GENERATE_CHUNKS, false)
        newWorld.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false)
        newWorld.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false)
        newWorld.setGameRule(GameRule.DO_WEATHER_CYCLE, false)
        newWorld.setGameRule(GameRule.DO_MOB_SPAWNING, false)

        world = newWorld

        if (firstTime) arenaMap.kidnapedPoints?.forEach { remainingNPCs[createKweebecNPC(it)] = false }
        else remainingNPCs.keys.forEach {
            remainingNPCs[it] = false
            it.spawn(it.storedLocation.toLocation(world!!))
        }

        arenaState = ArenaState.WAITING
        arenaTime = arenaMap.defaultCountdown
        eventIndex = 0
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
            attackerTeam.npcTemplate.signature,
            attackerTeam.npcTemplate.value
        )

        npc.getOrAddTrait(CurrentLocation::class.java)

        val lookClose = npc.getOrAddTrait(LookClose::class.java)
        lookClose.lookClose(true)

        val hologramTrait = npc.getOrAddTrait(HologramTrait::class.java)
        hologramTrait.lineHeight = -0.28
        hologramTrait.setUseDisplayEntities(true)

        hologramTrait.addLine(
            LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage()
                    .deserialize(languageManager.getLangString("arena.npc.name.${attackerTeam.id}"))
            )
        )

        hologramTrait.addLine(
            LegacyComponentSerializer.legacySection().serialize(
                MiniMessage.miniMessage()
                    .deserialize(languageManager.getLangString("arena.npc.action.${attackerTeam.id}"))
            )
        )

        hologramTrait.setMargin(0, "bottom", -0.15)

        npc.spawn(mapPoint.toLocation(world!!))

        return npc
    }

    private fun emptyPlayerPools() {
        playersPerTeam.values.forEach { it.clear() }
        spectators.clear()
        deadPlayers.clear()
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
        uuid.player()?.playerData()?.currentTeam = team
    }

    private fun removePlayerFrom(player: Player, team: TeamManager.Team) {
        removePlayerFrom(player.uniqueId, team)
    }

    private fun removePlayerFrom(uuid: UUID, team: TeamManager.Team) {
        playersPerTeam[team]?.remove(uuid)
        uuid.player()?.playerData()?.currentTeam = null
    }

}