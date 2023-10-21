package me.hugo.savethekweebecs.music

import me.hugo.savethekweebecs.extension.player
import me.hugo.savethekweebecs.extension.translate
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import org.koin.core.annotation.Single
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Single
class SoundManager : BukkitRunnable() {

    val inGameMusic = MusicTrack("music.save_the_kweebecs", 51.seconds)

    private var timeSinceNotification: Int = 0
    private val musicPlayers: ConcurrentMap<UUID, PlaybackData> = ConcurrentHashMap()

    override fun run() {
        val notify = timeSinceNotification >= 15

        musicPlayers.forEach { (uuid, playbackData) ->
            if (playbackData.showTrackStatus && notify) uuid.player()?.let { sendPlayingNotification(it) }

            if (playbackData.startTime + playbackData.track.duration.inWholeMilliseconds <= System.currentTimeMillis()) {
                musicPlayers[uuid] =
                    PlaybackData(playbackData.track, System.currentTimeMillis(), playbackData.showTrackStatus)
                val player = uuid.player()

                if (player != null) {
                    player.playSound(playbackData.track.sound)
                } else {
                    musicPlayers.remove(uuid)
                }
            }
        }

        if (notify) timeSinceNotification = 0
        else timeSinceNotification++
    }

    fun playTrack(track: MusicTrack, player: Player, showTrackStatus: Boolean = true) {
        // Stop the current track before playing a new one!
        stopTrack(player)

        player.playSound(track.sound)
        musicPlayers[player.uniqueId] = PlaybackData(track, System.currentTimeMillis(), showTrackStatus)

        sendPlayingNotification(player)
    }

    fun playSoundEffect(name: String, player: Player) {
        player.playSound(Sound.sound(Key.key(name), Sound.Source.AMBIENT, 1.0f, 1.0f))
    }

    private fun sendPlayingNotification(player: Player) {
        val track = musicPlayers[player.uniqueId]?.track ?: return

        player.sendActionBar(
            player.translate(
                "global.sound.music.now_playing",
                Placeholder.component("track_name", player.translate("global.sound.${track.trackId}.name")),
                Placeholder.component("track_author", player.translate("global.sound.${track.trackId}.author")),
            )
        )
    }

    fun stopTrack(player: Player) {
        musicPlayers.remove(player.uniqueId)?.track?.sound?.let { player.stopSound(it) }
    }

    data class PlaybackData(val track: MusicTrack, val startTime: Long, val showTrackStatus: Boolean = true)
    data class MusicTrack(val trackId: String, val duration: Duration) {
        val sound = Sound.sound(Key.key(trackId), Sound.Source.RECORD, 1.0f, 1.0f)
    }
}