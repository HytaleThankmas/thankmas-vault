package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableAsset(
    path = "Thankmas/Poses",
    extraImports = [
        "com.hypixel.hytale.codec.codecs.array.ArrayCodec",
        "java.util.function.IntFunction",
        "dev.rm20.thankmasvault.assets.PoseInfoCodec",
        "dev.rm20.thankmasvault.assets.PoseInfo"
    ]
)
class PoseAsset : AssetBase<PoseAsset>() {

    @SerialName("Poses")
    @SerialWithCodec("ArrayCodec(PoseInfoCodec, IntFunction { size -> java.lang.reflect.Array.newInstance(PoseInfo::class.java, size) as Array<PoseInfo> })")
    var poses: Array<PoseInfo> = emptyArray()

    companion object {
        private var ASSET_STORE: AssetStore<String, PoseAsset, DefaultAssetMap<String, PoseAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, PoseAsset, DefaultAssetMap<String, PoseAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(PoseAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, PoseAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): PoseAsset? = getAssetMap().getAsset(id)

        /**
         * Returns all poses merged from all PoseAssets in Thankmas/Poses.
         * returns DEFAULT_POSES if none.
         */
        @JvmStatic
        fun getAllPoses(): List<PoseInfo> {
            val merged = linkedMapOf<String, PoseInfo>()

            try {
                val store = getAssetStore()
                for (asset in store.assetMap.assetMap.values) {
                    for (pose in asset.poses) {
                        if (pose.name.isNotBlank()) {
                            merged[pose.name.lowercase()] = pose
                        }
                    }
                }
            } catch (_: Throwable) {
            }

            if (merged.isEmpty()) {
                for (defaultPose in DEFAULT_POSES) {
                    merged[defaultPose.name.lowercase()] = defaultPose
                }
            } else {
                for (defaultPose in DEFAULT_POSES) {
                    merged.putIfAbsent(defaultPose.name.lowercase(), defaultPose)
                }
            }

            return merged.values.toList()
        }

        val DEFAULT_POSES = listOf(
            PoseInfo("Sit", "Characters/Animations/Flavor/Sit.blockyanim"),
            PoseInfo("SitGround", "Characters/Animations/Flavor/Sit_Ground.blockyanim"),
            PoseInfo("Sleep", "Characters/Animations/Flavor/Sleep.blockyanim"),
            PoseInfo("Fashion", "Characters/Animations/Poses/Fashion.blockyanim"),
            PoseInfo("Fashion2", "Characters/Animations/Poses/Fashion2.blockyanim"),
            PoseInfo("Fashion3", "Characters/Animations/Poses/Fashion3.blockyanim"),
            PoseInfo("Fashion4", "Characters/Animations/Poses/Fashion4.blockyanim"),
            PoseInfo("Guardian", "Characters/Animations/Poses/Guardian.blockyanim"),
            PoseInfo("Strong", "Characters/Animations/Poses/Strong.blockyanim"),
            PoseInfo("Ninja", "Characters/Animations/Poses/Ninja.blockyanim"),
            PoseInfo("Girly", "Characters/Animations/Poses/Girly.blockyanim"),
            PoseInfo("BattleAxe", "Characters/Animations/Poses/BattleAxe.blockyanim"),
            PoseInfo("Sword", "Characters/Animations/Poses/Sword.blockyanim"),
            PoseInfo("Staff", "Characters/Animations/Poses/Staff.blockyanim"),
            PoseInfo("Warhammer", "Characters/Animations/Poses/Warhammer.blockyanim"),
            PoseInfo("Pickaxe", "Characters/Animations/Poses/Pickaxe.blockyanim"),
            PoseInfo("Hero_Bow", "Characters/Animations/Poses/Hero/Hero_Bow.blockyanim"),
            PoseInfo("Hero_Staff", "Characters/Animations/Poses/Hero/Hero_Staff.blockyanim"),
            PoseInfo("Hero_Sword_Shield", "Characters/Animations/Poses/Hero/Hero_Sword_Shield.blockyanim"),
            PoseInfo("Hero_Long_Sword", "Characters/Animations/Poses/Hero/Hero_Long_Sword.blockyanim"),
            PoseInfo("Dio_Walk", "Characters/Animations/Poses/Dio_Walk.blockyanim"),
            PoseInfo("Jotaro_Walk", "Characters/Animations/Poses/Jotaro_Walk.blockyanim"),
            PoseInfo("Book_Read", "Characters/Animations/Poses/Book_Read.blockyanim"),
            PoseInfo("Show_Item", "Characters/Animations/Poses/Show_Item.blockyanim"),
            PoseInfo("Crouch", "Characters/Animations/Default/Crouch.blockyanim")
        )
    }
}
