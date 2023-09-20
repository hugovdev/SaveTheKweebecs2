package me.hugo.savethekweebecs.scoreboard

import fr.mrmicky.fastboard.adventure.FastBoard
import me.hugo.savethekweebecs.ext.getDeserialized
import me.hugo.savethekweebecs.ext.getTranslationLines
import me.hugo.savethekweebecs.util.DynamicValue
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import org.bukkit.entity.Player

class ValueFastBoard(player: Player) : FastBoard(player) {

    private val valuesPerLine: MutableMap<Int, DynamicValue<*>> = mutableMapOf()
    private val builders: MutableMap<DynamicValue<*>, () -> Unit> = mutableMapOf()

    fun updateLine(line: Int, value: DynamicValue<*>, componentBuilder: () -> Component) {
        super.updateLine(line, componentBuilder())

        val builder = { super.updateLine(line, componentBuilder()) }

        valuesPerLine.put(line, value)?.let { it.onChange.remove(builders[it]) }
        builders[value] = builder
        value.onChange.add(builder)
    }

    fun updateLine(
        line: Int,
        translationKey: String,
        player: Player,
        value: DynamicValue<*>,
        vararg tagResolver: () -> TagResolver
    ) {
        updateLine(line, value) {
            player.getDeserialized(
                player.getTranslationLines(translationKey)[line],
                *tagResolver.map { it.invoke() }.toTypedArray()
            )
        }
    }

    override fun removeLine(line: Int) {
        valuesPerLine.remove(line)?.let { value -> builders.remove(value)?.let { value.onChange.remove(it) } }
        super.removeLine(line)
    }

    override fun updateLines(lines: Collection<Component>) {
        valuesPerLine.forEach { it.value.onChange.remove(builders[it.value]) }

        valuesPerLine.clear()
        builders.clear()

        super.updateLines(lines)
    }

    override fun updateLine(line: Int, text: Component?) {
        valuesPerLine.remove(line)?.let { value -> builders.remove(value)?.let { value.onChange.remove(it) } }
        super.updateLine(line, text)
    }

    override fun delete() {
        valuesPerLine.forEach { it.value.onChange.remove(builders[it.value]) }

        valuesPerLine.clear()
        builders.clear()

        super.delete()
    }

}