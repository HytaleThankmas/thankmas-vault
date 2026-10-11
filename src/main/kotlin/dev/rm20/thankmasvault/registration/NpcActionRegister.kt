package dev.rm20.thankmasvault.registration

import com.hypixel.hytale.server.npc.NPCPlugin
import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.npc.dialogue.BuilderActionOpenDialogue
import dev.rm20.thankmasvault.npc.shop.shopKepper.BuilderActionOpenShop

object NpcActionRegister {

    fun registerNpcActions(plugin: ThankmasVault) {
        NPCPlugin.get().registerCoreComponentType("OpenVaultShop") { BuilderActionOpenShop() }
        NPCPlugin.get().registerCoreComponentType("OpenNpcDialogue") { BuilderActionOpenDialogue() }
    }
}