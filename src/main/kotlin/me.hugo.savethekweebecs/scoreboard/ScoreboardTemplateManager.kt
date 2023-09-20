package me.hugo.savethekweebecs.scoreboard

import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.ext.playerData
import me.hugo.savethekweebecs.lang.LanguageManager
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Single
class ScoreboardTemplateManager : KoinComponent {

    private val languageManager: LanguageManager by inject()
    val loadedTemplates: MutableMap<String, ScoreboardTemplate> = mutableMapOf()

    init {
        ArenaState.entries.forEach { state ->
            val friendlyName = state.name.lowercase()
            val key = "scoreboard.$friendlyName.lines"

            if (languageManager.isList(key)) {
                val scoreboardTemplate = ScoreboardTemplate(key)
                loadedTemplates[friendlyName] = scoreboardTemplate

                scoreboardTemplate.registerTag("count") { (it.playerData()?.currentArena?.arenaTime ?: 0).toString() }
                scoreboardTemplate.registerTag("time") {
                    val totalSeconds = it.playerData()?.currentArena?.arenaTime ?: 0

                    val minutes = totalSeconds / 60
                    val seconds = totalSeconds % 60

                    String.format("%02d:%02d", minutes, seconds)
                }
                scoreboardTemplate.registerTag("players") {
                    (it.playerData()?.currentArena?.teamPlayers() ?: 0).toString()
                }
            }
        }
    }

}