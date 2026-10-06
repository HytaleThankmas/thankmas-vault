package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.economy.WalletComponent
import dev.rm20.thankmasvault.economy.WalletComponentCodec
import dev.rm20.thankmasvault.shop.shopKepper.ShopComponent
import dev.rm20.thankmasvault.shop.shopKepper.ShopComponentCodec

object ComponentRegister {
    fun registerComponents(plugin: ThankmasVault) {
        val registry = plugin.entityStoreRegistry

        WalletComponent.componentType = registry.registerComponent(
            WalletComponent::class.java, "thanksmasvault:wallet", WalletComponentCodec
        )

        ShopComponent.componentType = registry.registerComponent(
            ShopComponent::class.java, "thanksmasvault:shop", ShopComponentCodec
        )
    }
}