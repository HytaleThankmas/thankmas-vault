package dev.rm20.thankmasvault.registration

import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import com.hypixel.hytale.server.core.asset.HytaleAssetStore
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import dev.rm20.thankmasvault.ThankmasVault
import dev.rm20.thankmasvault.shop.ShopAsset

object AssetRegister {
    fun registerAssets(plugin: ThankmasVault) {
        val assetRegistry = plugin.assetRegistry
        assetRegistry.register(
            HytaleAssetStore.builder(ShopAsset::class.java, DefaultAssetMap())
                .setPath("Thankmas/Shops")
                .setCodec(ShopAsset.CODEC)
                .setKeyFunction(ShopAsset::getId)
                .loadsAfter(Item::class.java)
                .build()
        )
    }
}