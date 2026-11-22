package dev.rm20.thankmasvault.npc.player

import acorn.thankmasdelivery.component.DeliveryNpc
import com.hypixel.hytale.component.AddReason
import com.hypixel.hytale.component.Holder
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.HolderSystem
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.server.core.modules.entity.component.Interactable
import com.hypixel.hytale.server.core.modules.interaction.Interactions
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
        AppearanceHelper.applySavedAppearance(holder, config)

        val dType = DeliveryNpc.getComponentType()
        if (dType != null) {
            val deliveryNpc = holder.getComponent(dType)
            if (deliveryNpc != null && config.displayName.isNotBlank()) {
                deliveryNpc.setName(config.displayName)
            }
        }

        if (!config.isStatue) {
            val interactions = holder.getComponent(Interactions.getComponentType())
            if (interactions != null && "Parcel_Delivery_NPC" == interactions.getInteractionId(InteractionType.Use)) {
                interactions.removeInteractionId(InteractionType.Use)
                interactions.setInteractionHint(null)
            }
            if (holder.getComponent(Interactable.getComponentType()) == null) {
                holder.addComponent(Interactable.getComponentType(), Interactable.INSTANCE)
            }
        }
    }

    override fun onEntityRemoved(
        holder: Holder<EntityStore>,
        reason: RemoveReason,
        store: Store<EntityStore>
    ) {
    }
}
