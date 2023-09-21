package me.hugo.savethekweebecs.player

import fr.mrmicky.fastboard.adventure.FastBoard
import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.ext.player
import me.hugo.savethekweebecs.ext.translate
import me.hugo.savethekweebecs.ext.updateBoardTags
import me.hugo.savethekweebecs.scoreboard.ScoreboardTemplateManager
import me.hugo.savethekweebecs.team.TeamManager
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.*


data class PlayerData(private val uuid: UUID) : KoinComponent {

    private val scoreboardManager: ScoreboardTemplateManager by inject()

    var currentArena: Arena? = null
    var currentTeam: TeamManager.Team? = null
    var lastAttacker: PlayerAttack? = null

    var playerSkin: IProperty? = null
    var fastBoard: FastBoard? = null

    var kills: Int = 0
        set(value) {
            field = value
            uuid.player()?.updateBoardTags("kills")
        }

    var coins: Int = 0
        set(value) {
            field = value
            uuid.player()?.updateBoardTags("coins")
        }

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

        fastBoard = FastBoard(player)
        fastBoard!!.updateTitle(player.translate("global.scoreboard.title"))
    }

    fun setLobbyBoard(player: Player) {
        scoreboardManager.loadedTemplates["lobby"]?.printBoard(player)
    }

    data class PlayerAttack(val attacker: UUID, val time: Long = System.currentTimeMillis())
}