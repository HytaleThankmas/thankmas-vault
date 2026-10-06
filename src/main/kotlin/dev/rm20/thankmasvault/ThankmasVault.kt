package dev.rm20.thankmasvault

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit
import com.hypixel.hytale.server.npc.NPCPlugin
import dev.rm20.thankmasvault.registration.AssetRegister
import dev.rm20.thankmasvault.registration.CommandRegister
import dev.rm20.thankmasvault.registration.ComponentRegister
import dev.rm20.thankmasvault.shop.shopKepper.BuilderActionOpenShop
import java.util.logging.Level

class ThankmasVault(init: JavaPluginInit) : JavaPlugin(init) {
    companion object {
        val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    }

    override fun start() {
        LOGGER.at(Level.INFO).log("Starting Thankmas vault!")
    }

    override fun setup() {
        AssetRegister.registerAssets(this)
        CommandRegister.registerCommands(this)
        ComponentRegister.registerComponents(this)

        NPCPlugin.get().registerCoreComponentType("OpenVaultShop") { BuilderActionOpenShop() }
    }

    override fun shutdown() {
        LOGGER.at(Level.INFO).log("Shutting down Thankmas vault!")
    }
}
