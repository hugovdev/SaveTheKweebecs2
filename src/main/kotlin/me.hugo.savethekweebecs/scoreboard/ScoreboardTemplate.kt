package me.hugo.savethekweebecs.scoreboard

import me.hugo.savethekweebecs.ext.getDeserialized
import me.hugo.savethekweebecs.ext.getTranslationLines
import me.hugo.savethekweebecs.ext.playerData
import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ScoreboardTemplate(val key: String) : KoinComponent {

    private val languageManager: LanguageManager by inject()
    private val boardLines: List<String> = languageManager.getLangStringList(key)

    private val tagResolvers: MutableMap<String, (player: Player) -> String> = mutableMapOf()
    private val tagLocations: MutableMap<String, List<Int>> = mutableMapOf()

    fun printBoard(player: Player, vararg tagResolver: TagResolver) {
        val lines = player.getTranslationLines(key)
            .map { if (it.isEmpty()) Component.empty() else player.getDeserialized(it, *tagResolver) }

        player.playerData()?.fastBoard?.updateLines(lines)
    }

    fun registerTag(tag: String, resolver: (player: Player) -> String) {
        tagResolvers[tag] = resolver

        val locations = mutableListOf<Int>()

        boardLines.forEachIndexed { index, line ->
            if (line.contains("<$tag>")) locations.add(index)
        }

        tagLocations[tag] = locations
    }

    fun updateLinesForTag(player: Player, vararg tags: String) {
        val locations = mutableListOf<Int>()

        tags.forEach { tagLocations[it]?.let { newLocations -> locations.addAll(newLocations) } }

        locations.toSet().forEach {
            var line = boardLines[it]
            tags.forEach { currentTag ->
                line = line.replace("<$currentTag>", tagResolvers[currentTag]?.invoke(player) ?: "[$currentTag]")
            }

            player.playerData()?.fastBoard?.updateLine(it, player.getDeserialized(line))
        }
    }

}