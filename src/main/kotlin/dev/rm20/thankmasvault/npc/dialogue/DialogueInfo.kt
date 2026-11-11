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

enum class DialogueChoiceIcon(val assetPath: String) {
    Speach("UI/Custom/NPC/Images/Speach.png"),
    Question("UI/Custom/NPC/Images/Question.png"),
    Doors("UI/Custom/NPC/Images/Doors.png"),
    Container("UI/Custom/NPC/Images/Container.png");

    companion object {
        fun resolve(input: String?): String? {
            if (input.isNullOrBlank()) return null
            val trimmed = input.trim()
            val matched = entries.firstOrNull { it.name.equals(trimmed, ignoreCase = true) }
            if (matched != null) return matched.assetPath

            val normalized = trimmed.removeSuffix(".png")
            val matchedWithoutExt = entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            if (matchedWithoutExt != null) return matchedWithoutExt.assetPath

            if (trimmed.contains("/")) {
                return if (trimmed.endsWith(".png", ignoreCase = true)) trimmed else "$trimmed.png"
            }
            return "UI/Custom/NPC/Images/${trimmed.removeSuffix(".png")}.png"
        }
    }
}

@SerializableObject
class Choice(
    var title: String = "",
    var next: String? = null,
    var action: String? = null,
    var zoneIconImage: String? = null,
    var icon: String? = null,
) {
    fun resolveIcon(): String? = DialogueChoiceIcon.resolve(zoneIconImage ?: icon)
}