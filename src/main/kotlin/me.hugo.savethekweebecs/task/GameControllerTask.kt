package me.hugo.savethekweebecs.task

import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.ext.*
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Sound
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class GameControllerTask : KoinComponent, BukkitRunnable() {

    private val gameManager: GameManager by inject()

    override fun run() {
        gameManager.arenas.values.forEach { arena ->
            val arenaState = arena.arenaState
            if (arenaState == ArenaState.WAITING || arenaState == ArenaState.RESETTING) return@forEach

            arena.arenaTime--

            val time = arena.arenaTime

            if (time == 0) {
                when (arenaState) {
                    ArenaState.STARTING -> arena.start()
                    ArenaState.IN_GAME -> {
                        val event = arena.currentEvent

                        if (event != null) {
                            event.eventRun.invoke(arena)
                            arena.eventIndex++

                            arena.arenaMap.events.getOrNull(arena.eventIndex)?.second?.let { arena.arenaTime = it }
                            arena.updateBoard("next_event", "time")
                        } else arena.end(arena.arenaMap.defenderTeam)
                    }

                    ArenaState.FINISHING -> arena.reset()
                    else -> {}
                }
            } else {
                arena.updateBoard("next_event", if (arenaState == ArenaState.IN_GAME) "time" else "count")

                if (time <= 60 && (time % 10 == 0 || time <= 5)) {
                    val translationName = if (arenaState != ArenaState.IN_GAME) arenaState.name.lowercase()
                    else "event.${arena.currentEvent?.name?.lowercase()}"

                    arena.playSound(Sound.BLOCK_NOTE_BLOCK_HAT)
                    arena.announceTranslation(
                        if (time == 1) "arena.$translationName.second" else "arena.$translationName.seconds",
                        Placeholder.unparsed("count", time.toString())
                    )
                }
            }
        }
    }

}