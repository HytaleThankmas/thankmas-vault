package dev.rm20.thankmasvault.npc

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.dependency.Dependency
import com.hypixel.hytale.component.dependency.Order
import com.hypixel.hytale.component.dependency.SystemDependency
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.math.util.MathUtil
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.Interactable
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.system.TransformSystems
import com.hypixel.hytale.server.core.modules.interaction.Interactions
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.systems.SteeringSystem
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.utils.CameraUtil
import dev.rm20.thankmasvault.utils.getPoint
import java.util.function.BiConsumer

class NpcCameraGazeSystem : TickingSystem<EntityStore>() {

    companion object {
        private const val MAX_TURN_RAD: Float = (150.0 * (Math.PI / 180.0)).toFloat()
        private const val DEFAULT_EYE_HEIGHT: Double = 1.6

        private val VAULT_NPC_TYPE = VaultNpcComponent.componentType
        private val TRANSFORM_TYPE = TransformComponent.getComponentType()
        private val HEAD_ROT_TYPE = HeadRotation.getComponentType()
        private val MODEL_TYPE = ModelComponent.getComponentType()
        private val INTERACTABLE_TYPE = Interactable.getComponentType()
        private val INTERACTIONS_TYPE = Interactions.getComponentType()
    }

    private val dependencies: Set<Dependency<EntityStore>> = setOf(
        SystemDependency(Order.AFTER, SteeringSystem::class.java),
        SystemDependency(Order.BEFORE, TransformSystems.EntityTrackerUpdate::class.java)
    )

    private val query: Query<EntityStore> by lazy {
        Query.and(
            VAULT_NPC_TYPE,
            TRANSFORM_TYPE,
            HEAD_ROT_TYPE
        )
    }

    @Transient
    private var currentStore: Store<EntityStore>? = null

    private val chunkConsumer = BiConsumer<ArchetypeChunk<EntityStore>, CommandBuffer<EntityStore>> { chunk, buffer ->
        val store = currentStore ?: return@BiConsumer
        val size = chunk.size()

        for (i in 0 until size) {
            val ref = chunk.getReferenceTo(i)
            if (!ref.isValid) continue
            if (AppearanceHelper.isStatue(ref, store)) {
                val transform = chunk.getComponent(i, TRANSFORM_TYPE)
                val headRotation = chunk.getComponent(i, HEAD_ROT_TYPE)
                val vaultNpc = chunk.getComponent(i, VAULT_NPC_TYPE)
                if (transform != null && headRotation != null) {
                    if (vaultNpc != null) {
                        dev.rm20.thankmasvault.npc.statue.StatueOrientationService.tickStatueOrientation(
                            ref, store, vaultNpc, transform, headRotation
                        )
                    } else {
                        val bodyYaw = transform.rotation.yaw()
                        if (headRotation.rotation.pitch() != 0f || headRotation.rotation.yaw() != bodyYaw || headRotation.rotation.roll() != 0f) {
                            headRotation.rotation.set(0f, bodyYaw, 0f)
                        }
                    }
                }
                continue
            }

            val vaultNpc = chunk.getComponent(i, VAULT_NPC_TYPE) ?: continue
            val pointId = vaultNpc.pointId

            val cameraPos = if (pointId.isNotEmpty()) {
                store.getPoint(pointId)?.position
            } else {
                CameraUtil.getActiveCameraPositionForNpc(ref)
            } ?: continue

            val transform = chunk.getComponent(i, TRANSFORM_TYPE) ?: continue
            val headRotation = chunk.getComponent(i, HEAD_ROT_TYPE) ?: continue

            val modelComp = chunk.getComponent(i, MODEL_TYPE)
            val eyeHeight = modelComp?.model?.eyeHeight?.toDouble() ?: DEFAULT_EYE_HEIGHT

            val pos = transform.position
            val dx = cameraPos.x - pos.x
            val dy = cameraPos.y - (pos.y + eyeHeight)
            val dz = cameraPos.z - pos.z

            val baseYaw = PhysicsMath.normalizeTurnAngle(PhysicsMath.headingFromDirection(dx, dz))
            transform.rotation.set(0f, baseYaw, 0f)

            if (CameraUtil.isNpcBeingInteractedWith(ref)) {
                // look at player
                val pitch = PhysicsMath.pitchFromDirection(dx, dy, dz)
                headRotation.rotation.set(pitch, baseYaw, 0f)
            } else {
                // clamp rotation
                val currentHeadYaw = headRotation.rotation.yaw()
                val offset = MathUtil.wrapAngle(currentHeadYaw - baseYaw)

                if (offset > MAX_TURN_RAD) {
                    headRotation.rotation.setYaw(MathUtil.wrapAngle(baseYaw + MAX_TURN_RAD))
                } else if (offset < -MAX_TURN_RAD) {
                    headRotation.rotation.setYaw(MathUtil.wrapAngle(baseYaw - MAX_TURN_RAD))
                }
            }
        }
    }

    override fun getDependencies(): Set<Dependency<EntityStore>> = dependencies

    override fun tick(dt: Float, systemIndex: Int, store: Store<EntityStore>) {
        currentStore = store
        try {
            store.forEachChunk(query, chunkConsumer)
        } finally {
            currentStore = null
        }
    }
}