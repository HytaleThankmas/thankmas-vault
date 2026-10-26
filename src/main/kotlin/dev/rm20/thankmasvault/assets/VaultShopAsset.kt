package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline.voicelineInfo
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialIgnore
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(
    path = "Thankmas/Shops",
    extraImports = [
        "com.hypixel.hytale.codec.codecs.array.ArrayCodec",
        "java.util.function.IntFunction",
        "dev.rm20.thankmasvault.npc.shop.PriceCodec",
        "dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline.voicelineInfoCodec",
        "dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline.voicelineInfo"
    ]
)
class VaultShopAsset : AssetBase<VaultShopAsset>() {

    @SerialName("Title")
    var title: String? = null

    @SerialName("Items")
    @SerialWithCodec("ArrayCodec(PriceCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(String::class.java, size) as Array<String> })")
    var items: Array<String> = emptyArray()

    @SerialName("ShopMultiplier")
    var shopMultiplier: Double = 1.0

    @SerialName("ShopOpenVoicelLines")
    @SerialWithCodec("ArrayCodec(voicelineInfoCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(voicelineInfo::class.java, size) as Array<voicelineInfo> })")
    var shopOpenVoicelLine: Array<voicelineInfo> = emptyArray()

    @SerialName("UseBasePrice")
    var useBasePrice: Boolean = false

    fun getDisplayName(): String = title ?: id

    fun getShopItems(): List<ShopItemAsset> {
        return items.mapNotNull { id ->
            ShopItemAsset.getById(id)
        }
    }

    fun getItemPrice(item: ShopItemAsset): Int {
        return item.getPrice(this)
    }

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
