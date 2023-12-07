package me.hugo.savethekweebecs.extension

import com.destroystokyo.paper.MaterialTags
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.player.PlayerManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.ArmorItem
import net.minecraft.world.item.Item
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.craftbukkit.v1_20_R2.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.koin.java.KoinJavaComponent.inject
import java.util.*

private val playerManager: PlayerManager by inject(PlayerManager::class.java)
private val languageManager: LanguageManager by inject(LanguageManager::class.java)
private val scoreboardManager: ScoreboardTemplateManager by inject(ScoreboardTemplateManager::class.java)

private val miniMessage: MiniMessage
    get() = SaveTheKweebecs.getInstance().miniMessage

fun UUID.player(): Player? = Bukkit.getPlayer(this)

fun UUID.playerData(): PlayerData? = playerManager.getPlayerData(this)

fun Player.sendTranslated(key: String, vararg tagResolver: TagResolver) {
    if (languageManager.isList(key)) {
        getUnformattedList(key).forEach { sendMessage(toComponent(it, *tagResolver)) }
    } else sendMessage(translate(key, *tagResolver))
}

fun Player.getUnformattedLine(key: String): String {
    return languageManager.getLangString(key, playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE)
}

fun Player.getUnformattedList(key: String): List<String> {
    return languageManager.getLangStringList(key, playerData()?.locale ?: LanguageManager.DEFAULT_LANGUAGE)
}

fun Player.translate(key: String, vararg tagResolver: TagResolver): Component {
    return toComponent(getUnformattedLine(key), *tagResolver)
}

fun Player.translateList(key: String, vararg tagResolver: TagResolver): List<Component> {
    return getUnformattedList(key).map { toComponent(it, *tagResolver) }
}

fun Player.toComponent(miniString: String, vararg tagResolver: TagResolver): Component {
    return miniMessage.deserialize(miniString, *tagResolver)
}

fun Player.arena(): Arena? = playerManager.getPlayerData(this)?.currentArena

fun Player.updateBoardTags(vararg tags: String) {
    val arena = arena()

    if (arena != null) {
        scoreboardManager.loadedTemplates[arena.arenaState.name.lowercase()]?.updateLinesForTag(this, *tags)
    } else scoreboardManager.loadedTemplates["lobby"]?.updateLinesForTag(this, *tags)
}

fun Player.playerDataOrCreate(): PlayerData = playerManager.getOrCreatePlayerData(this)
fun Player.playerData(): PlayerData? = playerManager.getPlayerData(this)

fun Inventory.firstIf(predicate: (ItemStack) -> Boolean): Pair<Int, ItemStack>? {
    for (slot in 0 until size) {
        val item = getItem(slot) ?: continue
        if (predicate(item)) return Pair(slot, item)
    }

    return null
}

fun Player.intelligentGive(item: ItemStack) {
    val nmsItem: Item = CraftItemStack.asNMSCopy(item).item

    val slot: Int = if (MaterialTags.HELMETS.isTagged(item)) {
        39
    } else if (MaterialTags.CHESTPLATES.isTagged(item)) {
        38
    } else if (MaterialTags.LEGGINGS.isTagged(item)) {
        37
    } else if (MaterialTags.BOOTS.isTagged(item)) {
        36
    } else if (MaterialTags.SWORDS.isTagged(item)) {
        inventory.firstIf { MaterialTags.SWORDS.isTagged(it) }?.first ?: inventory.firstEmpty()
    } else if (MaterialTags.BOWS.isTagged(item)) {
        inventory.firstIf { MaterialTags.BOWS.isTagged(it) }?.first ?: inventory.firstEmpty()
    } else inventory.firstEmpty()

    val finalSlot = if (nmsItem is ArmorItem) {
        val originalItem = inventory.getItem(slot)

        if (originalItem == null) slot
        else {
            val originalNmsItem = CraftItemStack.asNMSCopy(originalItem).item as ArmorItem?

            if (originalNmsItem == null || originalNmsItem.defense >= nmsItem.defense) {
                inventory.firstEmpty()
            } else slot
        }
    } else if (MaterialTags.SWORDS.isTagged(item)) {
        val originalItem = inventory.getItem(slot)

        if (originalItem == null) slot
        else {
            val originalNmsItem = CraftItemStack.asNMSCopy(originalItem).item

            val originalDamage =
                originalNmsItem.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND)[Attributes.ATTACK_DAMAGE].sumOf(
                    AttributeModifier::getAmount
                )

            val newDamage =
                nmsItem.getDefaultAttributeModifiers(EquipmentSlot.MAINHAND)[Attributes.ATTACK_DAMAGE].sumOf(
                    AttributeModifier::getAmount
                )

            if (originalDamage >= newDamage) {
                val originalEnchants = originalItem.itemMeta?.hasEnchants()

                if (item.itemMeta?.hasEnchants() == true && (originalEnchants == null || originalEnchants == false)) slot
                else if (originalItem.enchantments.entries.sumOf { it.value } < item.enchantments.entries.sumOf { it.value }) slot
                else null
            } else slot
        }
    } else if (MaterialTags.BOWS.isTagged(item)) {
        val originalItem = inventory.getItem(slot)

        if (originalItem == null) slot
        else {
            val originalEnchants = originalItem.itemMeta?.hasEnchants()

            if (item.itemMeta?.hasEnchants() == true && (originalEnchants == null || originalEnchants == false)) slot
            else if (originalItem.enchantments.entries.sumOf { it.value } < item.enchantments.entries.sumOf { it.value }) slot
            else null
        }
    } else null

    if (finalSlot == null) inventory.addItem(item)
    else {
        playSound(Sound.ITEM_ARMOR_EQUIP_CHAIN)
        inventory.setItem(finalSlot, item)
    }
}

fun Player.showTitle(key: String, times: Title.Times, vararg tagResolver: TagResolver) {
    if (languageManager.isList(key)) {
        val titles = getUnformattedList(key)

        val title = titles.first()
        val subtitle = titles.getOrNull(1)

        showTitle(
            Title.title(
                translate(title, *tagResolver),
                subtitle?.let { translate(it, *tagResolver) } ?: Component.empty(),
                times))
    } else showTitle(Title.title(translate(getUnformattedLine(key), *tagResolver), Component.empty(), times))
}

fun Player.playSound(sound: Sound) = playSound(location, sound, 1.0f, 1.0f)

fun Player.reset(gameMode: GameMode) {
    setGameMode(gameMode)
    health = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 20.0
    foodLevel = 20
    exp = 0.0f
    level = 0
    arrowsInBody = 0

    fireTicks = 0

    closeInventory()

    inventory.clear()
    inventory.setArmorContents(null)

    inventory.heldItemSlot = 0

    activePotionEffects.forEach { removePotionEffect(it.type) }
}