package dev.rm20.thankmasvault.npc

import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.dependency.Dependency
import com.hypixel.hytale.component.dependency.Order
import com.hypixel.hytale.component.dependency.SystemDependency
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.component.system.tick.TickingSystem
import com.hypixel.hytale.math.util.MathUtil
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.system.TransformSystems
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.systems.SteeringSystem
import dev.rm20.thankmasvault.utils.CameraUtil
import dev.rm20.thankmasvault.utils.getPoint
import java.util.function.BiConsumer

class NpcCameraGazeSystem : TickingSystem<EntityStore>() {

    private val dependencies: Set<Dependency<EntityStore>> = setOf(
        SystemDependency(Order.AFTER, SteeringSystem::class.java),
        SystemDependency(Order.BEFORE, TransformSystems.EntityTrackerUpdate::class.java)
    )

    private val query: Query<EntityStore> by lazy {
        Query.and(
            VaultNpcComponent.componentType,
            TransformComponent.getComponentType(),
            HeadRotation.getComponentType()
        )
    }

    override fun getDependencies(): Set<Dependency<EntityStore>> = dependencies

    override fun tick(dt: Float, systemIndex: Int, store: Store<EntityStore>) {
        val maxTurnRad = Math.toRadians(150.0).toFloat()

        store.forEachChunk(query, BiConsumer { chunk, _ ->
            for (i in 0 until chunk.size()) {
                val vaultNpc = chunk.getComponent(i, VaultNpcComponent.componentType) ?: continue
                val pointId = vaultNpc.pointId
                val ref = chunk.getReferenceTo(i)

                val cameraPos = if (pointId.isNotBlank()) {
                    store.getPoint(pointId)?.position
                } else {
                    CameraUtil.getActiveCameraPositionForNpc(ref)
                } ?: continue

                if (!ref.isValid) continue

                val transform = chunk.getComponent(i, TransformComponent.getComponentType()) ?: continue
                val headRotation = chunk.getComponent(i, HeadRotation.getComponentType()) ?: continue
                val modelComp = chunk.getComponent(i, ModelComponent.getComponentType())
                val eyeHeight = modelComp?.model?.eyeHeight?.toDouble() ?: 1.6

                val dx = cameraPos.x - transform.position.x
                val dy = cameraPos.y - (transform.position.y + eyeHeight)
                val dz = cameraPos.z - transform.position.z

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

                    if (offset > maxTurnRad) {
                        headRotation.rotation.setYaw(MathUtil.wrapAngle(baseYaw + maxTurnRad))
                    } else if (offset < -maxTurnRad) {
                        headRotation.rotation.setYaw(MathUtil.wrapAngle(baseYaw - maxTurnRad))
                    }
                }
            }
        })
    }
}
