package dev.rm20.thankmasvault.shop

import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class ShopItem(
    @SerialName("ItemId")
    var itemId: String = "",

    @SerialName("Cost")
    var cost: Int = 0,

    @SerialName("Quantity")
    var quantity: Int = 1,

    @SerialName("DisplayName")
    var displayName: String? = null,

    @SerialName("Description")
    var description: String? = null
) {
    companion object {
    }
}
