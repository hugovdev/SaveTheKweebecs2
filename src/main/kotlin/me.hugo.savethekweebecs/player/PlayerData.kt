package me.hugo.savethekweebecs.player

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.Team
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.bukkit.scheduler.BukkitRunnable
import java.util.*


data class PlayerData(private val uuid: UUID) {

    var currentArena: Arena? = null
    var currentTeam: Team? = null
    var lastAttacker: PlayerAttack? = null

    var playerSkin: IProperty? = null

    init {
        // Save the player skin for later!
        object : BukkitRunnable() {
            override fun run() {
                playerSkin = SkinsRestorerAPI.getApi().getProfile(uuid.toString())
            }
        }.runTaskAsynchronously(SaveTheKweebecs.getInstance())
    }

    data class PlayerAttack(val attacker: UUID, val time: Long = System.currentTimeMillis())
}