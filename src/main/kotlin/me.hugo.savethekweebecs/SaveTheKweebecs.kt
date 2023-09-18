package me.hugo.savethekweebecs

import com.infernalsuite.aswm.api.SlimePlugin
import me.hugo.savethekweebecs.arena.GameManager
import me.hugo.savethekweebecs.commands.SaveTheKweebecsCommand
import me.hugo.savethekweebecs.di.SaveTheKweebecsModules
import me.hugo.savethekweebecs.listeners.JoinLeaveListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module
import revxrsal.commands.bukkit.BukkitCommandHandler

class SaveTheKweebecs : KoinComponent, JavaPlugin() {

    private val gameManager: GameManager by inject()
    private lateinit var commandHandler: BukkitCommandHandler
    public lateinit var slimePlugin: SlimePlugin

    companion object {
        private lateinit var main: SaveTheKweebecs

        fun getInstance(): SaveTheKweebecs {
            return main
        }
    }

    override fun onEnable() {
        main = this
        logger.info("Starting Save The Kweebecs 2.0...")

        logger.info("Starting dependencies and injections!")
        startKoin {
            modules(SaveTheKweebecsModules().module)
        }

        slimePlugin = Bukkit.getPluginManager().getPlugin("SlimeWorldManager") as SlimePlugin

        saveDefaultConfig()
        logger.info("Server started with ${gameManager.arenas.size} arenas!")

        commandHandler = BukkitCommandHandler.create(this)

        commandHandler.register(SaveTheKweebecsCommand())
        commandHandler.registerBrigadier()

        Bukkit.getPluginManager().registerEvents(JoinLeaveListener(), this)
    }

    override fun onDisable() {
        commandHandler.unregisterAllCommands()
    }


}