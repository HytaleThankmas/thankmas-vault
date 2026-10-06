package dev.rm20.thankmasvault.utils

import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.protocol.*
import com.hypixel.hytale.protocol.packets.camera.SetServerCamera
import com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
import com.hypixel.hytale.server.core.universe.PlayerRef
import org.joml.Vector3d


object CameraUtil {

    fun setFixedCameraFromPoint(
        playerRef: PlayerRef, pointId: String, backOffset: Double = 0.75
    ): Boolean {
        val ref = playerRef.reference ?: return false
        val pointEntry = ref.store.getPoint(pointId) ?: return false

        setFixedCameraAndTeleport(
            playerRef,
            pointEntry.position,
            pointEntry.rotation.x.toDouble(),
            pointEntry.rotation.y.toDouble(),
            pointEntry.rotation.z.toDouble(),
            pointEntry.transform.direction,
            backOffset
        )
        return true
    }

    fun setFixedCameraAndTeleport(
        playerRef: PlayerRef,
        position: Vector3d,
        rotX: Double,
        rotY: Double,
        rotZ: Double,
        forwardDirection: Vector3d,
        backOffset: Double = 0.75
    ) {
        val ref = playerRef.reference ?: return


        val yawRad = Math.toRadians(rotY).toFloat()
        val pitchRad = Math.toRadians(rotX).toFloat()
        val rollRad = Math.toRadians(rotZ).toFloat()

        // camera settings
        val cameraSettings = ServerCameraSettings().apply {
            positionLerpSpeed = 0.25f
            rotationLerpSpeed = 0.25f
            distance = 0f
            displayCursor = true
            isFirstPerson = false
            positionType = PositionType.Custom
            this.position = Position(position.x, position.y, position.z)
            rotationType = RotationType.Custom
            rotation = Direction(yawRad, pitchRad, rollRad)
        }

        playerRef.packetHandler.writeNoCache(
            SetServerCamera(ClientCameraView.Custom, true, cameraSettings)
        )

        // tp player
        val tpPos = Vector3d(
            position.x - forwardDirection.x * backOffset, position.y, position.z - forwardDirection.z * backOffset
        )

        val bodyRot = Rotation3f(0f, yawRad, 0f)
        val headRot = Rotation3f(pitchRad, yawRad, rollRad)

        val teleport = Teleport.createExact(tpPos, bodyRot, headRot)
        ref.store.putComponent(ref, Teleport.getComponentType(), teleport)
    }


    fun resetCamera(playerRef: PlayerRef) {
        playerRef.packetHandler.writeNoCache(
            SetServerCamera(ClientCameraView.Custom, false, null)
        )
    }
}