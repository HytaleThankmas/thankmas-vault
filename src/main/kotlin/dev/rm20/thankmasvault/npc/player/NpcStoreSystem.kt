package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.VaultNpcComponent

class NpcStoreSystem : RefSystem<EntityStore>() {

    override fun getQuery(): Query<EntityStore> = VaultNpcComponent.componentType

    override fun onEntityAdded(
        ref: Ref<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
        val config = store.getComponent(ref, VaultNpcComponent.componentType) ?: return
        if (config.skinJson.isNotBlank() || config.attachmentsJson.isNotBlank()) {
            store.externalData.world.execute {
                if (ref.isValid) {
                    AppearanceHelper.applySavedAppearance(ref, store, config)
                }
            }
        }
    }

    override fun onEntityRemove(
        ref: Ref<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
    }
}