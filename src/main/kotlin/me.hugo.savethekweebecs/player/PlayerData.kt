package me.hugo.savethekweebecs.player

import me.hugo.savethekweebecs.arena.Arena
import java.util.UUID

data class PlayerData(private val uuid: UUID) {

    var currentArena: Arena? = null

}