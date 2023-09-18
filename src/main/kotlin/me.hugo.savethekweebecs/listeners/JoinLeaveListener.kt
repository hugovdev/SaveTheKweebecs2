package me.hugo.savethekweebecs.listeners

import me.hugo.savethekweebecs.player.PlayerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JoinLeaveListener : KoinComponent, Listener {

    private val playerManager: PlayerManager by inject()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player

        val playerData = playerManager.getOrCreatePlayerData(player)
    }
}