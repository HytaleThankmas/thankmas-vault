package dev.rm20.thankmasvault.npc.shop.shopKepper.voiceline

import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class voicelineInfo(
    var soundEventId: String = "",
    var subtitle: String = "",
    var durationMs: Int = 3000
)