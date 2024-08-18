package world.ultravanilla

import org.bukkit.plugin.java.JavaPlugin

import scala.annotation.varargs

class Signage extends JavaPlugin {
    override def onEnable(): Unit = {
        Signage.instance = this

        val signCommand = new SignCommand(Signage.instance)
        getCommand("sign").setExecutor(signCommand)
        getServer.getPluginManager.registerEvents(signCommand, Signage.instance)
    }

    @varargs
    def getString(key: String, format: String*): String = {
        var message = getConfig.getString("strings." + key)
        var i = 0
        while ( {
            i < format.length
        }) {
            message = message.replace(format(i), format(i + 1))
            i += 2
        }
        Palette.translate(message)
    }
}

object Signage {
    private var instance: Signage = null

    def getInstance: Signage = instance
}
