package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.builtin.points.PointsPlugin
import com.hypixel.hytale.builtin.points.manager.PointEntry
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Locale

fun Store<EntityStore>.getPoint(pointId: String): PointEntry? =
    this.getResource(PointsPlugin.get().managerResourceType).getPoint(pointId)

fun Store<EntityStore>.createPoint(
    worldName: String,
    position: Vector3d,
    rotation: Vector3f = Vector3f(0f, 0f, 0f),
    name: String? = null
): PointEntry {
    val manager = this.getResource(PointsPlugin.get().managerResourceType)
    val id = manager.generateUniquePointId()
    val entry = PointEntry(id, worldName.lowercase(Locale.ROOT), position, rotation, name, true)
    manager.register(id, entry)
    manager.notifyViewersAdd(entry)
    return entry
}

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

fun Store<EntityStore>.findClosestPointForPlayer(
    playerRef: PlayerRef
): PointEntry? {
    val world = this.externalData.world
    val playerPos = playerRef.transform.position
    val origin = Vector3d(playerPos.x, playerPos.y, playerPos.z)
    return findClosestPoint(origin, world.name)
}