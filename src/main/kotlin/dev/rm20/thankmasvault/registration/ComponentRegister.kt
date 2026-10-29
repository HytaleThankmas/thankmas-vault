package dev.rm20.thankmasvault.registration

import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.economy.WalletComponent
import dev.rm20.thankmasvault.economy.WalletComponentCodec
import dev.rm20.thankmasvault.npc.player.NpcAppearanceSystem
import dev.rm20.thankmasvault.npc.player.NpcLoadSystem
import dev.rm20.thankmasvault.npc.player.NpcStoreSystem
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponentCodec
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

        PlayerNpcComponent.componentType = registry.registerComponent(
            PlayerNpcComponent::class.java, "thanksmasvault:playernpc", PlayerNpcComponentCodec
        )

        registry.registerSystem(NpcLoadSystem())
        registry.registerSystem(NpcStoreSystem())
        registry.registerSystem(NpcAppearanceSystem())
    }
}
