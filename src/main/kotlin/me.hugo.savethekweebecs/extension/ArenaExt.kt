package me.hugo.savethekweebecs.extension

import me.hugo.savethekweebecs.arena.Arena
import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.team.TeamManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.skinsrestorer.api.PlayerWrapper
import net.skinsrestorer.api.SkinsRestorerAPI
import org.bukkit.GameMode
import org.bukkit.Sound
import org.bukkit.entity.Player
import org.bukkit.scoreboard.Criteria
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Team
import org.koin.java.KoinJavaComponent


private val gameManager: GameManager by KoinJavaComponent.inject(GameManager::class.java)

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

            teamPlayer.sendTranslation("arena.start.${team.id}")
            SkinsRestorerAPI.getApi().applySkin(PlayerWrapper(teamPlayer), team.playerSkin)

            team.giveItems(teamPlayer)
            loadTeamColors(teamPlayer)

            spawnPointIndex = if (spawnPointIndex == spawnPoints.size - 1) 0 else spawnPointIndex + 1
        }
    }
}

fun Arena.end(winnerTeam: TeamManager.Team) {
    arenaTime = 10
    arenaState = ArenaState.FINISHING

    playersPerTeam.forEach { (_, players) ->
        players.mapNotNull { it.player() }.forEach { teamPlayer ->
            teamPlayer.reset(GameMode.ADVENTURE)
            SkinsRestorerAPI.getApi().applySkin(PlayerWrapper(teamPlayer), teamPlayer.playerDataOrCreate().playerSkin)
        }
    }

    announceTranslation("arena.win.${winnerTeam.id}")
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
    var ownTeam: Team? = scoreboard.getTeam("own")
    var enemyTeam: Team? = scoreboard.getTeam("enemy")

    if (ownTeam == null) ownTeam = scoreboard.registerNewTeam("own")
    if (enemyTeam == null) enemyTeam = scoreboard.registerNewTeam("enemy")

    ownTeam.color(NamedTextColor.GREEN)
    enemyTeam.color(NamedTextColor.RED)

    val health = scoreboard.getObjective("showHealth") ?: scoreboard.registerNewObjective(
        "showHealth",
        Criteria.HEALTH,
        Component.text("❤", NamedTextColor.RED)
    )

    health.displaySlot = DisplaySlot.BELOW_NAME

    playersPerTeam.forEach { (team, players) ->
        val isOwnTeam = team == player.playerData()?.currentTeam

        players.forEach {
            it.player()?.name?.let { playerName ->
                if (isOwnTeam) ownTeam.addEntry(playerName)
                else enemyTeam.addEntry(playerName)
            }
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
        it.player()?.sendTranslation(key, *tagResolver)
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