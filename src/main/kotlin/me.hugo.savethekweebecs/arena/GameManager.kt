package me.hugo.savethekweebecs.arena

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.arena.map.ArenaMap
import org.koin.core.annotation.Single

@Single
class GameManager {

    private val main: SaveTheKweebecs = SaveTheKweebecs.getInstance()

    private val maps: MutableMap<String, ArenaMap> = mutableMapOf()
    val arenas: MutableList<Arena> = mutableListOf()

    init {
        val mapKeys = main.config.getConfigurationSection("maps")?.getKeys(false)
        mapKeys?.forEach { maps[it] = ArenaMap(it) }
    }
}