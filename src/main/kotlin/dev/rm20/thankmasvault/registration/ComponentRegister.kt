package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.economy.WalletComponent
import dev.rm20.thankmasvault.economy.WalletComponentCodec
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.npc.VaultNpcComponentCodec

object ComponentRegister {
    fun registerComponents(plugin: ThankmasVault) {
        val registry = plugin.entityStoreRegistry

        WalletComponent.componentType = registry.registerComponent(
            WalletComponent::class.java, "thanksmasvault:wallet", WalletComponentCodec
        )

        VaultNpcComponent.componentType = registry.registerComponent(
            VaultNpcComponent::class.java, "thanksmasvault:shop", VaultNpcComponentCodec
        )
    }
}