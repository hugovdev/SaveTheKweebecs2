package me.hugo.savethekweebecs.player

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.ext.getDeserialized
import me.hugo.savethekweebecs.ext.getTranslationLines
import me.hugo.savethekweebecs.ext.player
import me.hugo.savethekweebecs.listeners.JoinLeaveListener
import me.hugo.savethekweebecs.scoreboard.ValueFastBoard
import me.hugo.savethekweebecs.team.TeamManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Formatter
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*


data class PlayerData(private val uuid: UUID) : KoinComponent {

    private val joinLeaveListener: JoinLeaveListener by inject()

    var currentArena: Arena? = null
    var currentTeam: TeamManager.Team? = null
    var lastAttacker: PlayerAttack? = null

    var playerSkin: IProperty? = null
    var fastBoard: ValueFastBoard? = null

    init {
        // Save the player skin for later!
        object : BukkitRunnable() {
            override fun run() {
                playerSkin = SkinsRestorerAPI.getApi().getProfile(uuid.toString())
            }
        }.runTaskAsynchronously(SaveTheKweebecs.getInstance())
    }

    fun initBoard() {
        val player = uuid.player() ?: return
        player.scoreboard = Bukkit.getScoreboardManager().newScoreboard

        fastBoard = ValueFastBoard(player)

        fastBoard!!.updateTitle(player.getDeserialized("global.scoreboard.title"))

        val tagResolver: Array<TagResolver> = arrayOf(
            Formatter.date("date", LocalDateTime.now(ZoneId.systemDefault())),
            Placeholder.unparsed("players", Bukkit.getOnlinePlayers().size.toString())
        )

        val lines = player.getTranslationLines("scoreboard.lobby.lines")
            .map { if (it.isEmpty()) Component.empty() else player.getDeserialized(it, *tagResolver) }

        fastBoard!!.updateLines(lines)

        fastBoard!!.updateLine(7, joinLeaveListener.onlinePlayers) {
            player.getDeserialized(
                player.getTranslationLines("scoreboard.lobby.lines")[7],
                Placeholder.unparsed("players", joinLeaveListener.onlinePlayers.value.toString())
            )
        }
    }

    fun setScoreboard(key: String, vararg tagResolver: TagResolver) {
        val player = uuid.player() ?: return

        val lines = player.getTranslationLines(key)
            .map { if (it.isEmpty()) Component.empty() else player.getDeserialized(it, *tagResolver) }

        fastBoard!!.updateLines(lines)
    }

    data class PlayerAttack(val attacker: UUID, val time: Long = System.currentTimeMillis())
}