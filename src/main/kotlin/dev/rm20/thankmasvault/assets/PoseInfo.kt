package dev.rm20.thankmasvault.assets

import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class PoseInfo(
    @SerialName("Name")
    var name: String = "",
    @SerialName("Animation")
    var animation: String = "",
    @SerialName("DisplayName")
    var displayName: String? = null
) {
    fun getLabel(): String = displayName?.ifBlank { null } ?: name
}