package me.hugo.savethekweebecs.util

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.extension.*
import me.hugo.savethekweebecs.lang.LanguageManager
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Item that can be clicked to run a command.
 */
class TranslatableClickableItem(val id: String, configPath: String) : KoinComponent {

    companion object {
        val CLICKABLE_ITEM_ID = NamespacedKey("stk", "clickable_item_id")
    }

    private val languageManager: LanguageManager by inject()

    // lang -> item
    private val items: MutableMap<String, ItemStack> = mutableMapOf()
    private val slot: Int

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
                .setKeyedData(CLICKABLE_ITEM_ID, PersistentDataType.STRING, id)

            items[langKey] = item
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