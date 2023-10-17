package me.hugo.savethekweebecs.team

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.util.menus.Icon
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.annotation.Single
import java.util.*

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

                val npcSkin = SkinProperty(
                    config.getString("$configPath.npc-skin-texture")!!,
                    config.getString("$configPath.npc-skin-signature")!!
                )

                Team(
                    it,
                    playerVisuals,
                    defaultPlayerVisual!!,
                    config.getInt("$configPath.transformations-menu-slot", 0),
                    npcSkin,
                    config.getConfigurationSection("$configPath.items")?.getKeys(false)
                        ?.associate { slot -> slot.toInt() to config.getItemStack("$configPath.items.$slot")!! }
                        ?: mapOf()
                )
            } ?: mapOf()
    }

    data class Team(
        val id: String,
        val visuals: List<TeamVisual>,
        val defaultPlayerVisual: TeamVisual,
        val transformationsMenuSlot: Int = 0,
        val npcTemplate: SkinProperty,
        var items: Map<Int, ItemStack> = mapOf()
    ) {
        fun giveItems(player: Player, clearInventory: Boolean = false) {
            val inventory = player.inventory

            if (clearInventory) {
                inventory.clear()
                inventory.setArmorContents(null)
            }

            items.forEach { inventory.setItem(it.key, it.value) }
        }
    }

    data class SkinProperty(val value: String, val signature: String)
    data class TeamVisual(val key: String, val skin: SkinProperty, val headCustomId: Int) {

        fun craftHead(teamPlayer: Player): ItemStack {
            val locale = teamPlayer.playerDataOrCreate().locale

            return ItemStack(Material.CARVED_PUMPKIN)
                .nameTranslatable("global.cosmetic.head.$key.hat_name", locale)
                .customModelData(headCustomId)
        }

        fun getIcon(playerUuid: UUID, team: Team, selected: Boolean = true): Icon {
            return Icon(getDisplayItem(playerUuid, selected)).addClickAction { player, _ ->
                val playerData = player.playerData() ?: return@addClickAction

                val oldVisual = playerData.selectedTeamVisuals[team] ?: team.defaultPlayerVisual
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