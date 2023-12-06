package me.hugo.savethekweebecs.scoreboard

import me.hugo.savethekweebecs.extension.playerData
import me.hugo.savethekweebecs.extension.toComponent
import me.hugo.savethekweebecs.lang.LanguageManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Representation of a scoreboard configured
 * in the language files.
 */
class ScoreboardTemplate(private val key: String) : KoinComponent {

    private val scoreboardManager: ScoreboardTemplateManager by inject()
    private val languageManager: LanguageManager by inject()

    /** Every line in the scoreboard for every language. */
    // lang -> [lines]
    private val boardLines: MutableMap<String, List<String>> = mutableMapOf()

    /** Which lines contain certain tag in each language. */
    // langKey -> [tag -> lines that contain the tag]
    private val tagLocations: MutableMap<String, MutableMap<String, List<Int>>> = mutableMapOf()
    private val usedResolvers: MutableMap<String, (player: Player) -> String> = mutableMapOf()

    /** Which tags are used in each line in each language. */
    // langKey [line -> tags]
    private val inversedTagLocations: MutableMap<String, MutableMap<Int, MutableList<String>>> = mutableMapOf()

    init {
        languageManager.availableLanguages.forEach { language ->
            val lines = languageManager.getLangStringList(key, language)
            boardLines[language] = lines

            scoreboardManager.tagResolvers.forEach { (tag, resolver) ->
                val locations = mutableListOf<Int>()

                lines.forEachIndexed { index, line ->
                    if (line.contains("<$tag>")) locations.add(index)
                    inversedTagLocations.computeIfAbsent(language) { mutableMapOf() }
                        .computeIfAbsent(index) { mutableListOf() }.add(tag)
                }

                if (locations.isNotEmpty()) {
                    tagLocations.computeIfAbsent(language) { mutableMapOf() }[tag] = locations
                    usedResolvers[tag] = resolver
                }
            }
        }
    }

    /** Prints this scoreboard to [player]. */
    fun printBoard(player: Player) {
        val playerData = player.playerData() ?: return

        val language = if (languageManager.availableLanguages.contains(playerData.locale)) playerData.locale
        else LanguageManager.DEFAULT_LANGUAGE

        val translatedResolvers = usedResolvers
            .map { tagData -> Placeholder.parsed(tagData.key, tagData.value.invoke(player)) }.toTypedArray()

        val lines = boardLines[language]!!
            .map { if (it.isEmpty()) Component.empty() else player.toComponent(it, *translatedResolvers) }

        playerData.fastBoard?.updateLines(lines)
    }

    /** Updates every line that contains any of the [tags] for [player]. */
    fun updateLinesForTag(player: Player, vararg tags: String) {
        val playerData = player.playerData() ?: return

        val locations = mutableListOf<Int>()

        val language = if (languageManager.availableLanguages.contains(playerData.locale)) playerData.locale
        else LanguageManager.DEFAULT_LANGUAGE

        tags.forEach {
            tagLocations[language]!![it]?.let { newLocations -> locations.addAll(newLocations) }
        }

        val boardLines = boardLines[language]!!

        locations.toSet().forEach {
            playerData.fastBoard?.updateLine(
                it, player.toComponent(
                    boardLines[it],
                    *inversedTagLocations[language]!![it]?.map { tag ->
                        Placeholder.parsed(tag, usedResolvers[tag]?.invoke(player) ?: tag)
                    }?.toTypedArray() ?: arrayOf()
                )
            )
        }
    }
}