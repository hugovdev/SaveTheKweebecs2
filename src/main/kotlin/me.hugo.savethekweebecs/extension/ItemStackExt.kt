package me.hugo.savethekweebecs.extension

import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.block.banner.Pattern
import org.bukkit.enchantments.Enchantment
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BannerMeta
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.koin.java.KoinJavaComponent.inject
import java.util.function.Consumer

private val languageManager: LanguageManager by inject(LanguageManager::class.java)
private val miniMessage = MiniMessage.miniMessage()

fun ItemStack.amount(amount: Int): ItemStack {
    setAmount(amount)
    return this
}

fun ItemStack.nameTranslatable(key: String, locale: String, vararg tagResolvers: TagResolver): ItemStack {
    val meta = itemMeta
    meta.displayName(miniMessage.deserialize(languageManager.getLangString(key, locale), *tagResolvers))
    itemMeta = meta
    return this
}

fun ItemStack.customModelData(id: Int): ItemStack {
    val meta = itemMeta
    meta.setCustomModelData(id)
    itemMeta = meta
    return this
}

fun ItemStack.loreTranslatable(key: String, locale: String, vararg tagResolvers: TagResolver): ItemStack {
    lore(languageManager.getLangStringList(key, locale).map { miniMessage.deserialize(it, *tagResolvers) })
    return this
}

fun ItemStack.name(name: Component): ItemStack {
    val meta = itemMeta
    meta.displayName(name)
    itemMeta = meta
    return this
}

fun ItemStack.name(key: String, locale: String, vararg tagResolver: TagResolver): ItemStack {
    val meta = itemMeta
    meta.displayName(miniMessage.deserialize(languageManager.getLangString(key, locale), *tagResolver))
    itemMeta = meta
    return this
}

fun ItemStack.putLore(key: String, locale: String, vararg tagResolver: TagResolver): ItemStack {
    this.lore(languageManager.getLangStringList(key, locale).map { miniMessage.deserialize(it, *tagResolver) })
    return this
}

fun ItemStack.putLore(text: List<Component>): ItemStack {
    this.lore(text)
    return this
}

fun ItemStack.putPatterns(vararg patterns: Pattern): ItemStack {
    val meta = itemMeta as BannerMeta
    patterns.forEach { meta.addPattern(it) }
    itemMeta = meta
    return this
}

fun ItemStack.enchantment(enchantment: Enchantment?, level: Int): ItemStack {
    if (enchantment == null) return this
    addUnsafeEnchantment(enchantment, level)
    return this
}

fun ItemStack.enchantment(enchantment: Enchantment): ItemStack {
    addUnsafeEnchantment(enchantment, 1)
    return this
}

fun ItemStack.type(material: Material): ItemStack {
    type = material
    return this
}

fun ItemStack.clearEnchantments(): ItemStack {
    enchantments.keys.forEach(Consumer<Enchantment> { this.removeEnchantment(it) })
    return this
}

fun ItemStack.color(color: Color): ItemStack {
    if (type == Material.LEATHER_BOOTS
        || type == Material.LEATHER_CHESTPLATE
        || type == Material.LEATHER_HELMET
        || type == Material.LEATHER_LEGGINGS
    ) {

        val meta = itemMeta as LeatherArmorMeta
        meta.setColor(color)
        itemMeta = meta
        return this
    } else {
        throw IllegalArgumentException("Colors only applicable for leather armor!")
    }
}

fun ItemStack.flag(vararg flag: ItemFlag): ItemStack {
    val meta = itemMeta
    meta.addItemFlags(*flag)
    itemMeta = meta
    return this
}