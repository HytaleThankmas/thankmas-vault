package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.HolderSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.VaultNpcComponent

class NpcLoadSystem : HolderSystem<EntityStore>() {

    override fun getQuery(): Query<EntityStore> = VaultNpcComponent.componentType

    override fun onEntityAdd(
        holder: Holder<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>
    ) {
        val config = holder.getComponent(VaultNpcComponent.componentType) ?: return
        if (config.skinJson.isNotBlank() || config.attachmentsJson.isNotBlank()) {
            AppearanceHelper.applySavedAppearanceToHolder(holder, config)
        }
    }

    override fun onEntityRemoved(
        holder: Holder<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>
    ) {
    }
}