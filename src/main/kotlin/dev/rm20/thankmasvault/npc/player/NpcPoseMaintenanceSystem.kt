package dev.rm20.thankmasvault.npc.player


import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.DelayedSystem
import com.hypixel.hytale.protocol.AnimationSlot
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import java.util.function.BiConsumer

class NpcPoseMaintenanceSystem : DelayedSystem<EntityStore>(2.5f) {

    private val query: Query<EntityStore> = Query.or(PlayerNpcComponent.componentType, VaultNpcComponent.componentType)

    override fun delayedTick(dt: Float, systemIndex: Int, store: Store<EntityStore>) {
        store.forEachChunk(query, BiConsumer { chunk, _ ->
            for (i in 0 until chunk.size()) {
                val playerNpc = chunk.getComponent(i, PlayerNpcComponent.componentType)
                val vaultNpc = chunk.getComponent(i, VaultNpcComponent.componentType)
                val pose: String? = playerNpc?.pose?.takeIf { it.isNotBlank() } ?: vaultNpc?.pose?.takeIf { it.isNotBlank() }

                if (!pose.isNullOrBlank() && !pose.equals("None", ignoreCase = true)) {
                    val ref = chunk.getReferenceTo(i)
                    if (ref.isValid) {
                        val activeAnim = chunk.getComponent(i, ActiveAnimationComponent.getComponentType())
                        val currentStatus = activeAnim?.activeAnimations?.getOrNull(AnimationSlot.Status.ordinal)

                        // Only re-apply if the pose was actually interrupted or cleared (zero overhead when steady)
                        if (currentStatus == null || !currentStatus.equals(pose, ignoreCase = true)) {
                            AppearanceHelper.applyPose(ref, store, pose)
                        }
                    }
                }
            }
        })
    }
}
