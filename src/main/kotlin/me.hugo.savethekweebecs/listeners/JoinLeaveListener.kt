package me.hugo.savethekweebecs.listeners

import me.hugo.savethekweebecs.player.PlayerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JoinLeaveListener : KoinComponent, Listener {

    private val playerManager: PlayerManager by inject()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        val playerData = playerManager.getOrCreatePlayerData(player)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerQuitEvent) {
        val player = event.player

        playerManager.removePlayerData(player.uniqueId)?.currentArena?.leave(player)
    }
}