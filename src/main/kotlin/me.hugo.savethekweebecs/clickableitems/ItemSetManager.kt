package me.hugo.savethekweebecs.clickableitems

import com.destroystokyo.paper.MaterialSetTag
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.extension.arena
import me.hugo.savethekweebecs.extension.playerData
import me.hugo.savethekweebecs.util.TranslatableClickableItem
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.koin.core.annotation.Single

@Single
class ItemSetManager : Listener {

    private val itemActions: MutableMap<ItemStack, String> = mutableMapOf()
    private val itemSets: MutableMap<String, List<TranslatableClickableItem>> = mutableMapOf()

    fun initialize() {
        val config = SaveTheKweebecs.getInstance().config

        config.getConfigurationSection("item-sets")?.getKeys(false)?.forEach { setKey ->
            config.getConfigurationSection("item-sets.$setKey")?.getKeys(false)
                ?.map { TranslatableClickableItem("item-sets.$setKey.$it") }?.let {
                    itemSets[setKey] = it
                }
        }
    }

    fun getSet(key: String?): List<TranslatableClickableItem>? {
        return itemSets[key]
    }

    fun registerItem(item: ItemStack, command: String) {
        if (MaterialSetTag.ITEMS_BANNERS.isTagged(item.type)) return

        itemActions[item] = command
    }

    @EventHandler
    private fun onItemClick(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        val item = event.item ?: return

        val command = itemActions[item] ?: return
        event.player.chat("/$command")

        event.isCancelled = true
    }

    @EventHandler
    private fun onBannerRightClick(event: PlayerInteractEvent) {
        if (!event.action.isRightClick) return
        val player = event.player

        if (player.arena() != null) return
        val item = event.item ?: return

        if (MaterialSetTag.ITEMS_BANNERS.isTagged(item.type)) {
            event.isCancelled = true
            player.playerData()?.bannersMenu?.open(player)
        }
    }

}