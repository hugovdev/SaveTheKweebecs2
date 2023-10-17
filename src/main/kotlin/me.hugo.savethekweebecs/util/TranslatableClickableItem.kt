package me.hugo.savethekweebecs.util

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.clickableitems.ItemSetManager
import me.hugo.savethekweebecs.extension.flag
import me.hugo.savethekweebecs.extension.loreTranslatable
import me.hugo.savethekweebecs.extension.nameTranslatable
import me.hugo.savethekweebecs.extension.playerData
import me.hugo.savethekweebecs.lang.LanguageManager
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TranslatableClickableItem(configPath: String) : KoinComponent {

    private val languageManager: LanguageManager by inject()
    private val itemManager: ItemSetManager by inject()

    // lang -> items
    private val items: MutableMap<String, ItemStack> = mutableMapOf()
    val slot: Int
    val command: String

    init {
        val config = SaveTheKweebecs.getInstance().config

        val material = Material.valueOf(config.getString("$configPath.material") ?: "BEDROCK")
        val nameTranslation = config.getString("$configPath.name-translation") ?: "$configPath.name-translation"
        val loreTranslation = config.getString("$configPath.lore-translation") ?: "$configPath.lore-translation"

        command = config.getString("$configPath.command") ?: "stk nocommand"
        slot = config.getInt("$configPath.slot")

        languageManager.availableLanguages.forEach { langKey ->
            val item = ItemStack(material)
                .nameTranslatable(nameTranslation, langKey)
                .loreTranslatable(loreTranslation, langKey)
                .flag(
                    ItemFlag.HIDE_ATTRIBUTES,
                    ItemFlag.HIDE_ITEM_SPECIFICS,
                    ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_DYE,
                    ItemFlag.HIDE_ARMOR_TRIM
                )

            items[langKey] = item
            itemManager.registerItem(item, command)
        }
    }

    fun give(player: Player) {
        val locale = player.playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE

        val language = if (languageManager.availableLanguages.contains(locale)) locale
        else LanguageManager.DEFAULT_LANGUAGE

        val item = items[language] ?: return

        player.inventory.setItem(slot, item)
    }

}