package me.hugo.savethekweebecs.arena.map

import me.hugo.savethekweebecs.SaveTheKweebecs
import org.bukkit.Location
import org.bukkit.World

/**
 * Object that contains the exact location of certain
 * place in a map but doesn't contain a specific world.
 */
class MapPoint(val x: Double, val y: Double, val z: Double, val yaw: Float, val pitch: Float) {

    companion object {
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

        fun deserializeFromConfig(configPath: String): MapPoint? {
            SaveTheKweebecs.getInstance().config.getString(configPath)?.let {
                return deserialize(it)
            }

            return null
        }

    }

    fun toLocation(world: World): Location {
        return Location(world, x, y, z, yaw, pitch)
    }

    fun serialize(): String {
        return "$x , $y , $z , $yaw , $pitch"
    }

}