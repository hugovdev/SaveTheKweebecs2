package me.hugo.savethekweebecs.clickableitems

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.extension.getKeyedData
import me.hugo.savethekweebecs.util.TranslatableClickableItem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.persistence.PersistentDataType
import org.koin.core.annotation.Single

@Single
class ItemSetManager : Listener {

    // ClickableItem id -> ClickableItem
    private val clickableItems: MutableMap<String, TranslatableClickableItem> = mutableMapOf()
    private val itemSets: MutableMap<String, List<TranslatableClickableItem>> = mutableMapOf()

    fun initialize() {
        val config = SaveTheKweebecs.getInstance().config

        config.getConfigurationSection("item-sets")?.getKeys(false)?.forEach { setKey ->
            config.getConfigurationSection("item-sets.$setKey")?.getKeys(false)
                ?.map {
                    TranslatableClickableItem("$setKey/$it", "item-sets.$setKey.$it").also { clickableItem ->
                        clickableItems[clickableItem.id] = clickableItem
                    }
                }?.let {
                    itemSets[setKey] = it
                }
        }
    }

    fun getSet(key: String?): List<TranslatableClickableItem>? {
        return itemSets[key]
    }

    @EventHandler
    private fun onItemClick(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        val item = event.item ?: return

        val clickableItemId =
            item.getKeyedData(TranslatableClickableItem.CLICKABLE_ITEM_ID, PersistentDataType.STRING) ?: return
        val clickableItem = clickableItems[clickableItemId] ?: return

        event.player.chat("/${clickableItem.command}")

        event.isCancelled = true
    }

}