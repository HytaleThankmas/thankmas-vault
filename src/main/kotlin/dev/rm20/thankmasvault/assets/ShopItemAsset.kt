package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.inventory.ItemStack
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset
import dev.rm20.thankmasvault.utils.TranslationUtil
import gg.ginco.jellyparty.codec.AssetBase
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableAsset

@SerializableAsset(
    path = "Thankmas/Prices",
    extraImports = [
        "dev.rm20.thankmasvault.npc.shop.ItemCodec",
        "dev.rm20.thankmasvault.npc.shop.CosmeticCodec"
    ]
)
class ShopItemAsset : AssetBase<ShopItemAsset>() {

    @SerialName("ItemId")
    @SerialWithCodec("ItemCodec")
    var itemId: String = ""

    @SerialName("CosmeticId")
    @SerialWithCodec("CosmeticCodec")
    var cosmeticId: String = ""

    @SerialName("Icon")
    var icon: String? = null

    @SerialName("Cost")
    var cost: Int = 1

    @SerialName("Quantity")
    var quantity: Int = 1

    @SerialName("DisplayName")
    var displayName: String? = null
        get() = TranslationUtil.resolve(field)

    @SerialName("Description")
    var description: String? = null
        get() = TranslationUtil.resolve(field)

    val isCosmetic: Boolean
        get() = cosmeticId.isNotBlank() || (effectiveItemId.isNotBlank() && runCatching { CosmeticAsset.getAssetMap().getAsset(effectiveItemId) != null }.getOrDefault(false))

    val effectiveItemId: String
        get() = itemId.ifBlank { if (cosmeticId.isBlank()) id else "" }

    val resolvedCosmeticId: String
        get() = when {
            cosmeticId.isNotBlank() -> cosmeticId
            effectiveItemId.isNotBlank() && runCatching { CosmeticAsset.getAssetMap().getAsset(effectiveItemId) != null }.getOrDefault(false) -> effectiveItemId
            else -> ""
        }

    fun getCosmeticAsset(): CosmeticAsset? {
        val cid = resolvedCosmeticId
        if (cid.isBlank()) return null
        return runCatching { CosmeticAsset.getAssetMap().getAsset(cid) }.getOrNull()
    }

    fun resolveName(): String {
        displayName?.takeIf { it.isNotBlank() }?.let { return it }
        if (isCosmetic) {
            val cosmetic = getCosmeticAsset()
            val msg = cosmetic?.properties?.translationProperties?.name
            if (msg != null && msg.ansiMessage.isNotBlank()) {
                return msg.ansiMessage
            }
            return resolvedCosmeticId
        }
        val tid = effectiveItemId
        if (tid.isNotBlank()) {
            return runCatching {
                Message.translation(ItemStack(tid, 1).item.translationKey).ansiMessage
            }.getOrDefault(tid)
        }
        return id
    }

    fun resolveDescription(): String {
        description?.takeIf { it.isNotBlank() }?.let { return it }
        if (isCosmetic) {
            val cosmetic = getCosmeticAsset()
            val msg = cosmetic?.properties?.translationProperties?.description
            if (msg != null && msg.ansiMessage.isNotBlank()) {
                return msg.ansiMessage
            }
            return ""
        }
        return ""
    }

    fun resolveIconPath(): String? {
        if (!icon.isNullOrBlank()) return icon
        if (isCosmetic) {
            return getCosmeticAsset()?.iconPath
        }
        return null
    }

    /**
     * get price with optional price per item price variance
     */
    fun getPrice(shopId: String?, shopMultiplier: Double = 1.0, useBasePrice: Boolean = false): Int {
        if (useBasePrice) {
            return maxOf(1, (cost * shopMultiplier).toInt())
        }

        val hash = 31 * (shopId?.hashCode() ?: 0) + id.hashCode()

        // -5% to +5%
        val variance = ((hash % 11) - 5) / 100.0

        val price = cost * shopMultiplier * (1.0 + variance)
        return maxOf(1, price.toInt())
    }

    fun getPrice(shop: VaultShopAsset): Int {
        return getPrice(shop.id, shop.shopMultiplier, shop.useBasePrice)
    }

    companion object {
        private var ASSET_STORE: AssetStore<String, ShopItemAsset, DefaultAssetMap<String, ShopItemAsset>>? = null

        @JvmStatic
        fun getAssetStore(): AssetStore<String, ShopItemAsset, DefaultAssetMap<String, ShopItemAsset>> {
            if (ASSET_STORE == null) {
                ASSET_STORE = AssetRegistry.getAssetStore(ShopItemAsset::class.java)
            }
            return ASSET_STORE!!
        }

        @JvmStatic
        fun getAssetMap(): DefaultAssetMap<String, ShopItemAsset> = getAssetStore().assetMap

        @JvmStatic
        fun getById(id: String): ShopItemAsset? {
            return runCatching { getAssetMap().getAsset(id) }.getOrNull()
        }
    }
}
