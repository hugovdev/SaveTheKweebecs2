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

    private val languageFiles: MutableMap<String, File> = mutableMapOf()
    private val loadedLanguages: MutableMap<String, FileConfiguration> = mutableMapOf()

    val availableLanguages: MutableSet<String>
        get() = loadedLanguages.keys

    companion object {
        const val DEFAULT_LANGUAGE = "en"
    }

    fun isList(key: String): Boolean {
        return loadedLanguages[DEFAULT_LANGUAGE]?.isList(key) == true
    }

    fun getLangString(key: String, locale: String = DEFAULT_LANGUAGE): String {
        return loadedLanguages[locale]?.getString(key) ?: loadedLanguages[DEFAULT_LANGUAGE]?.getString(key) ?: key
    }

    fun getLangStringList(key: String, locale: String = DEFAULT_LANGUAGE): List<String> {
        return loadedLanguages[locale]?.getStringList(key) ?: loadedLanguages[DEFAULT_LANGUAGE]?.getStringList(key)
        ?: listOf()
    }

    fun setupLanguageFiles() {
        val languages = main.getConfig().getStringList("locale")

        languages.forEach { currentLanguage ->
            val languageFile = File(
                main.dataFolder.toString() + File.separator + "lang" + File.separator,
                "messages_${currentLanguage.lowercase()}.yml"
            )

            languageFiles[currentLanguage] = languageFile

            if (!languageFile.exists()) {
                main.dataFolder.mkdirs()
                languageFile.getParentFile().mkdirs()

                try {
                    languageFile.createNewFile()
                } catch (e: IOException) {
                    Bukkit.getPluginManager().disablePlugin(main)
                    return
                }

                javaClass.getResourceAsStream("/messages_$currentLanguage.yml")?.let {
                    try {
                        Files.copy(
                            it,
                            languageFile.getAbsoluteFile().toPath(),
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

            loadedLanguages[currentLanguage] = YamlConfiguration.loadConfiguration(languageFiles[currentLanguage]!!)
        }
    }

    fun reloadLanguages() {
        languageFiles.forEach { loadedLanguages[it.key] = YamlConfiguration.loadConfiguration(it.value) }
    }
}