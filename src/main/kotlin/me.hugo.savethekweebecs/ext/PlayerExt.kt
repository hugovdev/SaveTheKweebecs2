package me.hugo.savethekweebecs.ext

import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.player.PlayerManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.inject
import java.util.*

private val playerManager: PlayerManager by inject(PlayerManager::class.java)
private val languageManager: LanguageManager by inject(LanguageManager::class.java)
private val miniMessage: MiniMessage = MiniMessage.miniMessage()

fun UUID.player(): Player? = Bukkit.getPlayer(this)

fun Player.sendTranslation(key: String, vararg tagResolver: TagResolver) {
    if (languageManager.isList(key)) {
        languageManager.getLangStringList(key).forEach { sendMessage(getDeserialized(it, *tagResolver)) }
    } else sendMessage(getDeserialized(key, *tagResolver))
}

fun Player.getTranslatedLine(key: String): String {
    return languageManager.getLangString(key)
}

fun Player.getTranslationLines(key: String): List<String> {
    return languageManager.getLangStringList(key)
}

fun Player.getDeserialized(key: String, vararg tagResolver: TagResolver): Component {
    return miniMessage.deserialize(languageManager.getLangString(key), *tagResolver)
}

fun Player.playerData(): PlayerData? = playerManager.getPlayerData(this)

fun Player.playerDataOrCreate(): PlayerData = playerManager.getOrCreatePlayerData(this)

fun Player.reset(gameMode: GameMode) {
    setGameMode(gameMode)
    health = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 20.0
    foodLevel = 20
    exp = 0.0f
    level = 0

    inventory.clear()
    inventory.setArmorContents(null)
}