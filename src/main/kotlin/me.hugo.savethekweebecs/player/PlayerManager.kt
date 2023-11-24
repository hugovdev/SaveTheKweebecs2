package me.hugo.savethekweebecs.player

import org.bukkit.entity.Player
import org.koin.core.annotation.Single
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Takes care of all the player data
 * registry.
 */
@Single
class PlayerManager {

    private val playerData: ConcurrentMap<UUID, PlayerData> = ConcurrentHashMap()

    fun getPlayerData(uuid: UUID): PlayerData? {
        return playerData[uuid]
    }

    fun getOrCreatePlayerData(uuid: UUID): PlayerData {
        return playerData.computeIfAbsent(uuid) { PlayerData(uuid) }
    }

    fun getPlayerData(player: Player): PlayerData? {
        return getPlayerData(player.uniqueId)
    }

    fun getOrCreatePlayerData(player: Player): PlayerData {
        return getOrCreatePlayerData(player.uniqueId)
    }

    fun removePlayerData(uuid: UUID): PlayerData? {
        return playerData.remove(uuid)
    }
}