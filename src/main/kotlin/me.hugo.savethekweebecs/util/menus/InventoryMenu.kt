package me.hugo.savethekweebecs.util.menus

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InventoryMenu(val size: Int, val title: Component, val format: String, var menuIcon: ItemStack) : KoinComponent {

    private val menuRegistry: MenuRegistry by inject()

    private val icons: MutableMap<Int, Icon> = mutableMapOf()
    private var slotIndex = 0
    val inventory: Inventory = Bukkit.createInventory(null, size, title)

    init {
        menuRegistry.registerMenu(inventory, this)

        setIcon(size - 5, Icon(menuIcon))
    }

    fun addItem(icon: Icon): Int {
        val slots: List<Int> = getFormatSlots()

        val slot = slots.getOrNull(slotIndex) ?: return -1

        icons[slot] = icon
        inventory.setItem(slot, icon.item)

        slotIndex++

        return slot
    }

    fun setIcon(position: Int, icon: Icon) {
        icons[position] = icon
        inventory.setItem(position, icon.item)
    }

    fun reset() {
        icons.clear()
        inventory.clear()
        setIcon(size - 5, Icon(menuIcon))
        slotIndex = 0
    }

    fun setIndicator(indicator: ItemStack) {
        menuIcon = indicator
        setIcon(size - 5, Icon(indicator))
    }

    fun getFormatSlots(): List<Int> {
        val charArray = format.toCharArray()
        val slots: MutableList<Int> = ArrayList()
        for (i in 0 until charArray.size - 1) {
            if (charArray[i] == 'X') {
                slots.add(i)
            }
        }
        return slots
    }

    fun getIcon(position: Int): Icon? {
        return icons[position]
    }
}