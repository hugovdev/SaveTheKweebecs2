package me.hugo.savethekweebecs.scoreboard

import me.hugo.savethekweebecs.arena.ArenaState
import me.hugo.savethekweebecs.ext.arena
import me.hugo.savethekweebecs.ext.getUnformattedLine
import me.hugo.savethekweebecs.ext.playerDataOrCreate
import me.hugo.savethekweebecs.lang.LanguageManager
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.koin.core.annotation.Single
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Single
class ScoreboardTemplateManager : KoinComponent {

    private val languageManager: LanguageManager by inject()
    val loadedTemplates: MutableMap<String, ScoreboardTemplate> = mutableMapOf()
    val tagResolvers: MutableMap<String, (player: Player) -> String> = mutableMapOf()

    fun initialize() {
        registerTags()
        loadTemplates()
    }

    fun loadTemplates() {
        ArenaState.entries.forEach { state ->
            val friendlyName = state.name.lowercase()
            val key = "scoreboard.$friendlyName.lines"

            if (languageManager.isList(key)) {
                loadedTemplates[friendlyName] = ScoreboardTemplate(key)
            }
        }

        loadedTemplates["lobby"] = ScoreboardTemplate("scoreboard.lobby.lines")
    }

    private fun registerTags() {
        registerTag("date") { DateTimeFormatter.ofPattern("MM/dd/yyyy").format(LocalDateTime.now()) }

        registerTag("all_players") { Bukkit.getOnlinePlayers().size.toString() }

        registerTag("count") { (it.arena()?.arenaTime ?: 0).toString() }

        registerTag("npcs_saved") {
            (it.arena()?.remainingNPCs?.count { it.value } ?: 0).toString()
        }
        registerTag("total_npcs") { (it.arena()?.remainingNPCs?.size ?: 0).toString() }

        registerTag("next_event") {
            it.getUnformattedLine("arena.event.${it.arena()?.currentEvent?.name?.lowercase() ?: "unknown"}.name")
        }

        registerTag("coins") { it.playerDataOrCreate().coins.toString() }

        registerTag("kills") { it.playerDataOrCreate().kills.toString() }

        registerTag("time") {
            val totalSeconds = it.arena()?.arenaTime ?: 0

            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60

            String.format("%02d:%02d", minutes, seconds)
        }

        registerTag("players") { (it.arena()?.teamPlayers()?.size ?: 0).toString() }
        registerTag("max_players") { (it.arena()?.arenaMap?.maxPlayers ?: 0).toString() }

        registerTag("map_name") { (it.arena()?.arenaMap?.mapName ?: 0).toString() }
        registerTag("display_name") { (it.arena()?.displayName ?: 0).toString() }
    }

    private fun registerTag(tag: String, resolver: (player: Player) -> String) {
        tagResolvers[tag] = resolver
    }
}