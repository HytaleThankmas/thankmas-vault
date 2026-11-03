package dev.rm20.thankmasvault.npc

import acorn.thankmasdelivery.component.DeliveryNpc
import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.dependency.Dependency
import com.hypixel.hytale.component.dependency.Order
import com.hypixel.hytale.component.dependency.SystemDependency
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.HolderSystem
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.systems.RoleBuilderSystem

/** Restores the configured NPC nameplate with "[NPC] " prefix after Hytale sets up the NPC's role. */
class NpcNameplateSystem : HolderSystem<EntityStore>() {

    private val dependencies: Set<Dependency<EntityStore>> = setOf(
        SystemDependency(Order.AFTER, RoleBuilderSystem::class.java)
    )

    override fun getQuery(): Query<EntityStore> {
        val dType = DeliveryNpc.getComponentType()
        return if (dType != null) {
            Query.or(VaultNpcComponent.componentType, dType)
        } else {
            VaultNpcComponent.componentType
        }
    }

    override fun getDependencies(): Set<Dependency<EntityStore>> = dependencies

    override fun onEntityAdd(
        holder: Holder<EntityStore>,
        reason: AddReason,
        store: Store<EntityStore>
    ) {
        val config = holder.getComponent(VaultNpcComponent.componentType)
        val dType = DeliveryNpc.getComponentType()
        val deliveryNpc = if (dType != null) holder.getComponent(dType) else null

        val rawName = config?.displayName?.takeIf { it.isNotBlank() }
            ?: deliveryNpc?.name()?.takeIf { it.isNotBlank() }
            ?: return

        NpcNameplateHelper.applyNameplate(holder, rawName)
    }

    override fun onEntityRemoved(
        holder: Holder<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>
    ) {
    }
}
