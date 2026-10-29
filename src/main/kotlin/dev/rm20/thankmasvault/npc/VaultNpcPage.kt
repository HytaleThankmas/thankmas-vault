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
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo
import com.hypixel.hytale.server.core.ui.LocalizableString
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.assets.PoseAsset
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
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
        var model: String? = null
        var pointId: String? = null
        var dialogueId: String? = null
        var pose: String? = null
        var mainHandItem: String? = null
        var offHandItem: String? = null
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
                    KeyedCodec("@Model", Codec.STRING),
                    { data, s -> data.model = s },
                    { it.model ?: "" }
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
                    KeyedCodec("@Pose", Codec.STRING),
                    { data, s -> data.pose = s },
                    { it.pose ?: "" }
                ).add()
                .append(
                    KeyedCodec("@MainHandItem", Codec.STRING),
                    { data, s -> data.mainHandItem = s },
                    { it.mainHandItem ?: "" }
                ).add()
                .append(
                    KeyedCodec("@OffHandItem", Codec.STRING),
                    { data, s -> data.offHandItem = s },
                    { it.offHandItem ?: "" }
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
            "#ModelField",
            EventData.of("@Model", "#ModelField.Value"),
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

        val isPlayer = AppearanceHelper.isPlayerNpc(targetEntityRef, store)
        if (isPlayer) {
            commandBuilder.set("#PlayerAppearanceGroup.Visible", true)

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#CopyAppearanceButton",
                EventData.of("Action", "copy_appearance"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#FetchSkinByNameButton",
                EventData.of("Action", "fetch_skin_by_name"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#PoseDropdown",
                EventData.of("@Pose", "#PoseDropdown.Value"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#MainHandField",
                EventData.of("@MainHandItem", "#MainHandField.Value"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#OffHandField",
                EventData.of("@OffHandItem", "#OffHandField.Value"),
                false
            )

            val poses = PoseAsset.getAllPoses()
            val entries = ArrayList<DropdownEntryInfo>()
            entries.add(DropdownEntryInfo(LocalizableString.fromString("None"), "None"))
            for (p in poses) {
                entries.add(DropdownEntryInfo(LocalizableString.fromString(p.getLabel()), p.name))
            }
            commandBuilder.set("#PoseDropdown.Entries", entries)
        } else {
            commandBuilder.set("#PlayerAppearanceGroup.Visible", false)
            commandBuilder.remove("#PlayerAppearanceGroup")
        }

        val config = store.getComponent(targetEntityRef, VaultNpcComponent.componentType)
        commandBuilder.set("#DisplayNameField.Value", config?.displayName ?: "")
        commandBuilder.set("#ShopIdField.Value", config?.shopId ?: "")
        commandBuilder.set("#ModelField.Value", config?.model ?: "")
        commandBuilder.set("#PointIdField.Value", config?.pointId ?: "")
        commandBuilder.set("#DialogueIdField.Value", config?.dialogueId ?: "")
        if (isPlayer) {
            commandBuilder.set("#PoseDropdown.Value", config?.pose?.ifBlank { "None" } ?: "None")
            commandBuilder.set("#MainHandField.Value", config?.mainHandItem ?: "")
            commandBuilder.set("#OffHandField.Value", config?.offHandItem ?: "")
        }
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
            val newName = data.displayName!!.trim()
            config.displayName = newName
            changed = true
            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    store.putComponent(
                        targetEntityRef,
                        DisplayNameComponent.getComponentType(),
                        DisplayNameComponent(Message.raw(newName))
                    )
                }
            }
        }

        if (data.shopId != null) {
            config.shopId = data.shopId!!.trim()
            changed = true
        }

        if (data.model != null) {
            val newModel = data.model!!.trim()
            if (config.model != newModel) {
                config.model = newModel
                changed = true
                store.externalData.world.execute {
                    if (targetEntityRef.isValid) {
                        if (newModel.isNotBlank()) {
                            val applied = AppearanceHelper.applyModelToNpc(targetEntityRef, store, newModel)
                            if (applied) {
                                playerRef.sendMessage(Message.raw("[VaultNPC] Changed model to '$newModel'."))
                            } else {
                                playerRef.sendMessage(Message.raw("[VaultNPC] Warning: Model '$newModel' not found in asset map."))
                            }
                        } else {
                            if (AppearanceHelper.isPlayerNpc(targetEntityRef, store)) {
                                AppearanceHelper.applySavedAppearance(targetEntityRef, store, config)
                            }
                            playerRef.sendMessage(Message.raw("[VaultNPC] Reset to default model."))
                        }
                    }
                }
            }
        }

        if (data.pointId != null) {
            config.pointId = data.pointId!!.trim()
            changed = true
        }

        if (data.dialogueId != null) {
            config.dialogueId = data.dialogueId!!.trim()
            changed = true
        }

        val isPlayer = AppearanceHelper.isPlayerNpc(targetEntityRef, store)
        if (isPlayer) {
            var playerNpcComp = store.getComponent(targetEntityRef, PlayerNpcComponent.componentType)
            var playerCompChanged = false
            if (playerNpcComp == null) {
                playerNpcComp = PlayerNpcComponent()
                playerCompChanged = true
            }

            if (data.pose != null) {
                val newPose = data.pose!!.trim()
                val targetPose = if (newPose.equals("None", ignoreCase = true)) "" else newPose
                if (config.pose != targetPose) {
                    config.pose = targetPose
                    playerNpcComp.pose = targetPose
                    changed = true
                    playerCompChanged = true
                    store.externalData.world.execute {
                        if (targetEntityRef.isValid) {
                            AppearanceHelper.applyPose(targetEntityRef, store, targetPose)
                        }
                    }
                }
            }
            if (data.mainHandItem != null) {
                val newItem = data.mainHandItem!!.trim()
                if (config.mainHandItem != newItem) {
                    config.mainHandItem = newItem
                    playerNpcComp.mainHandItem = newItem
                    changed = true
                    playerCompChanged = true
                    store.externalData.world.execute {
                        if (targetEntityRef.isValid) {
                            AppearanceHelper.equipItems(targetEntityRef, store, newItem, config.offHandItem)
                        }
                    }
                }
            }
            if (data.offHandItem != null) {
                val newItem = data.offHandItem!!.trim()
                if (config.offHandItem != newItem) {
                    config.offHandItem = newItem
                    playerNpcComp.offHandItem = newItem
                    changed = true
                    playerCompChanged = true
                    store.externalData.world.execute {
                        if (targetEntityRef.isValid) {
                            AppearanceHelper.equipItems(targetEntityRef, store, config.mainHandItem, newItem)
                        }
                    }
                }
            }

            if (playerCompChanged) {
                store.externalData.world.execute {
                    if (targetEntityRef.isValid) {
                        store.putComponent(targetEntityRef, PlayerNpcComponent.componentType, playerNpcComp)
                    }
                }
            }
        }

        if (data.action == "spawn_point") {
            val pointName = if (config.displayName.isNotBlank()) "${config.displayName}_Camera" else "NPC_Camera_" + getRandomString(5)
            val spawnedPoint = store.spawnCameraPointForNpc(playerRef, targetEntityRef, pointName)
            config.pointId = spawnedPoint.id
            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)
                }
            }

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
            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)
                }
            }

            val updateCommand = UICommandBuilder()
            updateCommand.set("#PointIdField.Value", closest.id)
            sendUpdate(updateCommand, false)

            playerRef.sendMessage(Message.raw("[VaultNPC] Found closest point '${closest.id}' (${closest.name ?: "unnamed"}) and updated Point ID."))
            return
        }

        if (data.action == "copy_appearance") {
            if (!AppearanceHelper.isPlayerNpc(targetEntityRef, store)) {
                playerRef.sendMessage(Message.raw("[VaultNPC] Not a player NPC"))
                return
            }

            val playerRefEntity = playerRef.reference
            if (playerRefEntity != null && playerRefEntity.isValid) {
                AppearanceHelper.copyPlayerAppearanceToNpc(
                    playerRefEntity,
                    targetEntityRef,
                    store,
                    saveToConfig = true
                )

                val updatedConfig = store.getComponent(targetEntityRef, VaultNpcComponent.componentType)
                val updateCommand = UICommandBuilder()
                updateCommand.set("#MainHandField.Value", updatedConfig?.mainHandItem ?: "")
                updateCommand.set("#OffHandField.Value", updatedConfig?.offHandItem ?: "")
                sendUpdate(updateCommand, false)

                playerRef.sendMessage(Message.raw("[VaultNPC] Copied your appearance and held items to the NPC."))
            } else {
                playerRef.sendMessage(Message.raw("[VaultNPC] Player reference not found."))
            }
            return
        }

        if (data.action == "fetch_skin_by_name") {
            if (!AppearanceHelper.isPlayerNpc(targetEntityRef, store)) {
                playerRef.sendMessage(Message.raw("[VaultNPC] Not a player NPC"))
                return
            }

            val targetName = config.displayName
            if (targetName.isBlank()) {
                playerRef.sendMessage(Message.raw("[VaultNPC] NPC Display Name is empty. Enter a username in Display Name first."))
                return
            }

            playerRef.sendMessage(Message.raw("[VaultNPC] Searching skin for '$targetName'"))
            AppearanceHelper.copyPlayerAppearanceByUsername(
                targetName,
                targetEntityRef,
                store,
                saveToConfig = true
            ).thenAccept { success ->
                if (success) {
                    playerRef.sendMessage(Message.raw("[VaultNPC] Applied skin for '$targetName'!"))
                } else {
                    playerRef.sendMessage(Message.raw("[VaultNPC] Could not find skin for '$targetName'"))
                }
            }
            return
        }

        if (changed) {
            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    store.putComponent(targetEntityRef, VaultNpcComponent.componentType, config)
                }
            }
        }
    }

    override fun onDismiss(ref: Ref<EntityStore>, store: Store<EntityStore>) {
        super.onDismiss(ref, store)
        if (targetEntityRef.isValid && AppearanceHelper.isPlayerNpc(targetEntityRef, store)) {
            AppearanceHelper.applyPose(targetEntityRef, store)
        }
    }

    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    fun getRandomString(length: Int = 5): String =
        (1..length)
            .map { allowedChars.random() }
            .joinToString("")
}
