package me.hugo.savethekweebecs.util.menus

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

/**
 * Item in a menu that can be clicked and
 * runs every action in [clickActions].
 */
class Icon(val item: ItemStack) {

    val clickActions: MutableList<(player: Player, clickType: ClickType) -> Unit> = mutableListOf()

    /** Adds [clickAction] to the list of actions that run when clicking this icon. */
    fun addClickAction(clickAction: (player: Player, clickType: ClickType) -> Unit): Icon {
        clickActions.add(clickAction)
        return this
    }
}