package dev.rm20.thankmasvault.npc.shop

import com.hypixel.hytale.codec.validation.Validator
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.inventory.ItemStack
import dev.hardaway.wardrobe.impl.cosmetic.CosmeticAsset
import dev.rm20.thankmasvault.utils.TranslationUtil
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class ShopItem(
    @SerialName("ItemId")
    @SerialWithCodec("ItemCodec")
    var itemId: String = "",

    @SerialName("CosmeticId")
    @SerialWithCodec("CosmeticCodec")
    var cosmeticId: String = "",

    @SerialName("Icon")
    var icon: String? = null,

    @SerialName("Cost")
    var cost: Int = 0,

    @SerialName("Quantity")
    var quantity: Int = 1,

    displayName: String? = null,
    description: String? = null
) {
    @SerialName("DisplayName")
    var displayName: String? = displayName
        get() = TranslationUtil.resolve(field)

    @SerialName("Description")
    var description: String? = description
        get() = TranslationUtil.resolve(field)

    val isCosmetic: Boolean
        get() = cosmeticId.isNotBlank() || (itemId.isNotBlank() && runCatching { CosmeticAsset.getAssetMap().getAsset(itemId) != null }.getOrDefault(false))

    val resolvedCosmeticId: String
        get() = when {
            cosmeticId.isNotBlank() -> cosmeticId
            itemId.isNotBlank() && runCatching { CosmeticAsset.getAssetMap().getAsset(itemId) != null }.getOrDefault(false) -> itemId
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
        if (itemId.isNotBlank()) {
            return runCatching {
                Message.translation(ItemStack(itemId, 1).item.translationKey).ansiMessage
            }.getOrDefault(itemId)
        }
        return ""
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

    companion object {
        val VALIDATOR: Validator<String> = Item.VALIDATOR_CACHE.validator
        val COSMETIC_VALIDATOR: Validator<String> = CosmeticCodec.VALIDATOR_CACHE.validator
    }
}

