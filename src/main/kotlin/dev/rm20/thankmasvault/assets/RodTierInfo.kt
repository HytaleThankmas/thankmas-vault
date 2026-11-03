package dev.rm20.thankmasvault.assets

import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class RodTierInfo(
    @SerialName("ItemId")
    var itemId: String = "",
    @SerialName("Cost")
    var cost: Int = 0,
    @SerialName("DisplayName")
    var displayName: String? = null,
    @SerialName("Description")
    var description: String? = null
) {
    fun resolveName(): String = displayName?.ifBlank { null } ?: itemId
    fun resolveDescription(): String = description ?: ""
}
