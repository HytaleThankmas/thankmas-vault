package dev.rm20.thankmasvault.assets

import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class PlayerNpcEntry(
    @SerialName("Name")
    var name: String = "",
    @SerialName("DisplayName")
    var displayName: String? = null,
    @SerialName("Icon")
    var icon: String? = null
) {
    fun getEffectiveDisplayName(): String = displayName?.ifBlank { null } ?: name
}
