package dev.rm20.thankmasvault.fishing

import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Component
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import gg.ginco.jellyparty.codec.annotations.SerialName
import gg.ginco.jellyparty.codec.annotations.SerializableObject

@SerializableObject
class PlayerRodProgressionComponent : Component<EntityStore> {

    @SerialName("UnlockedRods")
    var unlockedRods: List<String> = listOf("AA_Rod_Crude")

    fun isUnlocked(rodId: String): Boolean {
        if (rodId == "AA_Rod_Crude") return true
        return unlockedRods.contains(rodId)
    }

    fun unlockRod(rodId: String): Boolean {
        if (!unlockedRods.contains(rodId)) {
            unlockedRods = unlockedRods + rodId
            return true
        }
        return false
    }

    override fun clone(): Component<EntityStore> {
        val copy = PlayerRodProgressionComponent()
        copy.unlockedRods = ArrayList(unlockedRods)
        return copy
    }

    companion object {
        @JvmStatic
        lateinit var componentType: ComponentType<EntityStore, PlayerRodProgressionComponent>

        @JvmField
        val CODEC: BuilderCodec<PlayerRodProgressionComponent> = PlayerRodProgressionComponentCodec
    }
}
