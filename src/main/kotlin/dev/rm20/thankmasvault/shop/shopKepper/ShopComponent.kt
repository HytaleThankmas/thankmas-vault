package dev.rm20.thankmasvault.shop.shopKepper

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.codecannotation.AutoCodecBuilder

class ShopComponent : Component<EntityStore> {
    var shopId: String = ""
    var pointId: String = ""


    override fun clone(): Component<EntityStore> {
        val copy = ShopComponent()
        copy.shopId = this.shopId
        copy.pointId = this.pointId
        return copy;
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, ShopComponent>

        @JvmField
        val CODEC: BuilderCodec<ShopComponent> = AutoCodecBuilder.create(
            ShopComponent::class.java
        ) { ShopComponent() }
    }
}