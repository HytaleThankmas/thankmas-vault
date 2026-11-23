package dev.rm20.thankmasvault.assets

import com.hypixel.hytale.assetstore.AssetRegistry
import com.hypixel.hytale.assetstore.AssetStore
import com.hypixel.hytale.assetstore.map.DefaultAssetMap
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.inventory.ItemStack
import com.hypixel.hytale.server.npc.util.InventoryHelper
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset
import dev.rm20.thankmasvault.npc.player.PlayerNpcAPI
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

    @SerialName("RequiredPlayerNpc")
    var requiredPlayerNpc: String? = null

    @SerialName("RequiredActiveQuest")
    var requiredActiveQuest: String? = null

    @SerialName("ShopId")
    var shopId: String? = null

    fun getEffectiveRequiredPlayerNpc(): String? {
        if (!requiredPlayerNpc.isNullOrBlank()) return requiredPlayerNpc

        // Auto-detect plushies: e.g. "Plush_Slamma", "Deco_Plush_Slamma", "Plush_Simon_Armoured"
        val candidate = extractPlushieCandidateName() ?: return null
        if (PlayerNpcAPI.isAssetPlayerNpc(candidate)) {
            return candidate
        }
        return null
    }

    private fun extractPlushieCandidateName(): String? {
        val namesToCheck = listOfNotNull(
            id.takeIf { it.isNotBlank() },
            cosmeticId.takeIf { it.isNotBlank() },
            effectiveItemId.takeIf { it.isNotBlank() }
        )
        for (name in namesToCheck) {
            val stripped = when {
                name.startsWith("Plush_") -> name.removePrefix("Plush_")
                name.startsWith("Deco_Plush_") -> name.removePrefix("Deco_Plush_")
                else -> null
            } ?: continue

            if (PlayerNpcAPI.isAssetPlayerNpc(stripped)) {
                return stripped
            }
            val baseName = stripped.substringBefore("_")
            if (PlayerNpcAPI.isAssetPlayerNpc(baseName)) {
                return baseName
            }
        }
        return null
    }

    fun getEffectiveRequiredQuest(): String? {
        return requiredActiveQuest?.takeIf { it.isNotBlank() }
    }

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
        else
        {
            val item: ItemStack? = InventoryHelper.createItem(itemId)
            val msg = item?.displayDescription
            if (msg != null && msg.ansiMessage.isNotBlank()) {
                return msg.ansiMessage
            }
            return ""

        }
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

        @JvmStatic
        fun getAll(): Collection<ShopItemAsset> {
            return runCatching { getAssetMap().assetMap.values }.getOrDefault(emptyList())
        }
    }
}
