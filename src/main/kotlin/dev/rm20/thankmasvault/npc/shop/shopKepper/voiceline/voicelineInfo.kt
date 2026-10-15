package dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline

import dev.rm20.thankmasvault.utils.TranslationUtil
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class voicelineInfo(
    var soundEventId: String = "",
    subtitle: String = "",
    var durationMs: Int = 3000
) {
    var subtitle: String = subtitle
        get() = TranslationUtil.resolve(field) ?: ""
}