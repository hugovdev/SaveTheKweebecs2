package me.hugo.savethekweebecs.ext

import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.player.PlayerManager
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.inject
import java.util.*

private val playerManager: PlayerManager by inject(PlayerManager::class.java)

fun UUID.onlinePlayer(): Player? = Bukkit.getPlayer(this)

fun Player.sendTranslation(key: String, parameters: Map<String, String>? = null) = sendMessage(key)

fun Player.playerData(): PlayerData? = playerManager.getPlayerData(this)

fun Player.playerDataOrCreate(): PlayerData = playerManager.getOrCreatePlayerData(this)

fun Player.reset(gameMode: GameMode) {
    setGameMode(gameMode)
    health = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 20.0
    foodLevel = 20
    exp = 0.0f
    level = 0
}