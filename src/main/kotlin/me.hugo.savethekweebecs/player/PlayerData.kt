package me.hugo.savethekweebecs.player

import com.destroystokyo.paper.profile.ProfileProperty
import fr.mrmicky.fastboard.adventure.FastBoard
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.clickableitems.ItemSetManager
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import me.hugo.savethekweebecs.team.TeamManager
import me.hugo.savethekweebecs.util.menus.Icon
import me.hugo.savethekweebecs.util.menus.MenuRegistry
import me.hugo.savethekweebecs.util.menus.PaginatedMenu
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


data class PlayerData(private val uuid: UUID) : KoinComponent {

    private val menuRegistry: MenuRegistry by inject()
    private val scoreboardManager: ScoreboardTemplateManager by inject()
    private val itemManager: ItemSetManager by inject()
    private val teamManager: TeamManager by inject()

    var currentArena: Arena? = null
    var currentTeam: TeamManager.Team? = null
    var lastAttack: PlayerAttack? = null

    var playerSkin: TeamManager.SkinProperty? = null
    var fastBoard: FastBoard? = null

    var locale: String = LanguageManager.DEFAULT_LANGUAGE
        set(newLanguage) {
            if (newLanguage == locale) return
            field = newLanguage

            val player = uuid.player() ?: return

            player.sendTranslated("system.lang.changed", Placeholder.unparsed("language", newLanguage))

            val arena = player.arena()

            if (arena != null) {
                scoreboardManager.loadedTemplates[arena.arenaState.name.lowercase()]!!.printBoard(player)
                itemManager.getSet(arena.arenaState.itemSetKey)?.forEach { it.give(player) }
            } else {
                setLobbyBoard(player)
                itemManager.getSet("lobby")?.forEach { it.give(player) }
            }

            // Rebuild the visuals menus in the new language.
            transformationsMenu = createTransformationsMenu()
            teamVisualMenu.keys.forEach { teamVisualMenu[it] = it.makeTeamVisualsMenu() }
        }

    var transformationsMenu: PaginatedMenu = createTransformationsMenu()

    val selectedTeamVisuals: MutableMap<TeamManager.Team, TeamManager.TeamVisual> =
        teamManager.teams.values.associateWith { it.defaultPlayerVisual }.toMutableMap()

    val teamVisualMenu: MutableMap<TeamManager.Team, PaginatedMenu> =
        teamManager.teams.values.associateWith { it.makeTeamVisualsMenu() }.toMutableMap()

    var kills: Int = 0
        set(value) {
            field = value
            uuid.player()?.updateBoardTags("kills")
        }

    var deaths: Int = 0
        set(value) {
            field = value
            uuid.player()?.updateBoardTags("deaths")
        }

    private var coins: Int = 0

    fun getCoins(): Int {
        return coins
    }

    fun resetCoins() {
        coins = 0
    }

    fun addCoins(amount: Int, reason: String) {
        coins += amount

        val isNegative = amount < 0
        val displayedAmount = if (isNegative) amount * -1 else amount

        uuid.player()?.let {
            it.updateBoardTags("coins")
            it.sendTranslated(
                if (isNegative) "arena.gold.minus" else "arena.gold.plus",
                Placeholder.unparsed("amount", displayedAmount.toString()),
                Placeholder.component("reason", uuid.translate("arena.gold.reason.$reason"))
            )
        }
    }

    private fun createTransformationsMenu(): PaginatedMenu {
        transformationsMenu?.let { it.pages.forEach { menuRegistry.unregisterMenu(it) } }

        val newMenu = PaginatedMenu(
            9 * 4, "menu.teamVisuals.title",
            "",
            ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .flag(
                    ItemFlag.HIDE_ARMOR_TRIM,
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ITEM_SPECIFICS,
                    ItemFlag.HIDE_ENCHANTS
                )
                .nameTranslatable("menu.teamVisuals.icon.name", locale)
                .loreTranslatable("menu.teamVisuals.icon.lore", locale),
            null, locale
        )

        teamManager.teams.values.forEach {
            newMenu.setItem(
                Icon(
                    ItemStack(Material.CARVED_PUMPKIN)
                        .customModelData(it.defaultPlayerVisual.headCustomId)
                        .nameTranslatable("menu.teamVisuals.icon.team.${it.id}.name", locale)
                        .loreTranslatable("menu.teamVisuals.icon.team.${it.id}.lore", locale)
                ).addClickAction { player, _ ->
                    teamVisualMenu[it]?.open(player)
                    player.playSound(Sound.BLOCK_CHEST_OPEN)
                }, 0, it.transformationsMenuSlot
            )
        }

        return newMenu
    }

    private fun TeamManager.Team.makeTeamVisualsMenu(): PaginatedMenu {
        if (teamVisualMenu != null) teamVisualMenu[this]?.pages?.forEach { menuRegistry.unregisterMenu(it) }

        val newMenu = PaginatedMenu(
            9 * 4, "menu.teamVisuals.title",
            PaginatedMenu.PageFormat.ONE_TRIMMED.format,
            ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE)
                .flag(
                    ItemFlag.HIDE_ARMOR_TRIM,
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ITEM_SPECIFICS,
                    ItemFlag.HIDE_ENCHANTS
                )
                .nameTranslatable("menu.teamVisuals.icon.name", locale)
                .loreTranslatable("menu.teamVisuals.icon.lore", locale),
            transformationsMenu.pages.firstOrNull(), locale
        )

        this.visuals.forEach {
            newMenu.addItem(
                it.getIcon(
                    uuid, this,
                    (selectedTeamVisuals[this] ?: this.defaultPlayerVisual) == it
                )
            )
        }

        return newMenu
    }

    fun initialize() {
        val player = uuid.player() ?: return
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard

        fastBoard = FastBoard(player)
        fastBoard!!.updateTitle(player.translate("global.scoreboard.title"))

        val textures = player.playerProfile.properties.firstOrNull { it.name == "textures" }

        if (textures == null) {
            println("Could not find textures for player ${player.name}!")
            return
        }

        playerSkin = TeamManager.SkinProperty(textures.value, textures.signature ?: "")
    }

    fun resetSkin() {
        val skin = playerSkin ?: return
        setSkin(skin)
    }

    fun setSkin(skin: TeamManager.SkinProperty) {
        val player = uuid.player() ?: return

        val profile = player.playerProfile
        profile.setProperty(ProfileProperty("textures", skin.value, skin.signature))

        player.playerProfile = profile
    }

    fun setLobbyBoard(player: Player) {
        scoreboardManager.loadedTemplates["lobby"]?.printBoard(player)
    }

    data class PlayerAttack(val attacker: UUID, val time: Long = System.currentTimeMillis())
}