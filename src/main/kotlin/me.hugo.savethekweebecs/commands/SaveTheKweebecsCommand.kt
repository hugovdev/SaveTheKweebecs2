package me.hugo.savethekweebecs.commands

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.arena.Team
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.arena.map.MapPoint
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import revxrsal.commands.annotation.Command
import revxrsal.commands.annotation.Description
import revxrsal.commands.annotation.Subcommand
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*

@Command("savethekweebecs", "stk")
@Description("Main SaveTheKweebecs command.")
class SaveTheKweebecsCommand : KoinComponent {

    private val snakeRegex = "_[a-zA-Z]".toRegex()
    private val configuringMap: MutableMap<UUID, ArenaMap> = mutableMapOf()

    private val main = SaveTheKweebecs.getInstance()
    private val gameManager: GameManager by inject()

    @Subcommand("auto-join")
    @Description("Auto-joins a SaveTheKweebecs map!")
    private fun autoJoin(sender: Player) {
        // Picks the fullest available arena and joins!
        gameManager.arenas.filter { it.teamPlayers().size < it.arenaMap.maxPlayers }
            .maxByOrNull { it.teamPlayers().size }?.joinArena(sender)
    }

    @Subcommand("admin create")
    @Description("Creates a SaveTheKweebecs map!")
    @CommandPermission("savethekweebecs.admin")
    private fun createMap(sender: Player, mapName: String) {
        val camelName = mapName.replace(" ", "_").lowercase().snakeToLowerCamelCase()

        val map = ArenaMap(camelName, false)
        map.mapName = mapName

        configuringMap[sender.uniqueId] = map

        sender.sendMessage(
            Component.text(
                "Successfully created map $mapName with config name $camelName.",
                NamedTextColor.GREEN
            )
        )
    }

    @Subcommand("admin setlocation")
    @Description("Sets the MapLocation to the player's location!")
    @CommandPermission("savethekweebecs.admin")
    private fun setLocation(sender: Player, location: MapLocation) {
        sender.getConfiguringMap()?.apply {
            mapLocations[location] = MapPoint(sender.location)

            sender.sendMessage(
                Component.text(
                    "Successfully set the location of $location to your location.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin addspawn")
    @Description("Adds the player's location to the spawnpoint list of this team!")
    @CommandPermission("savethekweebecs.admin")
    private fun addSpawnpoint(sender: Player, team: Team) {
        sender.getConfiguringMap()?.apply {
            spawnPoints.computeIfAbsent(team) { mutableListOf() }.add(MapPoint(sender.location))

            sender.sendMessage(
                Component.text(
                    "Successfully added your location to the spawnpoint list of $team.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin addkidnaped")
    @Description("Adds the player's location to the kidnaped spawnpoint list!")
    @CommandPermission("savethekweebecs.admin")
    private fun addKidnaped(sender: Player) {
        sender.getConfiguringMap()?.apply {
            if (kidnapedPoints == null) kidnapedPoints = mutableListOf()
            kidnapedPoints!!.add(MapPoint(sender.location))

            sender.sendMessage(
                Component.text(
                    "Successfully added your location to the kidnaped NPC list.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin setminplayers")
    @Description("Sets the min player count for the map!")
    @CommandPermission("savethekweebecs.admin")
    private fun setMinplayers(sender: Player, minimumPlayers: Int) {
        sender.getConfiguringMap()?.apply {
            minPlayers = minimumPlayers

            sender.sendMessage(
                Component.text(
                    "Successfully set $minimumPlayers as the minimum for this map.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin setmaxplayers")
    @Description("Sets the max player count for the map!")
    @CommandPermission("savethekweebecs.admin")
    private fun setMaxplayers(sender: Player, maximumPlayers: Int) {
        sender.getConfiguringMap()?.apply {
            maxPlayers = maximumPlayers

            sender.sendMessage(
                Component.text(
                    "Successfully set $maximumPlayers as the maximum for this map.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin setcountdown")
    @Description("Sets the countdown for the map!")
    @CommandPermission("savethekweebecs.admin")
    private fun setCountdown(sender: Player, countdown: Int) {
        sender.getConfiguringMap()?.apply {
            defaultCountdown = countdown

            sender.sendMessage(
                Component.text(
                    "Successfully set the default map countdown to $countdown.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin save")
    @Description("Saves the map into the config!")
    @CommandPermission("savethekweebecs.admin")
    private fun saveMap(sender: Player) {
        sender.getConfiguringMap()?.apply {
            val config = SaveTheKweebecs.getInstance().config
            val configPath = "maps.${configName}"

            config.set("$configPath.minPlayers", minPlayers)
            config.set("$configPath.maxPlayers", maxPlayers)

            config.set("$configPath.defaultCountdown", defaultCountdown)

            config.set("$configPath.mapName", mapName)
            config.set("$configPath.slimeWorld", mapName.replace(" ", "_").lowercase())

            config.set("$configPath.kidnapedPoints", kidnapedPoints?.map { it.serialize() })

            MapLocation.entries.forEach { location ->
                config.set("$configPath.${location.name.lowercase()}", mapLocations[location]?.serialize())
            }

            Team.entries.forEach { team ->
                config.set("$configPath.${team.name.lowercase()}", spawnPoints[team]?.map { it.serialize() })
            }

            main.saveConfig()

            sender.sendMessage(
                Component.text(
                    "Successfully saved the map $mapName in the config file!",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    private fun Player.getConfiguringMap(): ArenaMap? {
        val map = configuringMap[this.uniqueId]

        if (map == null) this.sendMessage(
            Component.text(
                "Create a map first using /stk admin create <mapName>",
                NamedTextColor.RED
            )
        )

        return map
    }

    private fun String.snakeToLowerCamelCase(): String {
        return snakeRegex.replace(this) {
            it.value.replace("_", "")
                .uppercase()
        }
    }
}