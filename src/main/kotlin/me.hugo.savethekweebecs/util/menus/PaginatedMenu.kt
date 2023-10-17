package me.hugo.savethekweebecs.util.menus

import me.hugo.savethekweebecs.extension.name
import me.hugo.savethekweebecs.extension.putLore
import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PaginatedMenu(
    val size: Int,
    val titleTranslation: String,
    val format: String,
    var menuIcon: ItemStack,
    val previousMenu: InventoryMenu?,
    val locale: String = LanguageManager.DEFAULT_LANGUAGE
) : KoinComponent {

    private val miniMessage = MiniMessage.miniMessage()
    private val languageManager: LanguageManager by inject()

    val pages: MutableList<InventoryMenu> = mutableListOf()
    var index: Int = 0

    fun replaceFirst(replacing: ItemStack, icon: Icon): Boolean {
        var found = false

        for (menu in pages.toMutableList()) {
            val inventory = menu.inventory
            val slot = inventory.first(replacing)

            if (slot != -1) {
                menu.setIcon(slot, icon)
                found = true
                break
            }
        }

        return found
    }

    fun replaceAll(replacing: ItemStack, icon: Icon) {
        for (menuHandler in pages.toMutableList()) {
            val inventory = menuHandler.inventory

            val slot = inventory.first(replacing)

            if (slot != -1) {
                menuHandler.setIcon(slot, icon)
            }
        }
    }

    fun setItem(icon: Icon, page: Int, slot: Int) {
        if (pages.isEmpty()) createNewPage()

        pages[page].setIcon(slot, icon)
    }

    fun addItem(icon: Icon): IntArray {
        if (pages.isEmpty()) createNewPage()

        var finalSlot = -1
        var pageIndex = 0

        for (menu in pages.toMutableList()) {
            val slotList = menu.getFormatSlots()
            val lastSlot = slotList[slotList.size - 1]

            if (menu.inventory.getItem(lastSlot) == null) {
                finalSlot = menu.addItem(icon)
                break
            } else if (pages.size - 1 == pageIndex) {
                finalSlot = createNewPage().addItem(icon)
                pageIndex++
                break
            }

            pageIndex++
        }
        return intArrayOf(pageIndex, finalSlot)
    }

    fun open(player: Player) {
        player.openInventory(pages[0].inventory)
    }

    fun open(player: Player, index: Int) {
        player.openInventory(pages[index].inventory)
    }

    fun createNewPage(): InventoryMenu {
        val page: Int = index + 1
        val thisIndex: Int = index

        val previousIndex: Int = index - 1

        val newPage = InventoryMenu(
            size,
            miniMessage.deserialize(languageManager.getLangString(titleTranslation, locale)),
            format,
            menuIcon
        )

        val backButton = if (page == 1) (if (previousMenu == null) "close" else "back") else "previousPage"

        newPage.setIcon(newPage.inventory.size - 6,
            Icon(
                ItemStack(if (page == 1 && previousMenu == null) Material.DARK_OAK_DOOR else Material.ARROW)
                    .name("global.menu.$backButton.name", locale)
                    .putLore("global.menu.$backButton.lore", locale)
            ).addClickAction { player: Player, _: ClickType? ->
                if (page != 1) open(
                    player,
                    previousIndex
                ) else if (previousMenu != null) player.openInventory(previousMenu.inventory) else player.closeInventory()
                player.playSound(player.location, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f)
            })

        if (index >= 1) {
            val lastMenuHandler = pages[previousIndex]
            lastMenuHandler.setIcon(lastMenuHandler.inventory.size - 4, Icon(
                ItemStack(Material.ARROW)
                    .name("global.menu.nextPage.name", locale)
                    .putLore("global.menu.nextPage.lore", locale)
            ).addClickAction { player: Player, type: ClickType? ->
                open(player, thisIndex)
                player.playSound(player.location, Sound.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0f, 1.0f)
            })
        }

        pages.add(newPage)

        index++
        return newPage
    }

    enum class PageFormat(val format: String) {
        ONE_ROW(
            "---------"
                    + "-XXXXXXX-"
        ),
        ONE_TRIMMED(
            ("---------"
                    + "--XXXXX--")
        ),
        TWO_ROWS(
            ("---------"
                    + "-XXXXXXX-"
                    + "-XXXXXXX-")
        ),
        TWO_ROWS_TRIMMED(
            ("---------"
                    + "--XXXXX--"
                    + "--XXXXX--")
        ),
        TWO_ROWS_TO_THE_LEFT(
            ("---------"
                    + "-XXXXXX--"
                    + "-XXXXXX--")
        ),
        THREE_ROWS(
            ("---------"
                    + "-XXXXXXX-"
                    + "-XXXXXXX-"
                    + "-XXXXXXX-")
        ),
        THREE_ROWS_TRIMMED(
            ("---------"
                    + "--XXXXX--"
                    + "--XXXXX--"
                    + "--XXXXX--")
        ),
        ONE_UPGRADE(
            ("---------"
                    + "-X--X----")
        ),
        THREE_UPGRADES(
            ("---------"
                    + "-X-XXX---")
        ),
        FOUR_UPGRADES(
            ("---------"
                    + "-X-XXXX--")
        ),
        FIVE_UPGRADES(
            "---------" +
                    "-X-XXXXX-"
        ),
        SIX_UPGRADES(
            "---------" +
                    "X-XXXXXX-"
        )
    }

}