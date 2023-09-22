package me.hugo.savethekweebecs.util.menus

import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack

class Icon(val item: ItemStack) {

    val clickActions: MutableList<(player: Player, clickType: ClickType) -> Unit> = mutableListOf()

    fun addClickAction(clickAction: (player: Player, clickType: ClickType) -> Unit): Icon {
        clickActions.add(clickAction)
        return this
    }
}