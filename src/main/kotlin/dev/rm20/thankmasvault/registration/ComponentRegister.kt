package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.economy.WalletComponent

object ComponentRegister {
    fun registerComponents(plugin: ThankmasVault) {
        val registry = plugin.entityStoreRegistry

        WalletComponent.componentType = registry.registerComponent(
            WalletComponent::class.java,
            "thanksmasvault:wallet",
            WalletComponent.CODEC
        )
    }
}