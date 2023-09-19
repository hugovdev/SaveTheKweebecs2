package me.hugo.savethekweebecs.ext

import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.ArenaState
import net.kyori.adventure.text.Component
import net.skinsrestorer.api.PlayerWrapper
import net.skinsrestorer.api.SkinsRestorerAPI
import org.bukkit.GameMode
import org.bukkit.Sound

fun Arena.start() {
    if (this.teamPlayers().size < arenaMap.minPlayers) {
        arenaTime = arenaMap.defaultCountdown
        arenaState = ArenaState.WAITING

        announceTranslation("arena.not_enough_people")
        return
    }

    arenaTime = 300
    arenaState = ArenaState.IN_GAME

    playersPerTeam.forEach { (team, players) ->
        val spawnPointIndex = 0

        players.mapNotNull { it.player() }.forEach { teamPlayer ->
            teamPlayer.reset(GameMode.SURVIVAL)
            arenaMap.spawnPoints[team]?.get(spawnPointIndex)?.let {
                teamPlayer.teleport(it.toLocation(world!!))
            }

            teamPlayer.sendTranslation("arena.start.${team.id}")
            SkinsRestorerAPI.getApi().applySkin(PlayerWrapper(teamPlayer), team.playerSkin)
        }
    }
}

fun Arena.announce(message: Component) {
    arenaPlayers().forEach {
        it.player()?.sendMessage(message)
    }
}

fun Arena.announceTranslation(key: String, vararg parameters: Pair<String, String>) {
    arenaPlayers().forEach {
        it.player()?.sendTranslation(key, *parameters)
    }
}

fun Arena.playSound(sound: Sound) {
    arenaPlayers().forEach {
        it.player()?.let { player ->
            player.playSound(player.location, sound, 1.0f, 1.0f)
        }
    }
}

fun Arena.hasStarted(): Boolean {
    return this.arenaState != ArenaState.WAITING && this.arenaState != ArenaState.STARTING
}

fun Arena.isInGame(): Boolean {
    return this.arenaState == ArenaState.IN_GAME
}