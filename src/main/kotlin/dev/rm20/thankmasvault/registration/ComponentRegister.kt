package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.economy.WalletComponent
import dev.rm20.thankmasvault.shop.shopKepper.ShopComponent

object ComponentRegister {
    fun registerComponents(plugin: ThankmasVault) {
        val registry = plugin.entityStoreRegistry

        WalletComponent.componentType = registry.registerComponent(
            WalletComponent::class.java,
            "thanksmasvault:wallet",
            WalletComponent.CODEC
        )

        ShopComponent.componentType = registry.registerComponent(
            ShopComponent::class.java,
            "thanksmasvault:shop",
            ShopComponent.CODEC
        )
    }
}