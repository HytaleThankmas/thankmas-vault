package dev.rm20.thankmasvault.shop

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(
    path = "Thankmas/Shops",
    extraImports = ["com.hypixel.hytale.codec.codecs.array.ArrayCodec", "java.util.function.IntFunction"]
)
class VaultShopAsset : AssetBase<VaultShopAsset>() {

    @SerialName("Title")
    var title: String? = null

    @SerialName("Items")
    @SerialWithCodec("ArrayCodec(ShopItemCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(ShopItem::class.java, size) as Array<ShopItem> })")
    var items: Array<ShopItem> = emptyArray()

    fun getDisplayName(): String = title ?: id

    companion object {
        private var ASSET_STORE: AssetStore<String, VaultShopAsset, DefaultAssetMap<String, VaultShopAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, VaultShopAsset, DefaultAssetMap<String, VaultShopAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(VaultShopAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, VaultShopAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): VaultShopAsset? = getAssetMap().getAsset(id)
    }
}
