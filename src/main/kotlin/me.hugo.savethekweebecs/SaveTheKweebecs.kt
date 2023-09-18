package me.hugo.savethekweebecs

import com.infernalsuite.aswm.api.SlimePlugin
import me.hugo.savethekweebecs.di.SaveTheKweebecsModules
import me.hugo.savethekweebecs.listeners.JoinLeaveListener
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.component.KoinComponent
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.ksp.generated.module

class SaveTheKweebecs : KoinComponent, JavaPlugin() {

    public companion object {
        private lateinit var main: SaveTheKweebecs

        public fun getInstance(): SaveTheKweebecs {
            return main
        }

        public fun getSlimePlugin() {

        }
    }

    override fun onEnable() {
        main = this
        logger.info("Starting Save The Kweebecs 2.0...")

        logger.info("Starting dependencies and injections!")
        startKoin {
            modules(SaveTheKweebecsModules().module)
            module {
                single { Bukkit.getPluginManager().getPlugin("SlimeWorldManager") as SlimePlugin }
            }
        }

        Bukkit.getPluginManager().registerEvents(JoinLeaveListener(), this)
    }


}