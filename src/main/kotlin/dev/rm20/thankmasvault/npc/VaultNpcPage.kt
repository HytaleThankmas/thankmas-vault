package dev.rm20.thankmasvault.npc

import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.utils.findClosestPointForPlayer
import dev.rm20.thankmasvault.utils.spawnCameraPointForNpc
import javax.annotation.Nonnull

class VaultNpcPage(
    playerRef: PlayerRef,
    private val targetEntityRef: Ref<EntityStore>
) : InteractiveCustomUIPage<VaultNpcPage.BindingData>(
    playerRef, CustomPageLifetime.CanDismissOrCloseThroughInteraction, BindingData.CODEC
) {

    class BindingData {
        var displayName: String? = null
        var shopId: String? = null
        var pointId: String? = null
        var dialogueId: String? = null
        var action: String? = null

        companion object {
            @JvmField
            val CODEC: BuilderCodec<BindingData> = BuilderCodec.builder(BindingData::class.java, ::BindingData)
                .append(
                    KeyedCodec("@DisplayName", Codec.STRING),
                    { data, s -> data.displayName = s },
                    { it.displayName ?: "" }
                ).add()
                .append(
                    KeyedCodec("@ShopId", Codec.STRING),
                    { data, s -> data.shopId = s },
                    { it.shopId ?: "" }
                ).add()
                .append(
                    KeyedCodec("@PointId", Codec.STRING),
                    { data, s -> data.pointId = s },
                    { it.pointId ?: "" }
                ).add()
                .append(
                    KeyedCodec("@DialogueId", Codec.STRING),
                    { data, s -> data.dialogueId = s },
                    { it.dialogueId ?: "" }
                ).add()
                .append(
                    KeyedCodec("Action", Codec.STRING),
                    { data, s -> data.action = s },
                    { it.action ?: "" }
                ).add()
                .build()
        }
    }

    override fun build(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull commandBuilder: UICommandBuilder,
        @Nonnull eventBuilder: UIEventBuilder,
        @Nonnull store: Store<EntityStore>
    ) {
        commandBuilder.append("NPC/VaultNpcConfig.ui")

        commandBuilder.set("#TitleLabel.Text", "Vault NPC Config")

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DisplayNameField",
            EventData.of("@DisplayName", "#DisplayNameField.Value"),
            false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#ShopIdField",
            EventData.of("@ShopId", "#ShopIdField.Value"),
            false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#PointIdField",
            EventData.of("@PointId", "#PointIdField.Value"),
            false
        )
        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DialogueIdField",
            EventData.of("@DialogueId", "#DialogueIdField.Value"),
            false
        )

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#SpawnPointButton",
            EventData.of("Action", "spawn_point"),
            false
        )

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.Activating,
            "#LocateCloestPointButton",
            EventData.of("Action", "find_point"),
            false
        )

        val config = store.getComponent(targetEntityRef, VaultNpcComponent.componentType)
        commandBuilder.set("#DisplayNameField.Value", config?.displayName ?: "")
        commandBuilder.set("#ShopIdField.Value", config?.shopId ?: "")
        commandBuilder.set("#PointIdField.Value", config?.pointId ?: "")
        commandBuilder.set("#DialogueIdField.Value", config?.dialogueId ?: "")
    }

    override fun handleDataEvent(
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull store: Store<EntityStore>,
        @Nonnull data: BindingData
    ) {
        super.handleDataEvent(ref, store, data)

        if (!targetEntityRef.isValid) {
            playerRef.sendMessage(Message.raw("[VaultNPC] Target entity is no longer valid."))
            close()
            return
        }

        var config = store.getComponent(targetEntityRef, VaultNpcComponent.componentType)
        if (config == null) {
            config = VaultNpcComponent()
        }

        var changed = false

        if (data.displayName != null) {
            config.displayName = data.displayName!!.trim()
            changed = true
        }
        if (data.shopId != null) {
            config.shopId = data.shopId!!.trim()
            changed = true
        }
        if (data.pointId != null) {
            config.pointId = data.pointId!!.trim()
            changed = true
        }
        if (data.dialogueId != null) {
            config.dialogueId = data.dialogueId!!.trim()
            changed = true
        }

        if (data.action == "spawn_point") {
            val pointName = if (config.displayName.isNotBlank()) "${config.displayName}_Camera" else "NPC_Camera_" + getRandomString(5)
            val spawnedPoint = store.spawnCameraPointForNpc(playerRef, targetEntityRef, pointName)
            config.pointId = spawnedPoint.id
            store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)

            val updateCommand = UICommandBuilder()
            updateCommand.set("#PointIdField.Value", spawnedPoint.id)
            sendUpdate(updateCommand, false)

            playerRef.sendMessage(Message.raw("[VaultNPC] Spawned a camera point '${spawnedPoint.id}' for NPC and updated Point ID."))
            return
        }

        if (data.action == "find_point") {
            val closest = store.findClosestPointForPlayer(playerRef)
            if (closest == null) {
                playerRef.sendMessage(Message.raw("[VaultNPC] No points found in this world."))
                return
            }

            config.pointId = closest.id
            store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)

            val updateCommand = UICommandBuilder()
            updateCommand.set("#PointIdField.Value", closest.id)
            sendUpdate(updateCommand, false)

            playerRef.sendMessage(Message.raw("[VaultNPC] Found closest point '${closest.id}' (${closest.name ?: "unnamed"}) and updated Point ID."))
            return
        }

        if (changed) {
            store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)
        }
    }


    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    fun getRandomString(length: Int = 5): String =
        (1..length)
            .map { allowedChars.random() }
            .joinToString("")
}