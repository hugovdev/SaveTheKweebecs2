package me.hugo.savethekweebecs.arena.map

import com.infernalsuite.aswm.api.SlimePlugin
import com.infernalsuite.aswm.api.exceptions.SlimeException
import com.infernalsuite.aswm.api.world.SlimeWorld
import com.infernalsuite.aswm.api.world.properties.SlimeProperties
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Team
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class Map(private val configName: String) {

    companion object {
        val DEFAULT_PROPERTIES = SlimePropertyMap().apply {
            setValue(SlimeProperties.DIFFICULTY, "normal")
            setValue(SlimeProperties.ALLOW_ANIMALS, false)
            setValue(SlimeProperties.ALLOW_MONSTERS, false)
        }
    }

    public var isValid = false

    public var slimeWorld: SlimeWorld? = null

    public var mapName: String = "Unknown"

    public val mapLocations: MutableMap<MapLocation, MapPoint> = mutableMapOf()
    public val spawnPoints: MutableMap<Team, List<MapPoint>> = mutableMapOf()

    public var kidnapedPoints: List<MapPoint>? = null

    public var minPlayers: Int = 6
    public var maxPlayers: Int = 12

    public var defaultCountdown: Int = 60

    init {
        val main = SaveTheKweebecs.getInstance()

        loadMap(main) {
            val config = main.config
            val configPath = "maps.$configName"

            minPlayers = config.getInt("$configPath.minPlayers", 6)
            maxPlayers = config.getInt("$configPath.maxPlayers", 12)

            defaultCountdown = config.getInt("$configPath.defaultCountdown", 60)

            config.getString("$configPath.mapName")?.let { mapName = it }

            kidnapedPoints = config.getStringList("$configPath.kidnapedPoints")
                .mapNotNull { MapPoint.deserialize(it) }

            // Read every location from the map and save it. (Waiting lobby, spectators, etc.)
            MapLocation.entries.forEach { location ->
                MapPoint.deserializeFromConfig("$configPath.${location.name.lowercase()}")?.let {
                    mapLocations[location] = it
                }
            }

            // Read the spawn points for each team in the config file!
            Team.entries.forEach { team ->
                spawnPoints[team] = config.getStringList("$configPath.${team.name.lowercase()}")
                    .mapNotNull { MapPoint.deserialize(it) }
            }

            isValid = true
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

                    // TODO: Create the default game(s) for this map!
                } catch (e: SlimeException) {
                    main.logger.info("There was a problem trying to load the world: $slimeWorldName")
                    e.printStackTrace()
                }

                object : BukkitRunnable() {
                    override fun run() {
                        onSuccessful()
                    }
                }.runTask(main)
            }
        }.runTaskAsynchronously(main)
    }
}