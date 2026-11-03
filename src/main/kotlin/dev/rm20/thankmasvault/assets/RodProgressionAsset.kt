package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset
import java.util.function.IntFunction

@SerializableAsset(
    path = "Thankmas/RodProgression",
    extraImports = [
        "com.hypixel.hytale.codec.codecs.array.ArrayCodec",
        "java.util.function.IntFunction",
        "dev.rm20.thankmasvault.assets.RodTierInfoCodec",
        "dev.rm20.thankmasvault.assets.RodTierInfo"
    ]
)
class RodProgressionAsset : AssetBase<RodProgressionAsset>() {

    @SerialName("Title")
    var title: String? = null

    @SerialName("Tiers")
    @SerialWithCodec("ArrayCodec(RodTierInfoCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(RodTierInfo::class.java, size) as Array<RodTierInfo> })")
    var tiers: Array<RodTierInfo> = emptyArray()

    fun getDisplayName(): String = title ?: "Rod Upgrades"

    companion object {
        private var ASSET_STORE: AssetStore<String, RodProgressionAsset, DefaultAssetMap<String, RodProgressionAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, RodProgressionAsset, DefaultAssetMap<String, RodProgressionAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(RodProgressionAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, RodProgressionAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): RodProgressionAsset? = runCatching { getAssetMap().getAsset(id) }.getOrNull()

        @JvmStatic
        fun getDefault(): RodProgressionAsset? {
            return getById("Default_Rods")
                ?: getById("Fisher_Rods")
                ?: runCatching { getAssetMap().assetMap.values.firstOrNull() }.getOrNull()
        }
    }
}
