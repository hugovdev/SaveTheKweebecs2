package me.hugo.savethekweebecs.util.menus

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Contains every menu and what inventory it
 * belongs to.
 */
@Single
class MenuRegistry : Listener {

    private val menus: ConcurrentMap<Inventory, InventoryMenu> = ConcurrentHashMap()
    private val paginatedMenus: ConcurrentMap<InventoryMenu, PaginatedMenu> = ConcurrentHashMap()

    fun attachMenu(menu: InventoryMenu, paginatedMenu: PaginatedMenu) {
        paginatedMenus[menu] = paginatedMenu
    }

    fun registerMenu(inventory: Inventory, menu: InventoryMenu, paginatedMenu: PaginatedMenu? = null) {
        menus[inventory] = menu

        paginatedMenu?.let { paginatedMenus[menu] = it }
    }

    fun unregisterMenu(menu: InventoryMenu) {
        menus.remove(menu.inventory)
        paginatedMenus.remove(menu)
    }

    @EventHandler
    private fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as Player ?: return
        val menu = menus[event.inventory] ?: return

        menu.getIcon(event.rawSlot)?.clickActions?.forEach { it.invoke(player, event.click) }
        event.isCancelled = true
    }

    @EventHandler
    private fun onInventoryClose(event: InventoryCloseEvent) {
        val menu = menus[event.inventory] ?: return

        if (!menu.disposable) return

        val paginatedMenu = paginatedMenus[menu]

        if (paginatedMenu != null) {
            paginatedMenu.pages.forEach { unregisterMenu(it) }
        } else unregisterMenu(menu)
    }
}