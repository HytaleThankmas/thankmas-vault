package dev.rm20.thankmasvault.npc.shop

import com.hypixel.hytale.codec.validation.Validator
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import dev.rm20.thankmasvault.utils.TranslationUtil
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class ShopItem(
    @SerialName("ItemId")
    @SerialWithCodec("ItemCodec")
    var itemId: String = "",

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

    companion object {
        val VALIDATOR: Validator<String> = Item.VALIDATOR_CACHE.validator
    }
}
