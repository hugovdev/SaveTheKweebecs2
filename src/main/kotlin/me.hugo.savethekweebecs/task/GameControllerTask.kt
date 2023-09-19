package me.hugo.savethekweebecs.task

import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.ext.announceTranslation
import me.hugo.savethekweebecs.ext.playSound
import me.hugo.savethekweebecs.ext.start
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GameControllerTask() : KoinComponent, BukkitRunnable() {

    private val gameManager: GameManager by inject()

    override fun run() {
        gameManager.arenas.values.forEach { arena ->
            if (arena.arenaState == ArenaState.WAITING) return@forEach

            val time = arena.arenaTime--

            if (time == 0) {
                when (arena.arenaState) {
                    ArenaState.STARTING -> arena.start()
                    ArenaState.IN_GAME -> TODO()
                    ArenaState.FINISHING -> TODO()
                    ArenaState.RESETTING -> TODO()
                    else -> {}
                }
            } else {
                if (arena.arenaState == ArenaState.STARTING) {
                    if (time % 10 == 0 || time <= 5) {
                        arena.playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
                        arena.announceTranslation(
                            if (time == 1) "arena.starting.second" else "arena.starting.seconds",
                            Pair("count", time.toString())
                        )
                    }
                }
            }
        }
    }

}