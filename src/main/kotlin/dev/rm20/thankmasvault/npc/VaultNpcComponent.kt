package dev.rm20.thankmasvault.npc

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class VaultNpcComponent : Component<EntityStore> {
    var displayName: String = ""
    var shopId: String = ""
    var pointId: String = ""
    var dialogueId: String = ""
    var model: String = ""
    var skinJson: String = ""
    var attachmentsJson: String = ""
    var pose: String = ""
    var mainHandItem: String = ""
    var offHandItem: String = ""

    override fun clone(): Component<EntityStore> {
        val copy = VaultNpcComponent()
        copy.displayName = displayName
        copy.shopId = this.shopId
        copy.pointId = this.pointId
        copy.dialogueId = this.dialogueId
        copy.model = this.model
        copy.skinJson = this.skinJson
        copy.attachmentsJson = this.attachmentsJson
        copy.pose = this.pose
        copy.mainHandItem = this.mainHandItem
        copy.offHandItem = this.offHandItem
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, VaultNpcComponent>

        @JvmField
        val CODEC: BuilderCodec<VaultNpcComponent> = VaultNpcComponentCodec
    }
}
