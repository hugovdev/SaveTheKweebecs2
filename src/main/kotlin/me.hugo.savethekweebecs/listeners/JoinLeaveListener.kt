package me.hugo.savethekweebecs.listeners

import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.ext.playerDataOrCreate
import me.hugo.savethekweebecs.player.PlayerManager
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class JoinLeaveListener : KoinComponent, Listener {

    private val playerManager: PlayerManager by inject()
    private val gameManager: GameManager by inject()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)
        val player = event.player

        gameManager.sendToHub(player)
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)
        val player = event.player

        player.playerDataOrCreate().currentArena?.leave(player)
        playerManager.removePlayerData(player.uniqueId)
    }
}