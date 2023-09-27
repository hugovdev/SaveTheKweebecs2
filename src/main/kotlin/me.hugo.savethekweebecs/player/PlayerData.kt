package me.hugo.savethekweebecs.player

import fr.mrmicky.fastboard.adventure.FastBoard
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.clickableitems.ItemSetManager
import me.hugo.savethekweebecs.cosmetic.BannerCosmetic
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import me.hugo.savethekweebecs.team.TeamManager
import me.hugo.savethekweebecs.util.menus.MenuRegistry
import me.hugo.savethekweebecs.util.menus.PaginatedMenu
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


data class PlayerData(private val uuid: UUID) : KoinComponent {

    private val menuRegistry: MenuRegistry by inject()
    private val scoreboardManager: ScoreboardTemplateManager by inject()
    private val itemManager: ItemSetManager by inject()

    var currentArena: Arena? = null
    var currentTeam: TeamManager.Team? = null
    var lastAttack: PlayerAttack? = null

    var playerSkin: IProperty? = null
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

            // Rebuild the banner menu in the new language.
            makeBannersMenu()
        }

    var bannersMenu: PaginatedMenu? = makeBannersMenu()

    var bannerCosmetic: BannerCosmetic = BannerCosmetic.NONE
        set(banner) {
            val old = field
            field = banner

            bannersMenu?.replaceFirst(old.getDisplayItem(uuid, true), old.getIcon(uuid, false))
            bannersMenu?.replaceFirst(banner.getDisplayItem(uuid, false), banner.getIcon(uuid, true))
        }

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

    var coins: Int = 0
        set(value) {
            field = value
            uuid.player()?.updateBoardTags("coins")
        }

    init {
        // Save the player skin for later!
        object : BukkitRunnable() {
            override fun run() {
                playerSkin = SkinsRestorerAPI.getApi().getProfile(uuid.toString())
            }
        }.runTaskAsynchronously(SaveTheKweebecs.getInstance())
    }

    private fun makeBannersMenu(): PaginatedMenu? {
        bannersMenu?.pages?.forEach { menuRegistry.unregisterMenu(it) }

        bannersMenu = PaginatedMenu(
            9 * 4, "menu.banners.title",
            PaginatedMenu.PageFormat.TWO_ROWS_TRIMMED.format,
            ItemStack(Material.WHITE_BANNER)
                .nameTranslatable("menu.banners.icon.name", locale)
                .loreTranslatable("menu.banners.icon.lore", locale)
            ,
            null, locale
        )

        BannerCosmetic.entries.forEach { bannersMenu!!.addItem(it.getIcon(uuid, bannerCosmetic == it)) }

        return bannersMenu
    }

    fun initBoard() {
        val player = uuid.player() ?: return
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard

        fastBoard = FastBoard(player)
        fastBoard!!.updateTitle(player.translate("global.scoreboard.title"))
    }

    fun setLobbyBoard(player: Player) {
        scoreboardManager.loadedTemplates["lobby"]?.printBoard(player)
    }

    data class PlayerAttack(val attacker: UUID, val time: Long = System.currentTimeMillis())
}