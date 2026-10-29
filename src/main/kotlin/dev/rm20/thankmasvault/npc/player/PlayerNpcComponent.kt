package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class PlayerNpcComponent : Component<EntityStore> {
    var skinJson: String = ""
    var attachmentsJson: String = ""
    var pose: String = ""
    var mainHandItem: String = ""
    var offHandItem: String = ""

    override fun clone(): Component<EntityStore> {
        val copy = PlayerNpcComponent()
        copy.skinJson = this.skinJson
        copy.attachmentsJson = this.attachmentsJson
        copy.pose = this.pose
        copy.mainHandItem = this.mainHandItem
        copy.offHandItem = this.offHandItem
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, PlayerNpcComponent>

        @JvmField
        val CODEC: BuilderCodec<PlayerNpcComponent> = PlayerNpcComponentCodec
    }
}
