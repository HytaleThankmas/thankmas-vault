package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.ComponentType
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.RefChangeSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.VaultNpcComponent

class NpcAppearanceSystem : RefChangeSystem<EntityStore, VaultNpcComponent>() {

    override fun componentType(): ComponentType<EntityStore, VaultNpcComponent> = VaultNpcComponent.componentType

    override fun getQuery(): Query<EntityStore> = componentType()

    override fun onComponentAdded(
        ref: Ref<EntityStore>,
        component: VaultNpcComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
            store.externalData.world.execute {
                if (ref.isValid) {
                    AppearanceHelper.applySavedAppearance(ref, store, component)
                }
            }

    }

    override fun onComponentSet(
        ref: Ref<EntityStore>,
        oldComponent: VaultNpcComponent?,
        newComponent: VaultNpcComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
            store.externalData.world.execute {
                if (ref.isValid) {
                    AppearanceHelper.applySavedAppearance(ref, store, newComponent)
                }
            }

    }

    override fun onComponentRemoved(
        ref: Ref<EntityStore>,
        component: VaultNpcComponent,
        store: Store<EntityStore>,
        commandBuffer: CommandBuffer<EntityStore>
    ) {
    }
}
