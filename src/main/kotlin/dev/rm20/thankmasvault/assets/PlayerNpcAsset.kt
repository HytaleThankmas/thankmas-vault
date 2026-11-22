package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import dev.rm20.thankmasvault.npc.player.PlayerFoundNpcs
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(
    path = "Thankmas/PlayerNPC",
    extraImports = [
        "com.hypixel.hytale.codec.codecs.array.ArrayCodec",
        "java.util.function.IntFunction",
        "dev.rm20.thankmasvault.assets.PlayerNpcEntryCodec",
        "dev.rm20.thankmasvault.assets.PlayerNpcEntry"
    ]
)
class PlayerNpcAsset : AssetBase<PlayerNpcAsset>() {

    @SerialName("NPC")
    @SerialWithCodec("ArrayCodec(PlayerNpcEntryCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(PlayerNpcEntry::class.java, size) as Array<PlayerNpcEntry> })")
    var npcs: Array<PlayerNpcEntry> = emptyArray()

    companion object {
        private var ASSET_STORE: AssetStore<String, PlayerNpcAsset, DefaultAssetMap<String, PlayerNpcAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, PlayerNpcAsset, DefaultAssetMap<String, PlayerNpcAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(PlayerNpcAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, PlayerNpcAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): PlayerNpcAsset? = runCatching { getAssetMap().getAsset(id) }.getOrNull()

        @JvmStatic
        fun getAllPlayerNpcs(): List<PlayerNpcEntry> {
            val merged = linkedMapOf<String, PlayerNpcEntry>()

            try {
                val store = getAssetStore()
                for (asset in store.assetMap.assetMap.values) {
                    for (npc in asset.npcs) {
                        if (npc.name.isNotBlank()) {
                            merged[PlayerFoundNpcs.normalize(npc.name)] = npc
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            if (merged.isEmpty()) {
                for (defaultNpc in DEFAULT_NPCS) {
                    merged[PlayerFoundNpcs.normalize(defaultNpc.name)] = defaultNpc
                }
            }

            return merged.values.toList()
        }

        val DEFAULT_NPCS = listOf(
            PlayerNpcEntry("Acorn"),
            PlayerNpcEntry("CanadianFlash")
        )
    }
}
