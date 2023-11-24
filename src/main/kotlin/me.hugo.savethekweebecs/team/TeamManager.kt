package me.hugo.savethekweebecs.team

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.clickableitems.ItemSetManager
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.util.menus.Icon
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*

/**
 * Registry of every team that can be
 * played as in Save The Kweebecs.
 */
@Single
class TeamManager {

    private val main = SaveTheKweebecs.getInstance()
    val teams: Map<String, Team>

    init {
        val config = main.config

        teams = config.getConfigurationSection("teams")?.getKeys(false)
            ?.associateWith {
                val configPath = "teams.$it"

                val playerVisuals = mutableListOf<TeamVisual>()
                var defaultPlayerVisual: TeamVisual? = null

                config.getConfigurationSection("$configPath.player-visuals")?.getKeys(false)?.forEach {
                    val visualPath = "$configPath.player-visuals.$it"

                    val teamVisual = TeamVisual(
                        it,
                        SkinProperty(
                            config.getString("$visualPath.skin-texture", "")!!,
                            config.getString("$visualPath.skin-signature", "")!!
                        ),
                        config.getInt("$visualPath.headCustomId")
                    )

                    playerVisuals.add(teamVisual)

                    if (config.getBoolean("$visualPath.default", false)) {
                        defaultPlayerVisual = teamVisual
                    }
                }

                Team(
                    it,
                    playerVisuals,
                    defaultPlayerVisual!!,
                    config.getString("$configPath.chat-icon", "no-icon")!!,
                    config.getString("$configPath.team-icon", "no-icon")!!,
                    config.getInt("$configPath.transformations-menu-slot", 0),
                    config.getConfigurationSection("$configPath.items")?.getKeys(false)
                        ?.associate { slot -> slot.toInt() to config.getItemStack("$configPath.items.$slot")!! }
                        ?: mapOf(),
                    config.getConfigurationSection("$configPath.shop-items")?.getKeys(false)
                        ?.map { key ->
                            TeamShopItem(
                                key,
                                config.getItemStack("$configPath.shop-items.$key.item") ?: ItemStack(Material.BEDROCK),
                                config.getInt("$configPath.shop-items.$key.cost")
                            )
                        }?.toMutableList() ?: mutableListOf()
                )
            } ?: mapOf()
    }

    /**
     * A team that can be played as in Save The Kweebecs and all its data:
     *
     * - id
     * - List of visuals
     * - Slot in the transformations selector.
     * - Chat and team icons
     * - Items their kit has
     * - Items they can buy.
     */
    data class Team(
        val id: String,
        val visuals: List<TeamVisual>,
        val defaultPlayerVisual: TeamVisual,
        val chatIcon: String,
        val teamIcon: String,
        val transformationsMenuSlot: Int = 0,
        var kitItems: Map<Int, ItemStack> = mapOf(),
        var shopItems: MutableList<TeamShopItem> = mutableListOf()
    ) : KoinComponent {

        private val itemSetManager: ItemSetManager by inject()

        /**
         * Gives [player] the items in this team's kit.
         *
         * If [giveArenaItemSet] is true it will also give
         * the clickable item set "arena". Used mainly for the shop.
         */
        fun giveItems(player: Player, clearInventory: Boolean = false, giveArenaItemSet: Boolean = true) {
            val inventory = player.inventory

            if (clearInventory) {
                inventory.clear()
                inventory.setArmorContents(null)
            }

            kitItems.forEach { inventory.setItem(it.key, it.value) }
            if (giveArenaItemSet) itemSetManager.getSet("arena")?.forEach { it.give(player) }
        }
    }

    /** Skin data that can be used by players and NPCs. */
    data class SkinProperty(val value: String, val signature: String)

    /**
     * Item that can be bought in the shop for
     * certain [Team].
     */
    data class TeamShopItem(val key: String, val item: ItemStack, val cost: Int) {
        /**
         * Returns a clickable icon used to buy this shop item.
         */
        fun getIcon(player: Player): Icon {
            val isAvailable = (player.playerData()?.getCoins() ?: 0) >= cost
            val translatedLore = player.translateList(
                if (isAvailable)
                    "menu.shop.icon.availableLore"
                else "menu.shop.icon.notAvailableLore", Placeholder.unparsed("cost", cost.toString())
            )

            return Icon(
                ItemStack(item)
                    .putLore(item.itemMeta?.lore()?.plus(translatedLore) ?: translatedLore)
            ).addClickAction { clicker, _ ->
                val playerData = clicker.playerData() ?: return@addClickAction
                val canBuy = (clicker.playerData()?.getCoins() ?: 0) >= cost

                if (!canBuy) {
                    clicker.sendTranslated("menu.shop.poor")
                    clicker.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)

                    return@addClickAction
                }

                clicker.sendTranslated(
                    "menu.shop.item_bought",
                    Placeholder.component(
                        "item",
                        Component.text(
                            PlainTextComponentSerializer.plainText()
                                .serialize(item.itemMeta?.displayName() ?: item.displayName()), NamedTextColor.GREEN
                        )
                    ),
                    Placeholder.unparsed("amount", item.amount.toString())
                )
                playerData.addCoins(cost * -1, "bought_item")

                clicker.intelligentGive(item)
                clicker.playSound(Sound.ENTITY_EXPERIENCE_ORB_PICKUP)

                clicker.closeInventory()
            }
        }
    }

    /**
     * Skin and custom head that can be selected
     * as a transformation for certain [Team].
     */
    data class TeamVisual(val key: String, val skin: SkinProperty, val headCustomId: Int) {

        /**
         * Crafts a simple head used by this TeamVisual.
         */
        fun craftHead(teamPlayer: Player?): ItemStack {
            val locale = teamPlayer?.playerDataOrCreate()?.locale ?: LanguageManager.DEFAULT_LANGUAGE

            return ItemStack(Material.CARVED_PUMPKIN)
                .nameTranslatable("global.cosmetic.head.$key.hat_name", locale)
                .customModelData(headCustomId)
        }

        /**
         * Returns a clickable item for [playerUuid] used to select
         * this TeamVisual.
         */
        fun getIcon(playerUuid: UUID, team: Team, selected: Boolean = true): Icon {
            return Icon(getDisplayItem(playerUuid, selected)).addClickAction { player, _ ->
                val playerData = player.playerData() ?: return@addClickAction

                val oldVisual = playerData.selectedTeamVisuals[team] ?: team.defaultPlayerVisual

                if (oldVisual == this) {
                    player.sendTranslated(
                        "system.teamVisuals.alreadySelected",
                        Placeholder.component(
                            "visual_name",
                            playerUuid.translate("global.cosmetic.head.$key.selector_name")
                        )
                    )

                    player.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)

                    return@addClickAction
                }

                playerData.selectedTeamVisuals[team] = this

                player.sendTranslated(
                    "system.teamVisuals.equipped",
                    Placeholder.component(
                        "visual_name",
                        playerUuid.translate("global.cosmetic.head.$key.selector_name")
                    )
                )

                val visualsMenu = playerData.teamVisualMenu[team] ?: return@addClickAction

                visualsMenu.replaceFirst(
                    oldVisual.getDisplayItem(playerUuid, true),
                    oldVisual.getIcon(playerUuid, team, false)
                )
                visualsMenu.replaceFirst(getDisplayItem(playerUuid, false), getIcon(playerUuid, team, true))

                player.playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
            }
        }

        /**
         * Returns the visual item translated to [playerUuid]'s language,
         */
        private fun getDisplayItem(playerUuid: UUID, selected: Boolean = true): ItemStack {
            val language = playerUuid.playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE

            return ItemStack(Material.CARVED_PUMPKIN)
                .customModelData(headCustomId)
                .nameTranslatable("global.cosmetic.head.$key.selector_name", language)
                .loreTranslatable(
                    if (selected) "menu.teamVisuals.selectedLore" else
                        "menu.teamVisuals.selectLore", language, Placeholder.component(
                        "visual_name",
                        playerUuid.translate("global.cosmetic.head.$key.selector_name")
                    )
                )
                .flag(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ITEM_SPECIFICS)
                .enchantment(if (selected) Enchantment.ARROW_DAMAGE else null, 1)
        }

    }
}