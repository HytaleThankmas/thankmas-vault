package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import dev.rm20.thankmasvault.npc.dialogue.Dialogue
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(
    path = "Thankmas/Npc",
    extraImports = [
        "com.hypixel.hytale.codec.codecs.array.ArrayCodec",
        "java.util.function.IntFunction",
        "dev.rm20.thankmasvault.npc.dialogue.DialogueCodec",
        "dev.rm20.thankmasvault.npc.dialogue.Dialogue"
    ]
)
class NpcAsset : AssetBase<NpcAsset>(){

    @SerialName("Dialogues")
    @SerialWithCodec("DialogueCodec")
    var dialogue: Dialogue? = null

    companion object {
        private var ASSET_STORE: AssetStore<String, NpcAsset, DefaultAssetMap<String, NpcAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, NpcAsset, DefaultAssetMap<String, NpcAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(NpcAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, NpcAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): NpcAsset? = getAssetMap().getAsset(id)
    }
}