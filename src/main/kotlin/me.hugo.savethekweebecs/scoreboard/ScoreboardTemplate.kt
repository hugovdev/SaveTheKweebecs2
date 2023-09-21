package me.hugo.savethekweebecs.scoreboard

import me.hugo.savethekweebecs.ext.playerData
import me.hugo.savethekweebecs.ext.toComponent
import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScoreboardTemplate(val key: String) : KoinComponent {

    private val scoreboardManager: ScoreboardTemplateManager by inject()
    private val languageManager: LanguageManager by inject()
    private val boardLines: List<String> = languageManager.getLangStringList(key)

    private val tagLocations: MutableMap<String, List<Int>> = mutableMapOf()
    private val usedResolvers: MutableMap<String, (player: Player) -> String> = mutableMapOf()

    init {
        scoreboardManager.tagResolvers.forEach { (tag, resolver) ->
            val locations = mutableListOf<Int>()

            boardLines.forEachIndexed { index, line ->
                if (line.contains("<$tag>")) locations.add(index)
            }

            if (locations.isNotEmpty()) {
                tagLocations[tag] = locations
                usedResolvers[tag] = resolver
            }
        }
    }

    fun printBoard(player: Player) {
        val translatedResolvers = usedResolvers
            .map { tagData -> Placeholder.unparsed(tagData.key, tagData.value.invoke(player)) }.toTypedArray()

        val lines = boardLines
            .map { if (it.isEmpty()) Component.empty() else player.toComponent(it, *translatedResolvers) }

        player.playerData()?.fastBoard?.updateLines(lines)
    }

    fun updateLinesForTag(player: Player, vararg tags: String) {
        val locations = mutableListOf<Int>()

        tags.forEach { tagLocations[it]?.let { newLocations -> locations.addAll(newLocations) } }

        locations.toSet().forEach {
            player.playerData()?.fastBoard?.updateLine(
                it, player.toComponent(boardLines[it],
                    *tags.map { tag -> Placeholder.unparsed(tag, usedResolvers[tag]?.invoke(player) ?: tag) }
                        .toTypedArray()))
        }
    }
}