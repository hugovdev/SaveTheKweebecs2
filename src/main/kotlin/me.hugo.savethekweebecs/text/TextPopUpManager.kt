package me.hugo.savethekweebecs.text

import org.bukkit.Location
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.annotation.Single
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Single
class TextPopUpManager : BukkitRunnable() {

    private val popUps: ConcurrentMap<TextPopUp, Long> = ConcurrentHashMap()

    override fun run() {
        popUps.forEach { (popup, spawnTime) ->
            if (!popup.location.isWorldLoaded || System.currentTimeMillis() > spawnTime + popup.millisecondDuration) {
                removePopUp(popup)
            }
        }
    }

    fun createPopUp(
        viewers: List<Player>,
        textKey: String,
        location: Location,
        scale: Float = 1.0f,
        duration: Duration = 1.5.seconds,
        popupTime: Duration = 0.5.seconds
    ) {
        popUps[TextPopUp(textKey, location, viewers, duration, popupTime, scale)] = System.currentTimeMillis()
    }

    fun createPopUp(
        viewers: Player,
        textKey: String,
        location: Location,
        scale: Float = 1.0f,
        duration: Duration = 1.5.seconds,
        popupTime: Duration = 0.5.seconds
    ) {
        popUps[TextPopUp(textKey, location, listOf(viewers), duration, popupTime, scale)] = System.currentTimeMillis()
    }

    private fun removePopUp(popup: TextPopUp) {
        popUps.remove(popup)
        popup.entities.values.forEach { it.remove() }
    }

}