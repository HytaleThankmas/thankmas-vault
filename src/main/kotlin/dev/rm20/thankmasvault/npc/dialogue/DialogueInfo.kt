package dev.rm20.thankmasvault.npc.dialogue

import dev.rm20.thankmasvault.utils.TranslationUtil
import gg.ginco.jellyparty.codec.annotations.SerialWithCodec
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject(
    extraImports = [
        "com.hypixel.hytale.codec.codecs.map.MapCodec",
        "java.util.HashMap",
        "java.util.function.Supplier"
    ]
)
class Dialogue(
    var startNode: String = "",

    @SerialWithCodec("MapCodec(DialogueNodeCodec, Supplier { HashMap() })")
    var nodes: Map<String, DialogueNode> = emptyMap()
)

@SerializableObject
class DialogueNode(
    var content: List<Content> = emptyList(),
    var next: String? = null,
    var choices: List<Choice> = emptyList()
) {
    constructor(
        content: Content,
        next: String? = null,
        choices: List<Choice> = emptyList()
    ) : this(listOf(content), next, choices)

    fun getRandomContent(): Content? = content.randomOrNull()
}

@SerializableObject
class Content(
    var soundEventId: String = "",
    subtitle: String = "",
) {
    var subtitle: String = subtitle
        get() = TranslationUtil.resolve(field) ?: ""
}

@SerializableObject
class Choice(
    var title: String = "",
    var next: String? = null,
    var action: String? = null,
)