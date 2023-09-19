package me.hugo.savethekweebecs.lang

import me.hugo.savethekweebecs.SaveTheKweebecs
import org.bukkit.Bukkit
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.koin.core.annotation.Single
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Language system that loads messages form config.
 * Credit: MrIvanPlays
 */
@Single
class LanguageManager {

    private val main: SaveTheKweebecs = SaveTheKweebecs.getInstance()

    private var langFile: File? = null
    private var langCfg: FileConfiguration? = null

    fun isList(key: String): Boolean {
        return langCfg?.isList(key) == true
    }

    fun getLangString(key: String): String {
        return langCfg?.getString(key) ?: key
    }

    fun getLangStringList(key: String): List<String>? {
        return langCfg?.getStringList(key)
    }

    fun setupLanguageFiles() {
        langFile = File(
            main.dataFolder.toString() + File.separator + "lang" + File.separator,
            ("messages_" + main.getConfig().getString("locale", "en")?.lowercase()) + ".yml"
        )

        if (langFile?.exists() == false) {
            main.dataFolder.mkdirs()
            langFile!!.getParentFile().mkdirs()

            try {
                langFile!!.createNewFile()
            } catch (e: IOException) {
                Bukkit.getPluginManager().disablePlugin(main)
                return
            }

            javaClass.getResourceAsStream(
                ("/messages_" + main.getConfig().getString("locale", "en")?.lowercase()) + ".yml"
            )?.let {
                try {
                    Files.copy(
                        it,
                        langFile!!.getAbsoluteFile().toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: Exception) {
                    Bukkit.getPluginManager().disablePlugin(main)
                    return
                }
            }

        } else {
            main.logger.info("Using existing messages file.")
        }

        langCfg = YamlConfiguration.loadConfiguration(langFile!!)
    }

    fun reloadLanguages() {
        langCfg = YamlConfiguration.loadConfiguration(langFile!!)
    }
}