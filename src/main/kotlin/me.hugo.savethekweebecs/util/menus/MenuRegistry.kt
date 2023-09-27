package me.hugo.savethekweebecs.util.menus

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.koin.core.annotation.Single

@Single
class MenuRegistry : Listener {

    private val menus: MutableMap<Inventory, InventoryMenu> = mutableMapOf()

    fun registerMenu(inventory: Inventory, menu: InventoryMenu) {
        menus[inventory] = menu
    }

    fun unregisterMenu(menu: InventoryMenu) {
        menus.remove(menu.inventory)
    }

    @EventHandler
    private fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player ?: return
        val menu = menus[event.inventory] ?: return

        menu.getIcon(event.rawSlot)?.clickActions?.forEach { it.invoke(player, event.click) }
        event.isCancelled = true
    }
}