package dev.rm20.thankmasvault.npc.player

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.protocol.AnimationSlot
import com.hypixel.hytale.protocol.Packet
import com.hypixel.hytale.protocol.packets.entities.PlayEmote
import com.hypixel.hytale.protocol.packets.interface_.ChatMessage
import com.hypixel.hytale.server.core.entity.AnimationUtils
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters
import com.hypixel.hytale.server.core.io.adapter.PacketFilter
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.core.util.TargetUtil
import dev.rm20.thankmasvault.ThankmasVault
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.logging.Level

object NpcEmoteHandler {
    private val LOGGER: HytaleLogger = HytaleLogger.forEnclosingClass()
    private var packetFilter: PacketFilter? = null

    private val lastEmoteTimes = ConcurrentHashMap<Ref<EntityStore>, Long>()

    fun register(plugin: ThankmasVault) {
        if (packetFilter != null) return

        packetFilter = PacketAdapters.registerInbound(PlayerPacketWatcher { playerRef, packet ->
            handlePacket(playerRef, packet)
        })

        LOGGER.at(Level.INFO).log("Registered VaultNpcEmoteHandler for player emote interactions.")
    }

    fun unregister() {
        packetFilter?.let {
            try {
                PacketAdapters.deregisterInbound(it)
            } catch (e: Exception) {
                LOGGER.at(Level.WARNING).withCause(e).log("Failed to deregister VaultNpcEmoteHandler packet filter.")
            }
            packetFilter = null
        }
        lastEmoteTimes.clear()
    }

    private fun handlePacket(playerRef: PlayerRef, packet: Packet) {
        when (packet) {
            is PlayEmote -> {
                val emoteId = packet.emoteId
                if (!emoteId.isNullOrBlank()) {
                    triggerEmoteInteraction(playerRef, emoteId)
                }
            }
            is ChatMessage -> {
                val msg = packet.message
                if (!msg.isNullOrBlank() && msg.startsWith("/emote ", ignoreCase = true)) {
                    val parts = msg.removePrefix("/emote ").trim().split(" ")
                    if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                        triggerEmoteInteraction(playerRef, parts[0])
                    }
                }
            }
        }
    }

    private fun triggerEmoteInteraction(playerRef: PlayerRef, emoteId: String) {
        val playerEntityRef = playerRef.reference ?: return
        val store = playerEntityRef.store
        val world = store.externalData.world

        world.execute {
            if (!playerEntityRef.isValid) return@execute

            // 12 block range
            val targetNpcRef = findTargetPlayerNpc(playerEntityRef, store, 12.0f) ?: return@execute

            val now = System.currentTimeMillis()
            val lastTime = lastEmoteTimes[targetNpcRef] ?: 0L
            if (now - lastTime < 1800L) {
                return@execute
            }
            lastEmoteTimes[targetNpcRef] = now

            // look
            LookTowardsPlayer(targetNpcRef, playerEntityRef, store)

            // play emote
            if (targetNpcRef.isValid) {
                LookTowardsPlayer(targetNpcRef, playerEntityRef, store)

                AnimationUtils.playAnimation(
                    targetNpcRef,
                    AnimationSlot.Emote,
                    null,
                    emoteId,
                    true,
                    store
                )

                // Stop after 10
                world.scheduleAfter({
                    if (targetNpcRef.isValid) {
                        AnimationUtils.stopAnimation(targetNpcRef, AnimationSlot.Emote, true, store)
                    }
                }, 10000L, TimeUnit.MILLISECONDS)
            }
        }
    }

    private fun LookTowardsPlayer(
        npcRef: Ref<EntityStore>,
        playerRef: Ref<EntityStore>,
        store: Store<EntityStore>
    ) {
        val npcTransform = store.getComponent(npcRef, TransformComponent.getComponentType()) ?: return
        val playerTransform = store.getComponent(playerRef, TransformComponent.getComponentType()) ?: return

        val dx = playerTransform.position.x - npcTransform.position.x
        val dy = (playerTransform.position.y + 1.5) - (npcTransform.position.y + 1.5)
        val dz = playerTransform.position.z - npcTransform.position.z

        val lookRotation = Rotation3f.lookAt(dx, dy, dz)

        // Head
        val headRotation = store.getComponent(npcRef, HeadRotation.getComponentType())
        if (headRotation != null) {
            headRotation.rotation.set(lookRotation)
        }

        // Body
        npcTransform.rotation.setYaw(lookRotation.yaw())
    }

    private fun findTargetPlayerNpc(
        playerEntityRef: Ref<EntityStore>,
        store: Store<EntityStore>,
        maxDistance: Float
    ): Ref<EntityStore>? {
        // Raycast
        val directTarget = TargetUtil.getTargetEntity(playerEntityRef, maxDistance, store)
        if (directTarget != null && isPlayerNpc(directTarget, store)) {
            return directTarget
        }

        val playerTransform = store.getComponent(playerEntityRef, TransformComponent.getComponentType()) ?: return null
        val playerHeadRot = store.getComponent(playerEntityRef, HeadRotation.getComponentType()) ?: return null

        val playerEyePos = playerTransform.position
        val lookDir = playerHeadRot.direction

        // ~31 degree cone
        var bestTarget: Ref<EntityStore>? = null
        var bestDot = 0.85

        val entities = TargetUtil.getAllEntitiesInSphere(playerEyePos, maxDistance.toDouble(), store)
        for (candidateRef in entities) {
            if (candidateRef == null || !candidateRef.isValid || candidateRef == playerEntityRef) continue
            if (!isPlayerNpc(candidateRef, store)) continue

            val candidateTransform = store.getComponent(candidateRef, TransformComponent.getComponentType()) ?: continue
            val candidatePos = candidateTransform.position

            val dx = candidatePos.x - playerEyePos.x
            val dy = (candidatePos.y + 1.2) - (playerEyePos.y + 1.6)
            val dz = candidatePos.z - playerEyePos.z
            val distSq = dx * dx + dy * dy + dz * dz

            if (distSq > maxDistance * maxDistance || distSq < 0.01) continue

            val dist = Math.sqrt(distSq)
            val dirX = dx / dist
            val dirY = dy / dist
            val dirZ = dz / dist

            val dot = lookDir.x * dirX + lookDir.y * dirY + lookDir.z * dirZ
            if (dot > bestDot) {
                bestDot = dot
                bestTarget = candidateRef
            }
        }

        return bestTarget
    }

    private fun isPlayerNpc(ref: Ref<EntityStore>, store: Store<EntityStore>): Boolean {
        if (!ref.isValid) return false
        if (store.getComponent(ref, PlayerRef.getComponentType()) != null) return false
        return AppearanceHelper.isPlayerNpc(ref, store)
    }
}
