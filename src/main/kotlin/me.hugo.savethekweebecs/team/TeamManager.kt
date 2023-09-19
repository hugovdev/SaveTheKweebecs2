package me.hugo.savethekweebecs.team

import me.hugo.savethekweebecs.SaveTheKweebecs
import net.skinsrestorer.api.SkinsRestorerAPI
import net.skinsrestorer.api.property.IProperty
import org.koin.core.annotation.Single

@Single
class TeamManager {

    private val main = SaveTheKweebecs.getInstance()
    val teams: Map<String, Team>

    init {
        val skinsAPI = SkinsRestorerAPI.getApi()

        teams = main.config.getConfigurationSection("teams")?.getKeys(false)
            ?.associateWith {
                val configPath = "teams.$it"
                Team(
                    it, skinsAPI.createPlatformProperty(
                        "textures",
                        main.config.getString("$configPath.skinTexture")!!,
                        main.config.getString("$configPath.skinSignature")!!
                    ),
                    skinsAPI.createPlatformProperty(
                        "textures",
                        main.config.getString("$configPath.npcSkinTexture")!!,
                        main.config.getString("$configPath.npcSignature")!!
                    )
                )
            } ?: mapOf()
    }

    data class Team(val id: String, val playerSkin: IProperty, val npcSkin: IProperty)
}