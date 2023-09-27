package me.hugo.savethekweebecs.extension

import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.lang.LanguageManager
import me.hugo.savethekweebecs.player.PlayerData
import me.hugo.savethekweebecs.player.PlayerManager
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.koin.java.KoinJavaComponent.inject
import java.util.*

private val playerManager: PlayerManager by inject(PlayerManager::class.java)
private val languageManager: LanguageManager by inject(LanguageManager::class.java)
private val scoreboardManager: ScoreboardTemplateManager by inject(ScoreboardTemplateManager::class.java)

private val miniMessage: MiniMessage = MiniMessage.miniMessage()

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


fun Player.playSound(sound: Sound) = playSound(location, sound, 1.0f, 1.0f)

fun Player.reset(gameMode: GameMode) {
    setGameMode(gameMode)
    health = getAttribute(Attribute.GENERIC_MAX_HEALTH)?.baseValue ?: 20.0
    foodLevel = 20
    exp = 0.0f
    level = 0
    arrowsInBody = 0

    closeInventory()

    inventory.clear()
    inventory.setArmorContents(null)
}