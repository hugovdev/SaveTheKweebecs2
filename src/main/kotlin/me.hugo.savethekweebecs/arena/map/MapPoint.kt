package me.hugo.savethekweebecs.arena.map

import me.hugo.savethekweebecs.SaveTheKweebecs
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.abs
import kotlin.math.floor

/**
 * Object that contains the exact location of certain
 * place in a map but doesn't contain a specific world.
 */
class MapPoint(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {

    /**
     * Creates a [MapPoint] from a [location]
     *
     * If [centerToBlock] it also centers the [location]
     * given to the center of the block. Also moves the
     * yaw to the closest full rotation and locks the
     * pitch to 0.0 (looking forward).
     */
    constructor(location: Location, centerToBlock: Boolean = true) : this(
        if (centerToBlock) floor(location.x) + 0.5 else location.x,
        location.y,
        if (centerToBlock) floor(location.z) + 0.5 else location.z,
        if (centerToBlock) {
            listOf(-180.0f, -90.0f, 0.0f, 90.0f, 180.0f).minBy { v -> abs(v - location.yaw) }
        } else location.yaw,
        if (centerToBlock) 0.0f else location.pitch
    )

    companion object {
        /**
         * Returns a deserialized [MapPoint] from
         * a [serializedPoint].
         *
         * Returns null if the provided String doesn't
         * follow a [MapPoint] format.
         */
        fun deserialize(serializedPoint: String): MapPoint? {
            val split = serializedPoint.split(" , ")

            return if (split.size == 5) {
                MapPoint(
                    split[0].toDouble(), split[1].toDouble(), split[2].toDouble(),
                    split[3].toFloat(), split[4].toFloat()
                )
            } else {
                SaveTheKweebecs.getInstance().logger.info("Error while deserializing MapPoint: $serializedPoint")
                null
            }
        }

        /**
         * Fetches a string from config in the path
         * [configPath] and attempts to deserialize it.
         */
        fun deserializeFromConfig(configPath: String): MapPoint? {
            SaveTheKweebecs.getInstance().config.getString(configPath)?.let {
                return deserialize(it)
            }

            return null
        }

    }

    /**
     * Returns a bukkit location with this
     * MapPoint's coordinates in [world].
     */
    fun toLocation(world: World): Location {
        return Location(world, x, y, z, yaw, pitch)
    }

    /**
     * Serializes this MapPoint into a String with
     * format: "x , y , z , yaw , pitch".
     */
    fun serialize(): String {
        return "$x , $y , $z , $yaw , $pitch"
    }
}