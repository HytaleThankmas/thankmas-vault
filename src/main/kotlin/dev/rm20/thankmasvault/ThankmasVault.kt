package dev.rm20.thankmasvault

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import dev.rm20.thankmasvault.registration.CommandRegister
import dev.rm20.thankmasvault.registration.ComponentRegister
import dev.rm20.thankmasvault.assets.registerAssets
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.player.NpcEmoteHandler
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
        AppearanceHelper.registerPosesToModelAsset()
    }

    override fun setup() {
        registerAssets()
        CommandRegister.registerCommands(this)
        ComponentRegister.registerComponents(this)
        NpcActionRegister.registerNpcActions(this)

        DialogueActionsRegister.registerDialogueActions()
        NpcEmoteHandler.register(this)

        eventRegistry.registerGlobal(PlayerDisconnectEvent::class.java) { event ->
            CameraUtil.onPlayerDisconnect(event.playerRef)
        }
    }

    override fun shutdown() {
        NpcEmoteHandler.unregister()
        LOGGER.at(Level.INFO).log("Shutting down Thankmas vault!")
    }
}
