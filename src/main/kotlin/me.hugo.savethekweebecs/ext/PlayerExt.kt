package me.hugo.savethekweebecs.ext

import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.player.PlayerManager
import net.kyori.adventure.text.minimessage.MiniMessage
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

fun Player.sendTranslation(key: String, vararg parameters: Pair<String, String>) {
    if (languageManager.isList(key)) {
        languageManager.getLangStringList(key)?.forEach { sendFormattedMessage(it, *parameters) }
    } else sendFormattedMessage(languageManager.getLangString(key), *parameters)
}

fun Player.sendFormattedMessage(message: String, vararg parameters: Pair<String, String>) {
    var finalMessage = message
    parameters.forEach { (parameterName, result) -> finalMessage = finalMessage.replace("{$parameterName}", result) }

    sendMessage(miniMessage.deserialize(finalMessage))
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