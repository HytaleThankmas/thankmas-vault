package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.protocol.*
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.modules.physics.util.PhysicsMath
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.role.support.StateSupport
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object CameraUtil {

    data class OriginalPlayerTransform(
        val position: Vector3d,
        val bodyRotation: Rotation3f,
        val headRotation: Rotation3f
    )

    private val activeCameraPoints = ConcurrentHashMap<UUID, String>()
    private val originalTransforms = ConcurrentHashMap<UUID, OriginalPlayerTransform>()
    private val hiddenPlayersForViewer = ConcurrentHashMap<UUID, MutableSet<UUID>>()
    private val activeNpcForPlayer = ConcurrentHashMap<UUID, Ref<EntityStore>>()
    private val activeNpcCameraPositions = ConcurrentHashMap<Ref<EntityStore>, Vector3d>()

    const val DEFAULT_HIDE_RADIUS: Double = 2.0

    fun isCameraActiveAtPoint(playerRef: PlayerRef, pointId: String): Boolean {
        return activeCameraPoints[playerRef.uuid] == pointId
    }

    fun isCameraActive(playerRef: PlayerRef): Boolean {
        return activeCameraPoints.containsKey(playerRef.uuid)
    }

    fun isNpcBeingInteractedWith(npcRef: Ref<EntityStore>): Boolean {
        return activeNpcForPlayer.values.any { it == npcRef }
    }

    fun getActiveCameraPositionForNpc(npcRef: Ref<EntityStore>): Vector3d? {
        return activeNpcCameraPositions[npcRef]
    }

    fun setFixedCameraFromPoint(
        playerRef: PlayerRef,
        pointId: String,
        backOffset: Double = 0.75,
        force: Boolean = false,
        hideRadius: Double = DEFAULT_HIDE_RADIUS,
        npcRef: Ref<EntityStore>? = null
    ): Boolean {
        if (!force && activeCameraPoints[playerRef.uuid] == pointId) {
            return true
        }

        val ref = playerRef.reference ?: return false
        val pointEntry = ref.store.getPoint(pointId) ?: return false

        // save player location
        val transform = ref.store.getComponent(ref, TransformComponent.getComponentType())
        if (transform != null) {
            originalTransforms.putIfAbsent(
                playerRef.uuid,
                OriginalPlayerTransform(
                    Vector3d(transform.position),
                    Rotation3f(transform.rotation),
                    Rotation3f(playerRef.headRotation)
                )
            )
        }

        // Hide players
        hideNearbyPlayers(playerRef, pointEntry.position, hideRadius)

        if (npcRef != null && npcRef.isValid) {
            activeNpcForPlayer[playerRef.uuid] = npcRef
            activeNpcCameraPositions[npcRef] = pointEntry.position
            orientNpcTowardsCamera(npcRef, pointEntry.position)
        }

        setFixedCamera(
            playerRef,
            pointEntry.position,
            pointEntry.rotation.x.toDouble(),
            pointEntry.rotation.y.toDouble(),
            pointEntry.rotation.z.toDouble()
        )
        activeCameraPoints[playerRef.uuid] = pointId
        return true
    }

    fun setFixedCamera(
        playerRef: PlayerRef,
        position: Vector3d,
        rotX: Double,
        rotY: Double,
        rotZ: Double
    ) {

        val yawRad = Math.toRadians(rotY).toFloat()
        val pitchRad = Math.toRadians(rotX).toFloat()
        val rollRad = Math.toRadians(rotZ).toFloat()

        // camera settings
        val cameraSettings = ServerCameraSettings().apply {
            positionLerpSpeed = 0.25f
            rotationLerpSpeed = 0.25f
            distance = 0f
            displayCursor = true
            isFirstPerson = true
            positionType = PositionType.Custom
            this.position = Position(position.x, position.y, position.z)
            rotationType = RotationType.Custom
            rotation = Direction(yawRad, pitchRad, rollRad)
            movementMultiplier = Vector3f(0f, 0f, 0f)
        }

        playerRef.packetHandler.writeNoCache(
            SetServerCamera(ClientCameraView.Custom, true, cameraSettings)
        )
    }

    fun setFixedCameraAndTeleport(
        playerRef: PlayerRef,
        position: Vector3d,
        rotX: Double,
        rotY: Double,
        rotZ: Double,
        forwardDirection: Vector3d? = null,
        backOffset: Double = 0.75
    ) {
        val ref = playerRef.reference
        if (ref != null) {
            val transform = ref.store.getComponent(ref, TransformComponent.getComponentType())
            if (transform != null) {
                originalTransforms.putIfAbsent(
                    playerRef.uuid,
                    OriginalPlayerTransform(
                        Vector3d(transform.position),
                        Rotation3f(transform.rotation),
                        Rotation3f(playerRef.headRotation)
                    )
                )
            }
        }
        setFixedCamera(playerRef, position, rotX, rotY, rotZ)
    }

    fun orientNpcTowardsCamera(npcRef: Ref<EntityStore>, cameraPos: Vector3d) {
        val store = npcRef.store
        val npcTransform = store.getComponent(npcRef, TransformComponent.getComponentType()) ?: return
        val modelComp = store.getComponent(npcRef, ModelComponent.getComponentType())
        val eyeHeight = modelComp?.model?.eyeHeight?.toDouble() ?: 1.6

        val dx = cameraPos.x - npcTransform.position.x
        val dy = cameraPos.y - (npcTransform.position.y + eyeHeight)
        val dz = cameraPos.z - npcTransform.position.z

        val yaw = PhysicsMath.normalizeTurnAngle(PhysicsMath.headingFromDirection(dx, dz))
        val pitch = PhysicsMath.pitchFromDirection(dx, dy, dz)

        val headRotation = store.getComponent(npcRef, HeadRotation.getComponentType())
        headRotation?.rotation?.set(pitch, yaw, 0f)
        npcTransform.rotation.set(0f, yaw, 0f)

        val stateSupport = store.getComponent(npcRef, StateSupport.getComponentType())
        stateSupport?.isClearHeadMotion = true
    }

    fun restoreNpcGaze(npcRef: Ref<EntityStore>) {
        if (!npcRef.isValid) {
            activeNpcCameraPositions.remove(npcRef)
            return
        }

        val stillActive = activeNpcForPlayer.values.any { it == npcRef }
        if (!stillActive) {
            activeNpcCameraPositions.remove(npcRef)
            val store = npcRef.store
            val stateSupport = store.getComponent(npcRef, StateSupport.getComponentType())
            stateSupport?.isClearHeadMotion = false
        }
    }

    private fun hideNearbyPlayers(playerRef: PlayerRef, center: Vector3d, radius: Double) {
        val ref = playerRef.reference ?: return
        val world = ref.store.externalData.world
        val radiusSq = radius * radius
        val hiddenSet = hiddenPlayersForViewer.computeIfAbsent(playerRef.uuid) { ConcurrentHashMap.newKeySet() }

        val playerManager = playerRef.hiddenPlayersManager
        for (other in world.playerRefs) {
            if (other.uuid == playerRef.uuid) continue
            val otherPos = other.transform.position
            val dx = otherPos.x - center.x
            val dy = otherPos.y - center.y
            val dz = otherPos.z - center.z
            if (dx * dx + dy * dy + dz * dz <= radiusSq) {
                playerManager.hidePlayer(other.uuid)
                hiddenSet.add(other.uuid)
            }
        }
    }

    fun resetCamera(playerRef: PlayerRef) {
        activeCameraPoints.remove(playerRef.uuid)
        playerRef.packetHandler.writeNoCache(
            SetServerCamera(ClientCameraView.Custom, false, null)
        )

        val npcRef = activeNpcForPlayer.remove(playerRef.uuid)
        if (npcRef != null) {
            restoreNpcGaze(npcRef)
        }

        // Unhide players
        val hiddenSet = hiddenPlayersForViewer.remove(playerRef.uuid)
        if (hiddenSet != null) {
            val playerManager = playerRef.hiddenPlayersManager
            for (uuid in hiddenSet) {
                playerManager.showPlayer(uuid)
            }
        }

        val ref = playerRef.reference ?: run {
            originalTransforms.remove(playerRef.uuid)
            return
        }

        val original = originalTransforms.remove(playerRef.uuid) ?: return
        val currentTransform = ref.store.getComponent(ref, TransformComponent.getComponentType())

        // Return player
        if (currentTransform != null) {
            val moved = currentTransform.position.distanceSquared(original.position) > 0.0001
            val rotated = currentTransform.rotation.x != original.bodyRotation.x ||
                    currentTransform.rotation.y != original.bodyRotation.y ||
                    currentTransform.rotation.z != original.bodyRotation.z

            if (moved || rotated) {
                val teleport = Teleport.createExact(original.position, original.bodyRotation, original.headRotation)
                ref.store.putComponent(ref, Teleport.getComponentType(), teleport)
            }
        }
    }

    fun onPlayerDisconnect(playerRef: PlayerRef) {
        activeCameraPoints.remove(playerRef.uuid)
        originalTransforms.remove(playerRef.uuid)
        val npcRef = activeNpcForPlayer.remove(playerRef.uuid)
        if (npcRef != null) {
            restoreNpcGaze(npcRef)
        }
        val hiddenSet = hiddenPlayersForViewer.remove(playerRef.uuid)
        if (hiddenSet != null) {
            val playerManager = playerRef.hiddenPlayersManager
            for (uuid in hiddenSet) {
                playerManager.showPlayer(uuid)
            }
        }
    }
}
