package me.hugo.savethekweebecs.commands

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.arena.events.ArenaEvent
import me.hugo.savethekweebecs.arena.map.ArenaMap
import me.hugo.savethekweebecs.arena.map.MapLocation
import me.hugo.savethekweebecs.arena.map.MapPoint
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import me.hugo.savethekweebecs.team.TeamManager
import me.hugo.savethekweebecs.util.menus.PaginatedMenu
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import revxrsal.commands.annotation.*
import revxrsal.commands.bukkit.annotation.CommandPermission
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Command("savethekweebecs", "stk")
@Description("Main SaveTheKweebecs command.")
class SaveTheKweebecsCommand : KoinComponent {

    private val snakeRegex = "_[a-zA-Z]".toRegex()
    private val configuringMap: MutableMap<UUID, ArenaMap> = mutableMapOf()

    private val main = SaveTheKweebecs.getInstance()

    private val gameManager: GameManager by inject()
    private val languageManager: LanguageManager by inject()
    private val scoreboardManager: ScoreboardTemplateManager by inject()
    private val teamManager: TeamManager by inject()

    @DefaultFor("savethekweebecs", "stk", "savethekweebecs help", "stk help")
    @Description("Help for the the main STK plugin.")
    private fun help(sender: Player) {
        sender.sendTranslated("system.help")
    }

    @Subcommand("auto-join")
    @Description("Auto-joins a SaveTheKweebecs map!")
    private fun autoJoin(sender: Player) {
        // Picks the fullest available arena and joins!
        gameManager.arenas.values.filter { it.teamPlayers().size < it.arenaMap.maxPlayers }
            .maxByOrNull { it.teamPlayers().size }?.joinArena(sender)
    }

    @Subcommand("arenas")
    @Description("Opens an arena selector menu!")
    private fun openArenasMenu(sender: Player) {
        gameManager.openArenasMenu(sender)
    }

    @Subcommand("transformations")
    @Description("Opens the transformations selector menu!")
    private fun openBannerSelector(sender: Player) {
        val arena = sender.arena()
        if (arena?.hasStarted() == true) return

        sender.playerData()?.transformationsMenu?.open(sender)
    }

    @Subcommand("list")
    @Description("Lists arenas.")
    private fun listArenas(sender: Player) {
        sender.sendTranslated("arena.list.header")

        gameManager.arenas.values.forEach {
            sender.sendMessage(
                sender.toComponent(
                    sender.getUnformattedLine("arena.list.member").replace("<arena_uuid>", it.arenaUUID.toString()),
                    Placeholder.unparsed("display_name", it.displayName),
                    Placeholder.component(
                        "arena_state",
                        it.arenaState.getFriendlyName(sender.playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE)
                    ),
                    Placeholder.unparsed("team_size", (it.arenaMap.maxPlayers / 2).toString()),
                    Placeholder.unparsed("map_name", it.arenaMap.mapName),
                    Placeholder.unparsed("current_players", it.teamPlayers().size.toString()),
                    Placeholder.unparsed("max_players", it.arenaMap.maxPlayers.toString())
                )
            )
        }
    }

    @Subcommand("join")
    @Description("Joins an arena.")
    private fun listArenas(sender: Player, uuid: String) {
        try {
            val arena = gameManager.arenas[UUID.fromString(uuid)]
            if (arena == null) {
                sender.sendTranslated("arena.join.noExist")
                return
            }

            arena.joinArena(sender)
        } catch (exception: IllegalArgumentException) {
            sender.sendTranslated("arena.join.noExist")
        }
    }

    @Subcommand("leave")
    @Description("Leave the arena you're in!")
    private fun leaveArena(sender: Player) {
        val currentArena = sender.playerDataOrCreate().currentArena

        if (currentArena == null) {
            sender.sendTranslated("arena.leave.notInArena")
            return
        }

        currentArena.leave(sender)
    }

    @Subcommand("shop")
    @Description("Opens the shop for the player!")
    private fun openShop(sender: Player) {
        val currentArena = sender.playerDataOrCreate().currentArena

        if (currentArena == null) {
            sender.sendTranslated("arena.leave.notInArena")
            return
        }

        val playerData = sender.playerData() ?: return
        val team = playerData.currentTeam ?: return

        if (team.shopItems.isEmpty()) return

        PaginatedMenu(
            9 * 4, "menu.shop.title", PaginatedMenu.PageFormat.TWO_ROWS_TRIMMED.format,
            ItemStack(Material.NETHER_STAR)
                .name("menu.shop.icon.name", playerData.locale)
                .putLore("menu.shop.icon.lore", playerData.locale),
            null,
            playerData.locale,
            true
        ).also { menu ->
            team.shopItems.sortedBy { it.cost }.forEach { menu.addItem(it.getIcon(sender)) }
        }.open(sender)
    }

    @DefaultFor("savethekweebecs admin", "stk admin")
    @Description("Help for the admin system.")
    @CommandPermission("savethekweebecs.admin")
    private fun helpAdmin(sender: Player) {
        sender.sendTranslated("system.admin.help")
    }

    @Subcommand("admin sethub")
    @Description("Sets the location for the main hub!")
    @CommandPermission("savethekweebecs.admin")
    private fun setHub(sender: Player) {
        main.config.set("hubLocation", MapPoint(sender.location).serialize())
        main.saveConfig()

        sender.sendMessage(
            Component.text(
                "Successfully set location for the main hub. Please restart the server.",
                NamedTextColor.GREEN
            )
        )
    }

    @Subcommand("admin settimer")
    @Description("Sets the arena timer!")
    @CommandPermission("savethekweebecs.admin")
    private fun setHub(sender: Player, seconds: Int) {
        sender.arena()?.arenaTime = seconds
    }

    @Subcommand("admin openarena")
    @Description("Creates an arena using the chosen map.")
    @CommandPermission("savethekweebecs.admin")
    private fun createArena(sender: Player, map: ArenaMap, displayName: String) {
        val arena = Arena(map, displayName)

        gameManager.registerArena(arena)

        sender.sendMessage(
            Component.text(
                "Successfully created arena in map \"${map.mapName}\" with display name \"$displayName\"!",
                NamedTextColor.GREEN
            )
        )
    }

    @DefaultFor("savethekweebecs admin kit", "stk admin kit")
    @Description("Help for the kit system.")
    @CommandPermission("savethekweebecs.admin")
    private fun helpKit(sender: Player) {
        sender.sendTranslated("system.kit.help")
    }

    @Subcommand("admin kit get")
    @Description("Gives the kit for the team!")
    @CommandPermission("savethekweebecs.admin")
    private fun getKit(sender: Player, team: TeamManager.Team) {
        val items = team.kitItems

        if (items.isEmpty()) {
            sender.sendTranslated("system.kit.noKit", Placeholder.unparsed("team", team.id))
            return
        }

        team.giveItems(sender, clearInventory = true, giveArenaItemSet = false)
    }

    @Subcommand("admin kit save")
    @Description("Saves the kit for the team!")
    @CommandPermission("savethekweebecs.admin")
    private fun saveKit(sender: Player, team: TeamManager.Team) {
        val playerItems = mutableMapOf<Int, ItemStack>()

        (0..sender.inventory.size).forEach { slot ->
            val item = sender.inventory.getItem(slot) ?: return@forEach
            if (!item.type.isAir) playerItems[slot] = item
        }

        team.kitItems = playerItems

        main.config.set("teams.${team.id}.items", null)
        playerItems.forEach { main.config.set("teams.${team.id}.items.${it.key}", it.value) }

        main.saveConfig()

        sender.sendMessage(
            Component.text(
                "Successfully saved kit for ${team.id}.",
                NamedTextColor.GREEN
            )
        )
    }

    @DefaultFor("savethekweebecs admin shop", "stk admin shop")
    @Description("Help for the shop system.")
    @CommandPermission("savethekweebecs.admin")
    private fun helpShop(sender: Player) {
        sender.sendTranslated("system.shop.help")
    }

    @Subcommand("admin shop list")
    @Description("Lists the shop items for a team!")
    @CommandPermission("savethekweebecs.admin")
    private fun listShopItems(sender: Player, team: TeamManager.Team) {
        val items = team.shopItems

        if (items.isEmpty()) {
            sender.sendTranslated("system.shop.noShop", Placeholder.unparsed("team", team.id))
            return
        }

        team.shopItems.forEach {
            sender.sendTranslated(
                "system.shop.listedItem",
                Placeholder.unparsed("key", it.key),
                Placeholder.unparsed("cost", it.cost.toString())
            )
        }
    }

    @Subcommand("admin shop add")
    @Description("Add the item in your main hand to a team's shop.")
    @CommandPermission("savethekweebecs.admin")
    private fun addShopItem(sender: Player, key: String, cost: Int, team: TeamManager.Team) {
        val items = team.shopItems

        if (items.any { it.key == key }) {
            sender.sendTranslated("system.shop.duplicateKey", Placeholder.unparsed("key", team.id))
            return
        }

        val item = sender.inventory.itemInMainHand

        if (item.type.isAir) {
            sender.sendTranslated("system.shop.noItem")
            return
        }

        team.shopItems.add(TeamManager.TeamShopItem(key, item, cost))

        val configPath = "teams.${team.id}.shop-items.$key"

        main.config.set("$configPath.cost", cost)
        main.config.set("$configPath.item", item)

        main.saveConfig()

        sender.sendTranslated(
            "system.shop.added",
            Placeholder.unparsed("key", key),
            Placeholder.unparsed("team", team.id)
        )
    }

    @Subcommand("admin shop remove")
    @Description("Remove the item from a team's shop.")
    @CommandPermission("savethekweebecs.admin")
    private fun removeShopItem(sender: Player, key: String, team: TeamManager.Team) {
        val items = team.shopItems
        val item = items.firstOrNull { it.key == key }

        if (item == null) {
            sender.sendTranslated("system.shop.itemNotFound")
            return
        }

        team.shopItems.remove(item)

        val configPath = "teams.${team.id}.shop-items.$key"
        main.config.set(configPath, null)
        main.saveConfig()

        sender.sendTranslated(
            "system.shop.removed",
            Placeholder.unparsed("key", key),
            Placeholder.unparsed("team", team.id)
        )
    }

    @Subcommand("admin shop get")
    @Description("Get the item from a team's shop.")
    @CommandPermission("savethekweebecs.admin")
    private fun getShopItem(sender: Player, key: String, team: TeamManager.Team) {
        val items = team.shopItems
        val item = items.firstOrNull { it.key == key }

        if (item == null) {
            sender.sendTranslated("system.shop.itemNotFound")
            return
        }

        sender.inventory.addItem(item.item)
    }

    @DefaultFor("savethekweebecs admin map", "stk admin map")
    @Description("Help for the map system.")
    @CommandPermission("savethekweebecs.admin")
    private fun helpMap(sender: Player) {
        sender.sendTranslated("system.map.help")
    }

    @Subcommand("admin map create")
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

    @Subcommand("admin map setlocation")
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

    @Subcommand("admin map addspawn")
    @Description("Adds the player's location to the spawnpoint list of this team!")
    @CommandPermission("savethekweebecs.admin")
    private fun addSpawnpoint(sender: Player, team: TeamManager.Team) {
        sender.getConfiguringMap()?.apply {
            teamSpawnPoints.computeIfAbsent(team.id) { mutableListOf() }.add(MapPoint(sender.location))

            sender.sendMessage(
                Component.text(
                    "Successfully added your location to the spawnpoint list of $team.",
                    NamedTextColor.GREEN
                )
            )
        }
    }

    @Subcommand("admin map addkidnaped")
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

    @Subcommand("admin map setminplayers")
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

    @Subcommand("admin map setmaxplayers")
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

    @Subcommand("admin map setcountdown")
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

    @Subcommand("admin map addevent")
    @Description("Adds an event to the map!")
    @CommandPermission("savethekweebecs.admin")
    private fun addEvent(sender: Player, event: ArenaEvent, seconds: Int) {
        sender.getConfiguringMap()?.apply {
            events.add(Pair(event, seconds))
        }
    }

    @Subcommand("admin map save")
    @Description("Saves the map into the config!")
    @CommandPermission("savethekweebecs.admin")
    private fun saveMap(sender: Player) {
        sender.getConfiguringMap()?.apply {
            if (this.events.isEmpty()) {
                sender.sendMessage(
                    Component.text(
                        "This arena has no events! Use /stk admin map addevent [event] [seconds]",
                        NamedTextColor.RED
                    )
                )
                return@apply
            }

            val config = SaveTheKweebecs.getInstance().config
            val configPath = "maps.${configName}"

            config.set("$configPath.minPlayers", minPlayers)
            config.set("$configPath.maxPlayers", maxPlayers)

            config.set("$configPath.defaultCountdown", defaultCountdown)
            config.set("$configPath.events", events.map { it.first.serialize(it.second) })

            config.set("$configPath.mapName", mapName)
            config.set("$configPath.slimeWorld", mapName.replace(" ", "_").lowercase())

            config.set("$configPath.defenderTeam", defenderTeam.id)
            config.set("$configPath.attackerTeam", attackerTeam.id)

            config.set("$configPath.kidnapedPoints", kidnapedPoints?.map { it.serialize() })

            MapLocation.entries.forEach { location ->
                config.set("$configPath.${location.name.lowercase()}", mapLocations[location]?.serialize())
            }


            teamManager.teams.values.forEach { team ->
                config.set("$configPath.${team.id.lowercase()}", teamSpawnPoints[team.id]?.map { it.serialize() })
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

    @DefaultFor("savethekweebecs admin lang", "stk admin lang")
    @Description("Help for the language system.")
    @CommandPermission("savethekweebecs.admin")
    private fun helpLang(sender: Player) {
        sender.sendTranslated("system.lang.help")
    }

    @Subcommand("admin lang reload")
    @Description("Reloads the language system.")
    @CommandPermission("savethekweebecs.admin")
    private fun reloadLang(sender: Player) {
        languageManager.reloadLanguages()
        scoreboardManager.loadTemplates()
        sender.sendTranslated("system.lang.reloaded")
    }

    @Subcommand("admin lang testMessage")
    @Description("Test messages in the language system.")
    @CommandPermission("savethekweebecs.admin")
    @AutoComplete("@locale")
    private fun testMessage(
        sender: Player,
        locale: String,
        messageKey: String
    ) {
        if (languageManager.isList(messageKey))
            languageManager.getLangStringList(messageKey, locale).map { sender.toComponent(it) }
                .forEach { sender.sendMessage(it) }
        else sender.sendMessage(sender.toComponent(languageManager.getLangString(messageKey, locale)))
    }

    @Subcommand("admin lang testTitle")
    @Description("Test titles in the language system.")
    @CommandPermission("savethekweebecs.admin")
    @AutoComplete("@locale")
    private fun testTitle(
        sender: Player,
        locale: String,
        messageKey: String,
        @Default("0.5") fadeIn: Double,
        @Default("1.5") stay: Double,
        @Default("0.5") fadeOut: Double
    ) {
        sender.showTitle(
            messageKey,
            Title.Times.times(
                fadeIn.seconds.toJavaDuration(),
                stay.seconds.toJavaDuration(),
                fadeOut.seconds.toJavaDuration()
            )
        )
    }

    @Subcommand("admin lang setlocale")
    @Description("Reloads the language system.")
    @CommandPermission("savethekweebecs.admin")
    @AutoComplete("@locale")
    private fun reloadLang(sender: Player, locale: String) {
        sender.playerData()?.locale = locale
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