package me.hugo.savethekweebecs.ext

import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.ArenaState
import net.kyori.adventure.text.Component
import org.bukkit.Sound

fun Arena.announce(message: Component) {
    arenaPlayers().forEach {
        it.onlinePlayer()?.sendMessage(message)
    }
}

fun Arena.announceTranslation(key: String, parameters: Map<String, String>? = null) {
    arenaPlayers().forEach {
        it.onlinePlayer()?.sendTranslation(key, parameters)
    }
}

fun Arena.playSound(sound: Sound) {
    arenaPlayers().forEach {
        it.onlinePlayer()?.let { player ->
            player.playSound(player.location, sound, 1.0f, 1.0f)
        }
    }
}

fun Arena.hasStarted(): Boolean {
    return this.arenaState != ArenaState.WAITING && this.arenaState != ArenaState.STARTING
}