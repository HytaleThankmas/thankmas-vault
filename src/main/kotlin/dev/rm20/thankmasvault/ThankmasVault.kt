package dev.rm20.thankmasvault

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.npc.NPCPlugin
import dev.rm20.thankmasvault.registration.CommandRegister
import dev.rm20.thankmasvault.registration.ComponentRegister
import dev.rm20.thankmasvault.assets.registerAssets
import dev.rm20.thankmasvault.npc.shop.shopKepper.BuilderActionOpenShop
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import dev.rm20.thankmasvault.registration.DialogueActionsRegister
import dev.rm20.thankmasvault.registration.NpcActionRegister
import dev.rm20.thankmasvault.utils.CameraUtil
import java.util.logging.Level

class ThankmasVault(init: JavaPluginInit) : JavaPlugin(init) {
    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }

    override fun start() {
        LOGGER.at(Level.INFO).log("Starting Thankmas vault!")
    }

    override fun setup() {
        registerAssets()
        CommandRegister.registerCommands(this)
        ComponentRegister.registerComponents(this)
        NpcActionRegister.registerNpcActions(this)

        DialogueActionsRegister.registerDialogueActions()

        eventRegistry.registerGlobal(PlayerDisconnectEvent::class.java) { event ->
            CameraUtil.onPlayerDisconnect(event.playerRef)
        }
    }

    override fun shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Thankmas vault!")
    }
}
