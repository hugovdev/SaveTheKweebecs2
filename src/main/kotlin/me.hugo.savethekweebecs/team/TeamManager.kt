package me.hugo.savethekweebecs.team

import me.hugo.savethekweebecs.SaveTheKweebecs
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.koin.core.annotation.Single

@Single
class TeamManager {

    private val main = SaveTheKweebecs.getInstance()
    val teams: Map<String, Team>

    init {
        val skinsAPI = SkinsRestorerAPI.getApi()

        val config = main.config

        teams = config.getConfigurationSection("teams")?.getKeys(false)
            ?.associateWith {
                val configPath = "teams.$it"
                Team(
                    it, skinsAPI.createPlatformProperty(
                        "textures",
                        config.getString("$configPath.skinTexture")!!,
                        config.getString("$configPath.skinSignature")!!
                    ),
                    skinsAPI.createPlatformProperty(
                        "textures",
                        config.getString("$configPath.npcSkinTexture")!!,
                        config.getString("$configPath.npcSignature")!!
                    ),
                    config.getConfigurationSection("$configPath.items")?.getKeys(false)
                        ?.associate { slot -> slot.toInt() to config.getItemStack("$configPath.items.$slot")!! }
                        ?: mapOf()
                )
            } ?: mapOf()
    }

    data class Team(
        val id: String,
        val playerSkin: IProperty,
        val npcTemplate: IProperty,
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
}