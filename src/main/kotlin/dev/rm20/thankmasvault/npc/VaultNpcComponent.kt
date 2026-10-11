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

    override fun clone(): Component<EntityStore> {
        val copy = VaultNpcComponent()
        copy.displayName = displayName
        copy.shopId = this.shopId
        copy.pointId = this.pointId
        copy.dialogueId = this.dialogueId
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, VaultNpcComponent>

        @JvmField
        val CODEC: BuilderCodec<VaultNpcComponent> = VaultNpcComponentCodec
    }
}