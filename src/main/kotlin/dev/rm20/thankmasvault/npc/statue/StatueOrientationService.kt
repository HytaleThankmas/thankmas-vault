package dev.rm20.thankmasvault.npc.statue

import com.hypixel.hytale.builtin.points.PointsPlugin
import com.hypixel.hytale.builtin.points.manager.PointEntry
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.util.MathUtil
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.utils.getPoint
import dev.rm20.thankmasvault.utils.removePoint
import dev.rm20.thankmasvault.utils.spawnStatuePointForNpc
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.concurrent.ConcurrentHashMap

object StatueOrientationService {

    const val STATUE_POINT_HEIGHT_OFFSET = 2.2

    private val statueLastKnownYaw: ConcurrentHashMap<String, Float> = ConcurrentHashMap()


    fun ensureStatuePoint(
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        vaultNpc: VaultNpcComponent
    ): PointEntry? {
        if (!npcRef.isValid) return null
        var point = if (vaultNpc.pointId.isNotBlank()) store.getPoint(vaultNpc.pointId) else null
        if (point == null) {
            val pointName = if (vaultNpc.displayName.isNotBlank()) "${vaultNpc.displayName}_Statue" else "Statue_Point"
            point = store.spawnStatuePointForNpc(npcRef, pointName)
            vaultNpc.pointId = point.id
            val currentYawRad = store.getComponent(npcRef, TransformComponent.getComponentType())?.rotation?.yaw() ?: 0f
            statueLastKnownYaw[point.id] = currentYawRad
        }
        return point
    }


    fun rotateStatue(
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        yawDegrees: Float
    ): Boolean {
        if (!npcRef.isValid) return false
        val transform = store.getComponent(npcRef, TransformComponent.getComponentType()) ?: return false
        val headRotation = store.getComponent(npcRef, HeadRotation.getComponentType())
        val vaultNpc = store.getComponent(npcRef, VaultNpcComponent.componentType) ?: return false

        var normalizedDeg = yawDegrees % 360f
        if (normalizedDeg < 0f) normalizedDeg += 360f
        val targetYawRad = Math.toRadians(normalizedDeg.toDouble()).toFloat()

        transform.rotation.set(0f, targetYawRad, 0f)
        headRotation?.rotation?.set(0f, targetYawRad, 0f)

        val point = ensureStatuePoint(npcRef, store, vaultNpc)
        if (point != null) {
            point.rotation.set(0f, normalizedDeg, 0f)
            val manager = store.getResource(PointsPlugin.get().managerResourceType)
            manager.notifyViewersAdd(point)
            statueLastKnownYaw[point.id] = targetYawRad
        }

        return true
    }


    fun tickStatueOrientation(
        npcRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        vaultNpc: VaultNpcComponent,
        transform: TransformComponent,
        headRotation: HeadRotation
    ) {
        val point = ensureStatuePoint(npcRef, store, vaultNpc) ?: return

        val targetPointPos = Vector3d(
            transform.position.x,
            transform.position.y + STATUE_POINT_HEIGHT_OFFSET,
            transform.position.z
        )
        if (point.position.distanceSquared(targetPointPos) > 0.0001) {
            point.position.set(targetPointPos)
            val manager = store.getResource(PointsPlugin.get().managerResourceType)
            manager.markSpatialDirty()
            manager.notifyViewersAdd(point)
        }

        val teleport = store.getComponent(npcRef, Teleport.getComponentType())
        if (teleport != null && !teleport.rotation.y.isNaN()) {
            val tpYawRad = teleport.rotation.yaw()
            val tpYawDeg = Math.toDegrees(tpYawRad.toDouble()).toFloat()
            point.rotation.set(0f, tpYawDeg, 0f)
            val manager = store.getResource(PointsPlugin.get().managerResourceType)
            manager.notifyViewersAdd(point)
            statueLastKnownYaw[point.id] = tpYawRad
            transform.rotation.set(0f, tpYawRad, 0f)
            headRotation.rotation.set(0f, tpYawRad, 0f)
            return
        }

        val pointYawDeg = point.rotation.y()
        val pointYawRad = Math.toRadians(pointYawDeg.toDouble()).toFloat()
        val lastKnownYaw = statueLastKnownYaw[point.id]

        if (lastKnownYaw == null) {
            statueLastKnownYaw[point.id] = pointYawRad
            transform.rotation.set(0f, pointYawRad, 0f)
            headRotation.rotation.set(0f, pointYawRad, 0f)
        } else {
            val pointDelta = Math.abs(MathUtil.wrapAngle(pointYawRad - lastKnownYaw))
            if (pointDelta > 0.001f) {
                transform.rotation.set(0f, pointYawRad, 0f)
                headRotation.rotation.set(0f, pointYawRad, 0f)
                statueLastKnownYaw[point.id] = pointYawRad
            } else {
                transform.rotation.set(0f, lastKnownYaw, 0f)
                headRotation.rotation.set(0f, lastKnownYaw, 0f)
            }
        }
    }


    fun removeStatuePoint(pointId: String, store: Store<EntityStore>) {
        if (pointId.isBlank()) return
        statueLastKnownYaw.remove(pointId)
        store.removePoint(pointId)
    }


    fun getStatueYawDegrees(npcRef: Ref<EntityStore>, store: Store<EntityStore>): Float {
        val vaultNpc = store.getComponent(npcRef, VaultNpcComponent.componentType)
        if (vaultNpc != null && vaultNpc.pointId.isNotBlank()) {
            val point = store.getPoint(vaultNpc.pointId)
            if (point != null) {
                var deg = point.rotation.y() % 360f
                if (deg < 0f) deg += 360f
                return deg
            }
        }
        val transform = store.getComponent(npcRef, TransformComponent.getComponentType())
        val yawRad = transform?.rotation?.yaw() ?: 0f
        var deg = Math.toDegrees(yawRad.toDouble()).toFloat() % 360f
        if (deg < 0f) deg += 360f
        return deg
    }
}
