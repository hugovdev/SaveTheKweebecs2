package me.hugo.savethekweebecs.listeners

import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.ext.playerData
import me.hugo.savethekweebecs.ext.playerDataOrCreate
import me.hugo.savethekweebecs.player.PlayerManager
import me.hugo.savethekweebecs.util.DynamicValue
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class JoinLeaveListener : KoinComponent, Listener {

    private val playerManager: PlayerManager by inject()
    private val gameManager: GameManager by inject()

    val onlinePlayers: DynamicValue<Int> = DynamicValue(Bukkit.getOnlinePlayers().size)

    @EventHandler
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        // Create player data and save their skin!
        playerManager.getOrCreatePlayerData(event.uniqueId)
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        event.joinMessage(null)
        val player = event.player

        gameManager.sendToHub(player)
        player.playerData()?.initBoard()
        onlinePlayers.value++
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        event.quitMessage(null)
        val player = event.player

        player.playerDataOrCreate().currentArena?.leave(player)
        playerManager.removePlayerData(player.uniqueId)
        onlinePlayers.value--
    }
}