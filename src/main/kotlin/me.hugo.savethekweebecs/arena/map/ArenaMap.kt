package me.hugo.savethekweebecs.arena.map

import com.infernalsuite.aswm.api.SlimePlugin
import com.infernalsuite.aswm.api.exceptions.SlimeException
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.arena.events.ArenaEvent
import me.hugo.savethekweebecs.team.TeamManager
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ArenaMap(val configName: String, load: Boolean = true) : KoinComponent {

    private val gameManager: GameManager by inject()
    private val teamManager: TeamManager by inject()

    companion object {
        val DEFAULT_PROPERTIES = SlimePropertyMap().apply {
            setValue(SlimeProperties.DIFFICULTY, "normal")
            setValue(SlimeProperties.ALLOW_ANIMALS, false)
            setValue(SlimeProperties.ALLOW_MONSTERS, false)
        }
    }

    var isValid = false

    var slimeWorld: SlimeWorld? = null
    var mapName: String = configName.lowercase()

    val mapLocations: MutableMap<MapLocation, MapPoint> = mutableMapOf()
    val spawnPoints: MutableMap<String, MutableList<MapPoint>> = mutableMapOf()

    var defenderTeam: TeamManager.Team = teamManager.teams["trork"]!!
    var attackerTeam: TeamManager.Team = teamManager.teams["kweebec"]!!

    var events: MutableList<Pair<ArenaEvent, Int>> = mutableListOf()

    var kidnapedPoints: MutableList<MapPoint>? = null

    var minPlayers: Int = 6
    var maxPlayers: Int = 12
    var defaultCountdown: Int = 60

    init {
        if (load) {
            val main = SaveTheKweebecs.getInstance()

            main.logger.info("Loading map $configName...")

            loadMap(main) {
                main.logger.info("Slime map was loaded successfully!")
                main.logger.info("Fetching the rest of the game data...")

                val config = main.config
                val configPath = "maps.$configName"

                minPlayers = config.getInt("$configPath.minPlayers", 6)
                maxPlayers = config.getInt("$configPath.maxPlayers", 12)

                teamManager.teams[config.getString("$configPath.defenderTeam")]?.let { defenderTeam = it }
                teamManager.teams[config.getString("$configPath.attackerTeam")]?.let { attackerTeam = it }

                defaultCountdown = config.getInt("$configPath.defaultCountdown", 60)

                events = config.getStringList("$configPath.events").mapNotNull { ArenaEvent.deserialize(it) }
                    .toMutableList()

                config.getString("$configPath.mapName")?.let { mapName = it }

                kidnapedPoints = config.getStringList("$configPath.kidnapedPoints")
                    .mapNotNull { MapPoint.deserialize(it) }.toMutableList()

                // Read every location from the map and save it. (Waiting lobby, spectators, etc.)
                MapLocation.entries.forEach { location ->
                    MapPoint.deserializeFromConfig("$configPath.${location.name.lowercase()}")?.let {
                        mapLocations[location] = it
                    }
                }

                // Read the spawn points for each team in the config file!
                teamManager.teams.values.forEach { team ->
                    spawnPoints[team.id] = config.getStringList("$configPath.${team.id.lowercase()}")
                        .mapNotNull { MapPoint.deserialize(it) }.toMutableList()
                }

                main.logger.info("Map $configName has been loaded correctly and is now valid!")
                isValid = true

                val arena = Arena(this, mapName)
                gameManager.arenas[arena.arenaUUID] = arena
            }
        }
    }

    fun getLocation(location: MapLocation, world: World?): Location? {
        if (world == null) return null
        return mapLocations[location]?.toLocation(world)
    }

    private fun loadMap(main: JavaPlugin, onSuccessful: () -> Unit) {
        val slimePlugin: SlimePlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager") as SlimePlugin
        val slimeWorldName = SaveTheKweebecs.getInstance().config.getString("maps.$configName.slimeWorld")

        object : BukkitRunnable() {
            override fun run() {
                try {
                    slimeWorld = slimePlugin.loadWorld(
                        slimePlugin.getLoader("file"),
                        slimeWorldName, true, DEFAULT_PROPERTIES
                    )

                    object : BukkitRunnable() {
                        override fun run() {
                            onSuccessful()
                        }
                    }.runTask(main)
                } catch (e: SlimeException) {
                    main.logger.info("There was a problem trying to load the world: $slimeWorldName")
                    e.printStackTrace()
                }
            }
        }.runTaskAsynchronously(main)
    }
}