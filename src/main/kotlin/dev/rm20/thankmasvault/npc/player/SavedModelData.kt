package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.server.core.asset.type.model.config.ModelAttachment

data class SavedModelData(
    val scale: Float = 1.0f,
    val modelAssetId: String? = null,
    val model: String? = null,
    val texture: String? = null,
    val gradientSet: String? = null,
    val gradientId: String? = null,
    val randomAttachments: Map<String, String>? = null,
    val attachments: Array<ModelAttachment>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SavedModelData

        if (scale != other.scale) return false
        if (modelAssetId != other.modelAssetId) return false
        if (model != other.model) return false
        if (texture != other.texture) return false
        if (gradientSet != other.gradientSet) return false
        if (gradientId != other.gradientId) return false
        if (randomAttachments != other.randomAttachments) return false
        if (!attachments.contentEquals(other.attachments)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = scale.hashCode()
        result = 31 * result + (modelAssetId?.hashCode() ?: 0)
        result = 31 * result + (model?.hashCode() ?: 0)
        result = 31 * result + (texture?.hashCode() ?: 0)
        result = 31 * result + (gradientSet?.hashCode() ?: 0)
        result = 31 * result + (gradientId?.hashCode() ?: 0)
        result = 31 * result + (randomAttachments?.hashCode() ?: 0)
        result = 31 * result + (attachments?.contentHashCode() ?: 0)
        return result
    }
}
