package me.hugo.savethekweebecs.text

import me.hugo.savethekweebecs.SaveTheKweebecs
import me.hugo.savethekweebecs.extension.player
import me.hugo.savethekweebecs.extension.translate
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.entity.Display
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import java.util.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class TextPopUp(
    private val textKey: String,
    val location: Location,
    viewers: List<Player>,
    duration: Duration = 1.5.seconds,
    popupTime: Duration = 0.5.seconds,
    scale: Float = 1.0f,
) {

    val popupMilliseconds = popupTime.inWholeMilliseconds
    val millisecondDuration = duration.inWholeMilliseconds

    val entities: Map<UUID, TextDisplay>

    init {
        entities = viewers.map { it.uniqueId }.associateWith {
            val player = it.player()
            val textDisplay =
                location.world.spawnEntity(location, EntityType.TEXT_DISPLAY, CreatureSpawnEvent.SpawnReason.CUSTOM)
                { entity ->
                    entity as TextDisplay

                    entity.isVisibleByDefault = false

                    entity.isDefaultBackground = false
                    entity.backgroundColor = Color.fromARGB(0, 255, 255, 255)

                    entity.billboard = Display.Billboard.CENTER
                    entity.text(it.translate(textKey))
                    entity.brightness = Display.Brightness(15, 15)

                    entity.transformation = Transformation(Vector3f(), AxisAngle4f(), Vector3f(), AxisAngle4f())
                    entity.interpolationDuration = (popupMilliseconds * 0.02).toInt()
                    entity.interpolationDelay = -1
                } as TextDisplay

            player?.showEntity(SaveTheKweebecs.getInstance(), textDisplay)

            textDisplay
        }

        object : BukkitRunnable() {
            override fun run() {
                entities.values.forEach {
                    it.transformation = Transformation(Vector3f(), AxisAngle4f(), Vector3f(scale), AxisAngle4f())
                }
            }
        }.runTaskLater(SaveTheKweebecs.getInstance(), 2L)
    }

}