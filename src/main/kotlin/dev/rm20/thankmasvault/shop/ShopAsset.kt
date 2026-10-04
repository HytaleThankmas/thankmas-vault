package dev.rm20.thankmasvault.shop

import com.hypixel.hytale.assetstore.AssetExtraInfo
import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.codec.AssetBuilderCodec
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import com.hypixel.hytale.assetstore.map.JsonAssetWithMap
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.codecs.array.ArrayCodec
import java.lang.reflect.Array as JavaArray
import java.util.function.IntFunction

class ShopAsset : JsonAssetWithMap<String, DefaultAssetMap<String, ShopAsset>> {

    private var shopId: String = ""
    var data: AssetExtraInfo.Data? = null
    var title: String? = null
    var items: Array<ShopItem> = emptyArray()

    constructor()

    constructor(id: String, title: String?, items: Array<ShopItem>) {
        this.shopId = id
        this.title = title
        this.items = items
    }

    override fun getId(): String = shopId

    fun setId(id: String) {
        this.shopId = id
    }

    fun getDisplayName(): String = title ?: shopId

    companion object {
        private var ASSET_STORE: AssetStore<String, ShopAsset, DefaultAssetMap<String, ShopAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, ShopAsset, DefaultAssetMap<String, ShopAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(ShopAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, ShopAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): ShopAsset? = getAssetMap().getAsset(id)

        @Suppress("UNCHECKED_CAST")
        @JvmField
        val CODEC: AssetBuilderCodec<String, ShopAsset> = AssetBuilderCodec.builder(
            ShopAsset::class.java,
            ::ShopAsset,
            Codec.STRING,
            { asset, id -> asset.setId(id) },
            { asset -> asset.getId() },
            { asset, extraData -> asset.data = extraData },
            { asset -> asset.data }
        )
            .append(
                KeyedCodec("Title", Codec.STRING),
                { asset, title -> asset.title = title },
                { asset -> asset.title }
            ).add()
            .append(
                KeyedCodec("Items", ArrayCodec(ShopItem.CODEC, IntFunction { size ->
                    JavaArray.newInstance(ShopItem::class.java, size) as Array<ShopItem>
                })),
                { asset, items -> asset.items = items ?: emptyArray() },
                { asset -> asset.items }
            ).add()
            .build()
    }
}
