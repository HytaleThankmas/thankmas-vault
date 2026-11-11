package dev.rm20.thankmasvault.npc

import acorn.thankmasdelivery.ThankmasDeliveryPlugin
import acorn.thankmasdelivery.component.DeliveryNpc
import acorn.thankmasdelivery.persistence.DeliveryStore
import com.hypixel.hytale.codec.Codec
import com.hypixel.hytale.codec.KeyedCodec
import com.hypixel.hytale.codec.builder.BuilderCodec
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.protocol.InteractionType
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.entity.UUIDComponent
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
import com.hypixel.hytale.server.core.modules.entity.component.Interactable
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.interaction.Interactions
import com.hypixel.hytale.server.core.ui.DropdownEntryInfo
import com.hypixel.hytale.server.core.ui.LocalizableString
import com.hypixel.hytale.server.core.ui.builder.EventData
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.role.support.DisplayNameSupport
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

    private var lastDeliveryKind: String = "none"
    private var lastDeliveryId: String = ""
    private var lastDeliveryBonusPoints: Int = 0
    private var lastDeliveryHint: String = ""

    class BindingData {
        var displayName: String? = null
        var shopId: String? = null
        var model: String? = null
        var pointId: String? = null
        var dialogueId: String? = null
        var dialogueText: String? = null
        var pose: String? = null
        var mainHandItem: String? = null
        var offHandItem: String? = null
        var action: String? = null

        var npcType: String? = null
        var statueOverlay: String? = null
        var statueRotation: String? = null

        var deliveryKind: String? = null
        var deliveryId: String? = null
        var deliveryBonusPoints: String? = null
        var deliveryHint: String? = null

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
                    KeyedCodec("@DialogueText", Codec.STRING),
                    { data, s -> data.dialogueText = s },
                    { it.dialogueText ?: "" }
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
                    KeyedCodec("@NpcType", Codec.STRING),
                    { data, s -> data.npcType = s },
                    { it.npcType ?: "" }
                ).add()
                .append(
                    KeyedCodec("@StatueOverlay", Codec.STRING),
                    { data, s -> data.statueOverlay = s },
                    { it.statueOverlay ?: "" }
                ).add()
                .append(
                    KeyedCodec("@StatueRotation", Codec.STRING),
                    { data, s -> data.statueRotation = s },
                    { it.statueRotation ?: "" }
                ).add()
                .append(
                    KeyedCodec("@DeliveryKind", Codec.STRING),
                    { data, s -> data.deliveryKind = s },
                    { it.deliveryKind ?: "" }
                ).add()
                .append(
                    KeyedCodec("@DeliveryId", Codec.STRING),
                    { data, s -> data.deliveryId = s },
                    { it.deliveryId ?: "" }
                ).add()
                .append(
                    KeyedCodec("@DeliveryBonusPoints", Codec.STRING),
                    { data, s -> data.deliveryBonusPoints = s },
                    { it.deliveryBonusPoints ?: "" }
                ).add()
                .append(
                    KeyedCodec("@DeliveryHint", Codec.STRING),
                    { data, s -> data.deliveryHint = s },
                    { it.deliveryHint ?: "" }
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
            CustomUIEventBindingType.ValueChanged,
            "#DialogueTextField",
            EventData.of("@DialogueText", "#DialogueTextField.Value"),
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
        val isPlayer = AppearanceHelper.isPlayerNpc(targetEntityRef, store)
        val isStatue = AppearanceHelper.isStatue(targetEntityRef, store)

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
                "#NpcTypeDropdown",
                EventData.of("@NpcType", "#NpcTypeDropdown.Value"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#StatueOverlayDropdown",
                EventData.of("@StatueOverlay", "#StatueOverlayDropdown.Value"),
                false
            )

            eventBuilder.addEventBinding(
                CustomUIEventBindingType.ValueChanged,
                "#StatueRotationField",
                EventData.of("@StatueRotation", "#StatueRotationField.Value"),
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

            val typeEntries = ArrayList<DropdownEntryInfo>()
            typeEntries.add(DropdownEntryInfo(LocalizableString.fromString("Statue"), "statue"))
            typeEntries.add(DropdownEntryInfo(LocalizableString.fromString("Parcel Recipient"), "recipient"))
            commandBuilder.set("#NpcTypeDropdown.Entries", typeEntries)
            commandBuilder.set("#NpcTypeDropdown.Value", if (isStatue) "statue" else "recipient")

            val overlayEntries = ArrayList<DropdownEntryInfo>()
            overlayEntries.add(DropdownEntryInfo(LocalizableString.fromString("Stone Gray"), "stone"))
            overlayEntries.add(DropdownEntryInfo(LocalizableString.fromString("Gold"), "gold"))
            overlayEntries.add(DropdownEntryInfo(LocalizableString.fromString("None (Original)"), "none"))
            commandBuilder.set("#StatueOverlayDropdown.Entries", overlayEntries)
            commandBuilder.set("#StatueOverlayDropdown.Value", config?.statueOverlay?.ifBlank { "stone" } ?: "stone")

            val poses = PoseAsset.getAllPoses()
            val entries = ArrayList<DropdownEntryInfo>()
            entries.add(DropdownEntryInfo(LocalizableString.fromString("None"), "None"))
            for (p in poses) {
                entries.add(DropdownEntryInfo(LocalizableString.fromString(p.getLabel()), p.name))
            }
            commandBuilder.set("#PoseDropdown.Entries", entries)

            val currentRotDeg = dev.rm20.thankmasvault.npc.statue.StatueOrientationService.getStatueYawDegrees(targetEntityRef, store)
            commandBuilder.set("#StatueRotationField.Value", Math.round(currentRotDeg).toString())

            commandBuilder.set("#StatueOverlayGroup.Visible", isStatue)
            commandBuilder.set("#StatueRotationGroup.Visible", isStatue)
            commandBuilder.set("#DeliveryNpcGroup.Visible", !isStatue)
        } else {
            commandBuilder.set("#PlayerAppearanceGroup.Visible", false)
            commandBuilder.remove("#PlayerAppearanceGroup")
            commandBuilder.set("#DeliveryNpcGroup.Visible", true)
        }

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DeliveryKindDropdown",
            EventData.of("@DeliveryKind", "#DeliveryKindDropdown.Value"),
            false
        )

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DeliveryIdField",
            EventData.of("@DeliveryId", "#DeliveryIdField.Value"),
            false
        )

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DeliveryBonusPointsField",
            EventData.of("@DeliveryBonusPoints", "#DeliveryBonusPointsField.Value"),
            false
        )

        eventBuilder.addEventBinding(
            CustomUIEventBindingType.ValueChanged,
            "#DeliveryHintField",
            EventData.of("@DeliveryHint", "#DeliveryHintField.Value"),
            false
        )

        val kindEntries = ArrayList<DropdownEntryInfo>()
        kindEntries.add(DropdownEntryInfo(LocalizableString.fromString("None"), "none"))
        kindEntries.add(DropdownEntryInfo(LocalizableString.fromString("Dispatcher"), "dispatcher"))
        kindEntries.add(DropdownEntryInfo(LocalizableString.fromString("Recipient"), "recipient"))
        commandBuilder.set("#DeliveryKindDropdown.Entries", kindEntries)

        val dType = DeliveryNpc.getComponentType()
        val existingDelivery = if (dType != null) store.getComponent(targetEntityRef, dType) else null

        val service = ThankmasDeliveryPlugin.get()?.deliveryService()
        if (existingDelivery != null) {
            lastDeliveryKind = existingDelivery.kind()
            lastDeliveryId = existingDelivery.id()
            lastDeliveryBonusPoints = existingDelivery.bonusPoints
            lastDeliveryHint = existingDelivery.hint().ifBlank {
                if (existingDelivery.id().isNotBlank()) {
                    service?.deliveryStore()?.recipient(existingDelivery.id())?.hint() ?: ""
                } else ""
            }
        } else {
            lastDeliveryKind = if (isPlayer && !isStatue) "recipient" else "none"
            lastDeliveryId = DeliveryNpc.generateId(config?.displayName)
            lastDeliveryBonusPoints = 0
            lastDeliveryHint = ""
        }

        commandBuilder.set("#DeliveryKindDropdown.Value", lastDeliveryKind)
        commandBuilder.set("#DeliveryIdField.Value", lastDeliveryId)
        commandBuilder.set("#DeliveryBonusPointsField.Value", lastDeliveryBonusPoints.toString())
        commandBuilder.set("#DeliveryHintField.Value", lastDeliveryHint)

        commandBuilder.set("#DisplayNameField.Value", config?.displayName ?: "")
        commandBuilder.set("#ShopIdField.Value", config?.shopId ?: "")
        commandBuilder.set("#ModelField.Value", config?.model ?: "")
        commandBuilder.set("#PointIdField.Value", config?.pointId ?: "")
        commandBuilder.set("#DialogueIdField.Value", config?.dialogueId ?: "")
        commandBuilder.set("#DialogueTextField.Value", config?.dialogueText ?: "")
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

            val autoId = DeliveryNpc.generateId(newName)
            val oldId = lastDeliveryId
            lastDeliveryId = autoId

            val updateCmd = UICommandBuilder()
            updateCmd.set("#DeliveryIdField.Value", autoId)
            sendUpdate(updateCmd, false)

            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    NpcNameplateHelper.applyNameplate(targetEntityRef, newName, store)
                    val dType = DeliveryNpc.getComponentType()
                    if (dType != null) {
                        val dNpc = store.getComponent(targetEntityRef, dType)
                        if (dNpc != null) {
                            dNpc.setName(newName)
                            val prevId = dNpc.id()
                            dNpc.setId(autoId)
                            store.putComponent(targetEntityRef, dType, dNpc)
                            if (dNpc.recipient()) {
                                syncRecipientDestination(targetEntityRef, store, dNpc, prevId)
                            }
                        }
                    }
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

        if (data.dialogueText != null) {
            config.dialogueText = data.dialogueText!!.trim()
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

            if (data.npcType != null) {
                val chosenType = data.npcType!!.trim().lowercase()
                val makeStatue = chosenType == "statue"
                if (config.isStatue != makeStatue) {
                    config.isStatue = makeStatue
                    playerNpcComp.isStatue = makeStatue
                    changed = true
                    playerCompChanged = true
                    val dType = DeliveryNpc.getComponentType()

                    store.externalData.world.execute {
                        if (targetEntityRef.isValid) {
                            if (makeStatue) {
                                // statue
                                if (dType != null) {
                                    val curr = store.getComponent(targetEntityRef, dType)
                                    if (curr != null && curr.recipient()) {
                                        val service = ThankmasDeliveryPlugin.get()?.deliveryService()
                                        service?.deliveryStore()?.removeRecipient(curr.id())
                                    }
                                    store.tryRemoveComponent(targetEntityRef, dType)
                                }
                                dev.rm20.thankmasvault.npc.statue.StatueOrientationService.ensureStatuePoint(targetEntityRef, store, config)
                                AppearanceHelper.enforceStatueState(targetEntityRef, store, config.statueOverlay)
                                AppearanceHelper.applySavedAppearance(targetEntityRef, store, config)
                                val updateCmd = UICommandBuilder()
                                updateCmd.set("#StatueOverlayGroup.Visible", true)
                                updateCmd.set("#StatueRotationGroup.Visible", true)
                                val currentDeg = dev.rm20.thankmasvault.npc.statue.StatueOrientationService.getStatueYawDegrees(targetEntityRef, store)
                                updateCmd.set("#StatueRotationField.Value", Math.round(currentDeg).toString())
                                updateCmd.set("#DeliveryNpcGroup.Visible", false)
                                sendUpdate(updateCmd, false)
                                playerRef.sendMessage(Message.raw("[VaultNPC] Switched NPC to Statue mode (interactions & gaze disabled)."))
                            } else {
                                // parcel
                                dev.rm20.thankmasvault.npc.statue.StatueOrientationService.removeStatuePoint(config.pointId, store)
                                config.pointId = ""
                                AppearanceHelper.clearStatueOverlay(targetEntityRef, store)
                                AppearanceHelper.applySavedAppearance(targetEntityRef, store, config)
                                val autoId =
                                    if (lastDeliveryId.isNotBlank()) lastDeliveryId else DeliveryNpc.generateId(config.displayName)
                                lastDeliveryId = autoId
                                lastDeliveryKind = "recipient"
                                val marker = DeliveryNpc("recipient", autoId, config.displayName)
                                if (dType != null) {
                                    store.putComponent(targetEntityRef, dType, marker)
                                }
                                val interactions = store.getComponent(targetEntityRef, Interactions.getComponentType())
                                    ?: Interactions()
                                interactions.setInteractionId(InteractionType.Use, "Parcel_Delivery_NPC")
                                interactions.setInteractionHint("server.interactionHints.Parcel_Recipient")
                                store.putComponent(targetEntityRef, Interactions.getComponentType(), interactions)
                                store.putComponent(
                                    targetEntityRef,
                                    Interactable.getComponentType(),
                                    Interactable.INSTANCE
                                )
                                syncRecipientDestination(targetEntityRef, store, marker)

                                val updateCmd = UICommandBuilder()
                                updateCmd.set("#StatueOverlayGroup.Visible", false)
                                updateCmd.set("#StatueRotationGroup.Visible", false)
                                updateCmd.set("#DeliveryNpcGroup.Visible", true)
                                updateCmd.set("#DeliveryKindDropdown.Value", "recipient")
                                updateCmd.set("#DeliveryIdField.Value", autoId)
                                sendUpdate(updateCmd, false)
                                playerRef.sendMessage(Message.raw("[VaultNPC] Switched NPC to Parcel Recipient mode."))
                            }
                        }
                    }
                }
            }

            if (data.statueOverlay != null) {
                val newOverlay = data.statueOverlay!!.trim().lowercase()
                if (config.statueOverlay != newOverlay) {
                    config.statueOverlay = newOverlay
                    playerNpcComp.statueOverlay = newOverlay
                    changed = true
                    playerCompChanged = true
                    store.externalData.world.execute {
                        if (targetEntityRef.isValid && config.isStatue) {
                            AppearanceHelper.applyStatueOverlay(targetEntityRef, store, newOverlay)
                            AppearanceHelper.applySavedAppearance(targetEntityRef, store, config)
                            playerRef.sendMessage(Message.raw("[VaultNPC] Updated statue overlay to '$newOverlay'."))
                        }
                    }
                }
            }

            if (data.statueRotation != null) {
                val inputAngle = data.statueRotation!!.trim().toFloatOrNull()
                if (inputAngle != null) {
                    store.externalData.world.execute {
                        if (targetEntityRef.isValid && config.isStatue) {
                            dev.rm20.thankmasvault.npc.statue.StatueOrientationService.rotateStatue(
                                targetEntityRef, store, inputAngle
                            )
                            playerRef.sendMessage(Message.raw("[VaultNPC] Updated statue rotation to ${Math.round(inputAngle)}°."))
                        }
                    }
                }
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


        if (!config.isStatue) {
            val dType = DeliveryNpc.getComponentType()
            var currentDelivery = if (dType != null) store.getComponent(targetEntityRef, dType) else null
            var deliveryChanged = false
            var kindChanged = false

            if (data.deliveryKind != null) {
                val newKind = data.deliveryKind!!.trim().lowercase()
                if (lastDeliveryKind != newKind) {
                    lastDeliveryKind = newKind
                    kindChanged = true
                }
            }

            if (data.deliveryId != null) {
                val inputId = data.deliveryId!!.trim()
                val newId = if (inputId.isNotBlank()) {
                    inputId.lowercase().replace("\\s+".toRegex(), "").replace("[^a-z0-9_-]".toRegex(), "")
                } else {
                    DeliveryNpc.generateId(config.displayName)
                }
                if (lastDeliveryId != newId) {
                    val oldId = lastDeliveryId
                    lastDeliveryId = newId
                    val updateCmd = UICommandBuilder()
                    updateCmd.set("#DeliveryIdField.Value", newId)
                    sendUpdate(updateCmd, false)

                    if (currentDelivery != null) {
                        currentDelivery.setId(newId)
                        deliveryChanged = true
                        if (currentDelivery.recipient() && oldId.isNotBlank()) {
                            val service = ThankmasDeliveryPlugin.get()?.deliveryService()
                            service?.deliveryStore()?.removeRecipient(oldId)
                        }
                    }
                }
            }

            if (data.deliveryBonusPoints != null) {
                val rawBonus = data.deliveryBonusPoints!!.trim().removePrefix("+").trim()
                val newBonus = rawBonus.toIntOrNull() ?: 0
                if (lastDeliveryBonusPoints != newBonus) {
                    lastDeliveryBonusPoints = newBonus
                    if (currentDelivery != null) {
                        currentDelivery.setBonusPoints(newBonus)
                        deliveryChanged = true
                    }
                }
            }

            if (data.deliveryHint != null) {
                val newHint = data.deliveryHint!!.trim()
                if (lastDeliveryHint != newHint) {
                    lastDeliveryHint = newHint
                    if (currentDelivery != null) {
                        currentDelivery.setHint(newHint)
                        deliveryChanged = true
                    }
                    if (lastDeliveryKind == "recipient" && lastDeliveryId.isNotBlank()) {
                        val service = ThankmasDeliveryPlugin.get()?.deliveryService()
                        if (service != null) {
                            val world = store.externalData.world
                            service.adminAction(world, playerRef, {
                                service.deliveryStore().setLocationHint(lastDeliveryId, newHint)
                            }, {}, { })
                        }
                    }
                }
            }

            if (kindChanged) {
                if (lastDeliveryKind == "none") {
                    // Selecting "none" -> should not be a deliveryNPC
                    if (currentDelivery != null && dType != null) {
                        val wasRecipient = currentDelivery.recipient()
                        val recipientId = currentDelivery.id()
                        store.externalData.world.execute {
                            if (targetEntityRef.isValid) {
                                store.removeComponent(targetEntityRef, dType)
                                val interactions = store.getComponent(targetEntityRef, Interactions.getComponentType())
                                if (interactions != null && "Parcel_Delivery_NPC" == interactions.getInteractionId(
                                        InteractionType.Use
                                    )
                                ) {
                                    interactions.removeInteractionId(InteractionType.Use)
                                    interactions.setInteractionHint(null)
                                    store.putComponent(targetEntityRef, Interactions.getComponentType(), interactions)
                                }
                            }
                        }

                        if (wasRecipient) {
                            val service = ThankmasDeliveryPlugin.get()?.deliveryService()
                            service?.deliveryStore()?.removeRecipient(recipientId)
                        }

                        currentDelivery = null
                        playerRef.sendMessage(Message.raw("[VaultNPC] Delivery role removed from NPC."))
                    }
                } else {
                    // Selecting "dispatcher" or "recipient"
                    val nameToUse = if (config.displayName.isNotBlank()) config.displayName else {
                        if (lastDeliveryKind == "recipient") "Thankmas Recipient" else "Thankmas Dispatcher"
                    }
                    if (config.displayName.isBlank()) {
                        config.displayName = nameToUse
                        changed = true
                        val updateCommand = UICommandBuilder()
                        updateCommand.set("#DisplayNameField.Value", nameToUse)
                        sendUpdate(updateCommand, false)
                    }

                    if (lastDeliveryId.isBlank()) {
                        lastDeliveryId = DeliveryNpc.generateId(nameToUse)
                        val updateCommand = UICommandBuilder()
                        updateCommand.set("#DeliveryIdField.Value", lastDeliveryId)
                        sendUpdate(updateCommand, false)
                    }

                    val marker = DeliveryNpc(lastDeliveryKind, lastDeliveryId, nameToUse)
                    marker.setBonusPoints(lastDeliveryBonusPoints)
                    marker.setHint(lastDeliveryHint)
                    currentDelivery = marker
                    deliveryChanged = true

                    val service = ThankmasDeliveryPlugin.get()?.deliveryService()
                    if (service != null && lastDeliveryKind == "recipient") {
                        val world = store.externalData.world
                        service.adminAction(world, playerRef, {
                            service.deliveryStore().setLocationHint(lastDeliveryId, lastDeliveryHint)
                        }, {}, { })
                    }

                    store.externalData.world.execute {
                        if (targetEntityRef.isValid) {
                            if (dType != null) {
                                store.putComponent(targetEntityRef, dType, marker)
                            }
                            DisplayNameSupport.setDisplayName(targetEntityRef, nameToUse, store)
                            NpcNameplateHelper.applyNameplate(targetEntityRef, nameToUse, store)

                            var interactions = store.getComponent(targetEntityRef, Interactions.getComponentType())
                            if (interactions == null) {
                                interactions = Interactions()
                            }
                            interactions.setInteractionId(InteractionType.Use, "Parcel_Delivery_NPC")
                            interactions.setInteractionHint("server.interactionHints.Parcel_" + (if (lastDeliveryKind == "recipient") "Recipient" else "Dispatcher"))
                            store.putComponent(targetEntityRef, Interactions.getComponentType(), interactions)

                            if (store.getComponent(targetEntityRef, Interactable.getComponentType()) == null) {
                                store.addComponent(
                                    targetEntityRef,
                                    Interactable.getComponentType(),
                                    Interactable.INSTANCE
                                )
                            }

                            if (marker.recipient()) {
                                syncRecipientDestination(targetEntityRef, store, marker)
                            }
                        }
                    }

                    val oldId = lastDeliveryId
                    if (currentDelivery.recipient() && oldId.isNotBlank() && oldId != lastDeliveryId) {
                        val service2 = ThankmasDeliveryPlugin.get()?.deliveryService()
                        service2?.deliveryStore()?.removeRecipient(oldId)
                    }
                    playerRef.sendMessage(Message.raw("[VaultNPC] Changed Delivery kind to '$lastDeliveryKind'."))
                }
            }

            if (currentDelivery != null) {
                val nameToUse = config.displayName.ifBlank { currentDelivery.name() }
                if (currentDelivery.name() != nameToUse) {
                    currentDelivery.setName(nameToUse)
                    deliveryChanged = true
                }
            }

            if (deliveryChanged && currentDelivery != null && dType != null) {
                val toSave = currentDelivery
                store.externalData.world.execute {
                    if (targetEntityRef.isValid) {
                        store.putComponent(targetEntityRef, dType, toSave)
                        if (toSave.recipient()) {
                            syncRecipientDestination(targetEntityRef, store, toSave)
                        }
                    }
                }
            }
        }

        if (data.action == "spawn_point") {
            val pointName =
                if (config.displayName.isNotBlank()) "${config.displayName}_Camera" else "NPC_Camera_" + getRandomString(
                    5
                )
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

                if (config.isStatue) {
                    AppearanceHelper.enforceStatueState(targetEntityRef, store, config.statueOverlay)
                }

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
                    if (config.isStatue) {
                        AppearanceHelper.enforceStatueState(targetEntityRef, store, config.statueOverlay)
                    }
                    playerRef.sendMessage(
                        Message.raw("[VaultNPC] Successfully updated skin for '$targetName'.")
                    )
                } else {
                    playerRef.sendMessage(
                        Message.raw("[VaultNPC] Failed to fetch skin for '$targetName' via HyTags API. Check spelling.")
                    )
                }
            }
            return
        }

        if (changed) {
            val toSave = config
            store.externalData.world.execute {
                if (targetEntityRef.isValid) {
                    store.putComponent(targetEntityRef, VaultNpcComponent.componentType, toSave)
                }
            }
        }
    }

    private fun syncRecipientDestination(
        ref: Ref<EntityStore>,
        store: Store<EntityStore>,
        marker: DeliveryNpc,
        oldId: String? = null
    ) {
        val service = ThankmasDeliveryPlugin.get()?.deliveryService() ?: return
        val transform = store.getComponent(ref, TransformComponent.getComponentType()) ?: return
        val uuid = store.getComponent(ref, UUIDComponent.getComponentType()) ?: return
        val world = store.externalData.world
        val pos = transform.position
        val hintToUse = marker.hint().ifBlank {
            service.deliveryStore().recipient(marker.id())?.hint() ?: lastDeliveryHint
        }
        if (oldId != null && oldId.isNotBlank() && oldId != marker.id()) {
            service.deliveryStore().removeRecipient(oldId)
        }
        val destination = DeliveryStore.Destination(
            marker.id(),
            marker.name(),
            world.name,
            pos.x,
            pos.y,
            pos.z,
            hintToUse,
            uuid.uuid,
            world.worldConfig.uuid,
            false,
            "",
            marker.bonusPoints
        )
        service.deliveryStore().register(destination)
        service.observeRecipient(ref, marker)
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
