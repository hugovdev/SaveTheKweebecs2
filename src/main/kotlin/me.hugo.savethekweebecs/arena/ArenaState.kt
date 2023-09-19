package me.hugo.savethekweebecs.arena

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

enum class ArenaState(val color: TextColor) {

    WAITING(NamedTextColor.GREEN), STARTING(NamedTextColor.GOLD), IN_GAME(NamedTextColor.RED), FINISHING(NamedTextColor.RED), RESETTING(
        NamedTextColor.AQUA
    )

}