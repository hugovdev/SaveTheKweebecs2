package me.hugo.savethekweebecs.arena

import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import org.bukkit.Material
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

enum class ArenaState(val color: TextColor, val material: Material, val itemSetKey: String? = null) : KoinComponent {

    WAITING(NamedTextColor.GREEN, Material.LIME_CONCRETE, "arena-lobby"),
    STARTING(NamedTextColor.GOLD, Material.YELLOW_CONCRETE, "arena-lobby"),
    IN_GAME(NamedTextColor.RED, Material.ORANGE_CONCRETE),
    FINISHING(NamedTextColor.RED, Material.RED_CONCRETE, "arena-lobby"),
    RESETTING(NamedTextColor.AQUA, Material.BLACK_CONCRETE);

    private val languageManager: LanguageManager by inject()
    private val miniMessage = MiniMessage.miniMessage()

    fun getFriendlyName(locale: String): Component {
        return miniMessage.deserialize(languageManager.getLangString("arena.state.${this.name.lowercase()}", locale))
    }

}