package me.hugo.savethekweebecs.extension

import io.papermc.paper.adventure.PaperAdventure
import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.music.SoundManager
import me.hugo.savethekweebecs.team.TeamManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.title.Title
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.world.scores.DisplaySlot
import net.minecraft.world.scores.Objective
import net.minecraft.world.scores.criteria.ObjectiveCriteria
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.craftbukkit.v1_20_R2.entity.CraftPlayer
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Team
import org.koin.java.KoinJavaComponent
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val gameManager: GameManager by KoinJavaComponent.inject(GameManager::class.java)
private val soundManager: SoundManager by KoinJavaComponent.inject(SoundManager::class.java)

fun Arena.start() {
    if (this.teamPlayers().size < arenaMap.minPlayers) {
        arenaTime = arenaMap.defaultCountdown
        arenaState = ArenaState.WAITING

        announceTranslation("arena.notEnoughPeople")
        return
    }

    arenaTime = 300
    arenaState = ArenaState.IN_GAME

    playersPerTeam.forEach { (team, players) ->
        var spawnPointIndex = 0
        val spawnPoints = arenaMap.spawnPoints[team.id]

        players.mapNotNull { it.player() }.forEach { teamPlayer ->
            teamPlayer.reset(GameMode.SURVIVAL)

            println("${spawnPoints?.size} spawns! [${team.id}] - $spawnPointIndex")
            teamPlayer.teleport(spawnPoints!![spawnPointIndex].toLocation(world!!))

            teamPlayer.sendTranslated(
                "arena.start.${team.id}",
                Placeholder.unparsed("team_icon", team.chatIcon)
            )

            teamPlayer.playSound(Sound.ENTITY_ENDERMAN_TELEPORT)
            teamPlayer.showTitle(
                "arena.start.title",
                Title.Times.times(
                    0.5.seconds.toJavaDuration(),
                    2.0.seconds.toJavaDuration(),
                    0.5.seconds.toJavaDuration()
                )
            )

            team.giveItems(teamPlayer)

            val playerData = teamPlayer.playerDataOrCreate()

            val selectedVisual = playerData.selectedTeamVisuals[team] ?: team.defaultPlayerVisual

            teamPlayer.inventory.helmet = selectedVisual.craftHead(teamPlayer)
            loadTeamColors(teamPlayer)

            playerData.setSkin(selectedVisual.skin)

            soundManager.playTrack(soundManager.inGameMusic, teamPlayer)
            spawnPointIndex = if (spawnPointIndex == spawnPoints.size - 1) 0 else spawnPointIndex + 1
        }
    }
}

fun Arena.end(winnerTeam: TeamManager.Team) {
    this.winnerTeam = winnerTeam
    this.arenaTime = 10
    this.arenaState = ArenaState.FINISHING

    playersPerTeam.forEach { (_, players) ->
        players.mapNotNull { it.player() }.forEach { teamPlayer ->
            teamPlayer.reset(GameMode.ADVENTURE)

            soundManager.stopTrack(teamPlayer)
            soundManager.playSoundEffect("save_the_kweebecs.victory", teamPlayer)

            teamPlayer.showTitle(
                if (winnerTeam == teamPlayer.playerData()?.currentTeam) "arena.win.title"
                else "arena.lost.title",
                Title.Times.times(
                    0.2.seconds.toJavaDuration(),
                    3.5.seconds.toJavaDuration(),
                    0.2.seconds.toJavaDuration()
                )
            )

            val playerData = teamPlayer.playerData() ?: return
            playerData.resetSkin()
        }
    }

    announceTranslation(
        "arena.win.${winnerTeam.id}",
        Placeholder.unparsed("team_icon", winnerTeam.chatIcon)
    )
}

fun Arena.reset() {
    arenaState = ArenaState.RESETTING

    arenaPlayers().mapNotNull { it.player() }.forEach { player ->
        player.playerData()?.apply {
            currentArena = null
            currentTeam = null
        }
        gameManager.sendToHub(player)
    }

    loadMap(false)
}

fun Arena.loadTeamColors(player: Player) {
    val scoreboard = player.scoreboard

    val playerTeam = player.playerDataOrCreate().currentTeam
    val attackerTeam = arenaMap.attackerTeam
    val defenderTeam = arenaMap.defenderTeam

    val hisTeam = if (attackerTeam == playerTeam) attackerTeam else defenderTeam
    val otherTeam = if (attackerTeam != playerTeam) attackerTeam else defenderTeam

    val ownTeam: Team = (scoreboard.getTeam("own") ?: scoreboard.registerNewTeam("own")).apply {
        color(NamedTextColor.GREEN)
        suffix(Component.text(" ${hisTeam.teamIcon}").color(NamedTextColor.WHITE))
    }

    val enemyTeam: Team = (scoreboard.getTeam("enemy") ?: scoreboard.registerNewTeam("enemy")).apply {
        color(NamedTextColor.RED)
        suffix(Component.text(" ${otherTeam.teamIcon}").color(NamedTextColor.WHITE))
    }

    val serverPlayer = (player as CraftPlayer).handle

    val objective = player.playerData()?.healthObjective ?: Objective(
        serverPlayer.scoreboard, "showHealth", ObjectiveCriteria.DUMMY,
        PaperAdventure.asVanilla(Component.text("❤", NamedTextColor.RED)), ObjectiveCriteria.RenderType.INTEGER
    ).also { player.playerData()?.healthObjective = it }

    serverPlayer.connection.send(ClientboundSetObjectivePacket(objective, ClientboundSetObjectivePacket.METHOD_ADD))
    serverPlayer.connection.send(ClientboundSetDisplayObjectivePacket(DisplaySlot.BELOW_NAME, objective))

    playersPerTeam.forEach { (team, players) ->
        val isOwnTeam = team == player.playerData()?.currentTeam

        players.forEach {
            it.player()?.name?.let { playerName ->
                if (isOwnTeam) ownTeam.addEntry(playerName)
                else enemyTeam.addEntry(playerName)
            }

            it.player()?.updateHealth(20)
        }
    }
}

fun Arena.announce(message: Component) {
    arenaPlayers().forEach {
        it.player()?.sendMessage(message)
    }
}

fun Arena.announceTranslation(key: String, vararg tagResolver: TagResolver) {
    arenaPlayers().forEach {
        it.player()?.sendTranslated(key, *tagResolver)
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