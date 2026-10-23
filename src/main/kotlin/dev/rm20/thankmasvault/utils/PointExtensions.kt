package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.builtin.points.PointsPlugin
import com.hypixel.hytale.builtin.points.manager.PointEntry
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.player.PointShapeType
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

/**
 * Returns the point on the receiver [Store<EntityStore>]
 *
 * @param pointId the point id
 * @return the point entry
 */
fun Store<EntityStore>.getPoint(pointId: String): PointEntry? =
    this.getResource(PointsPlugin.get().managerResourceType).getPoint(pointId)

/**
 * Creates a new point [Store<EntityStore>]
 *
 * @param worldName the world name
 * @param position
 * @param rotation
 * @param name
 * @param shape
 * @return the point entry
 */
fun Store<EntityStore>.createPoint(
    worldName: String,
    position: Vector3d,
    rotation: Vector3f = Vector3f(0f, 0f, 0f),
    name: String? = null,
    shape: PointShapeType = PointShapeType.Block
): PointEntry {
    val manager = this.getResource(PointsPlugin.get().managerResourceType)
    val id = manager.generateUniquePointId()
    val entry = PointEntry(id, worldName.lowercase(Locale.ROOT), position, rotation, name, true)
    entry.shape = shape
    manager.register(id, entry)
    manager.notifyViewersAdd(entry)
    return entry
}

/**
 * Spawn point for player [Store<EntityStore>]
 *
 * @param playerRef the player ref
 * @param name
 * @return the point entry
 */
fun Store<EntityStore>.spawnPointForPlayer(
    playerRef: PlayerRef,
    name: String? = null
): PointEntry {
    val manager = this.getResource(PointsPlugin.get().managerResourceType)
    val world = this.externalData.world
    val worldName = world.name.lowercase(Locale.ROOT)

    val playerPos = playerRef.transform.position
    val position = Vector3d(playerPos.x, playerPos.y, playerPos.z)

    val headRot = playerRef.headRotation
    val rotX = Math.toDegrees(headRot.x.toDouble()).toFloat()
    val rotY = Math.toDegrees(headRot.y.toDouble()).toFloat()
    val rotZ = Math.toDegrees(headRot.z.toDouble()).toFloat()
    val rotation = Vector3f(rotX, rotY, rotZ)

    val id = manager.generateUniquePointId()
    val entry = PointEntry(id, worldName, position, rotation, name, true)
    manager.register(id, entry)
    manager.notifyViewersAdd(entry)
    return entry
}


/**
 * Spawn camera point for npc [Store<EntityStore>]
 *
 * @param playerRef the player ref
 * @param npcRef the npc ref
 * @param name
 * @return the point entry
 */
fun Store<EntityStore>.spawnCameraPointForNpc(
    playerRef: PlayerRef,
    npcRef: Ref<EntityStore>,
    name: String? = null
): PointEntry {
    val manager = this.getResource(PointsPlugin.get().managerResourceType)
    val world = this.externalData.world
    val worldName = world.name.lowercase(Locale.ROOT)

    val npcTransform = this.getComponent(npcRef, TransformComponent.getComponentType())
    val npcPos = npcTransform?.position ?: run {
        val pPos = playerRef.transform.position
        Vector3d(pPos.x, pPos.y, pPos.z)
    }

    val headRot = playerRef.headRotation
    val yawRad = headRot.y.toDouble()
    val playerYawDeg = Math.toDegrees(yawRad).toFloat()

    val sinYaw = sin(yawRad)
    val cosYaw = cos(yawRad)

    val offsetX = 1.5 * sinYaw - 0.1 * cosYaw
    val offsetY = 1.6
    val offsetZ = 1.5 * cosYaw + 0.1 * sinYaw

    val position = Vector3d(
        npcPos.x + offsetX,
        npcPos.y + offsetY,
        npcPos.z + offsetZ
    )

    var pointYaw = (playerYawDeg + 20f) % 360f
    if (pointYaw > 180f) pointYaw -= 360f
    if (pointYaw <= -180f) pointYaw += 360f

    val rotation = Vector3f(0f, pointYaw, 0f)

    val id = manager.generateUniquePointId()
    val entry = PointEntry(id, worldName, position, rotation, name, true)
    entry.shape = PointShapeType.Sphere
    manager.register(id, entry)
    manager.notifyViewersAdd(entry)
    return entry
}

/**
 * Returns the closest point near the NPC [Store<EntityStore>]
 *
 * @param origin
 * @param worldName the world name
 * @return the point entry
 */
fun Store<EntityStore>.findClosestPoint(
    origin: Vector3d,
    worldName: String? = null
): PointEntry? {
    val manager = this.getResource(PointsPlugin.get().managerResourceType)
    val points = manager.points
    val targetWorld = worldName?.lowercase(Locale.ROOT)

    var closest: PointEntry? = null
    var minDistanceSq = Double.MAX_VALUE

    for (point in points) {
        if (targetWorld != null && point.worldName.lowercase(Locale.ROOT) != targetWorld) {
            continue
        }
        val distSq = point.position.distanceSquared(origin)
        if (distSq < minDistanceSq) {
            minDistanceSq = distSq
            closest = point
        }
    }
    return closest
}

/**
 * Returns the closest point for player [Store<EntityStore>]
 *
 * @param playerRef the player ref
 * @return the point entry
 */
fun Store<EntityStore>.findClosestPointForPlayer(
    playerRef: PlayerRef
): PointEntry? {
    val world = this.externalData.world
    val playerPos = playerRef.transform.position
    val origin = Vector3d(playerPos.x, playerPos.y, playerPos.z)
    return findClosestPoint(origin, world.name)
}