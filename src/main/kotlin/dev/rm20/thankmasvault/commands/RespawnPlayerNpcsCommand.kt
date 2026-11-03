package dev.rm20.thankmasvault.commands

import acorn.thankmasdelivery.component.DeliveryNpc
import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.RemoveReason
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.component.query.Query
import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.protocol.AnimationSlot
import com.hypixel.hytale.protocol.PlayerSkin
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.NameMatching
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.inventory.InventoryComponent
import com.hypixel.hytale.server.core.modules.entity.component.ActiveAnimationComponent
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.NPCPlugin
import com.hypixel.hytale.server.npc.entities.NPCEntity
import dev.hardaway.wardrobe.impl.player.PlayerWardrobeComponent
import dev.rm20.thankmasvault.npc.NpcNameplateHelper
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.player.HyTagsService
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
import org.joml.Vector3d
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import javax.annotation.Nonnull

class RespawnPlayerNpcsCommand : AbstractPlayerCommand(
    "respawnplayernpcs",
    "Respawns all player NPCs at their location, re-importing their skin"
) {

    private val nameArg = withOptionalArg("name", "Optional NPC name to filter", ArgTypes.STRING)

    init {
        addAliases("respawnplayernpc", "respawnpnpcs", "respawnpnpc", "respawnplayers", "reloadplayernpcs")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    private data class NpcSnapshot(
        val oldRef: Ref<EntityStore>,
        val position: Vector3d,
        val rotation: Rotation3f,
        val headRotation: Rotation3f,
        val roleIndex: Int,
        val vaultConfig: VaultNpcComponent,
        val playerConfig: PlayerNpcComponent,
        val wardrobeComponent: PlayerWardrobeComponent?,
        val deliveryNpc: DeliveryNpc?,
        val existingSkin: PlayerSkin?,
        val targetUsername: String
    )

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val targetFilter = if (nameArg.provided(context)) nameArg.get(context).trim() else null
        val filterAll = targetFilter.isNullOrBlank() || targetFilter.equals("all", ignoreCase = true)

        val npcCompType = NPCEntity.getComponentType()
        val query = if (npcCompType != null) {
            Query.or(
                PlayerNpcComponent.componentType,
                VaultNpcComponent.componentType,
                npcCompType
            )
        } else {
            Query.or(
                PlayerNpcComponent.componentType,
                VaultNpcComponent.componentType
            )
        }

        val snapshots = mutableListOf<NpcSnapshot>()

        store.forEachChunk(query, BiConsumer { chunk, _ ->
            for (i in 0 until chunk.size()) {
                // Ensure we never target real players
                if (chunk.getComponent(i, Player.getComponentType()) != null) continue

                val entityRef = chunk.getReferenceTo(i)
                if (!entityRef.isValid) continue

                val vaultComp = chunk.getComponent(i, VaultNpcComponent.componentType)
                val playerNpcComp = chunk.getComponent(i, PlayerNpcComponent.componentType)

                val isPlayerNpc = playerNpcComp != null ||
                        (vaultComp != null && vaultComp.skinJson.isNotBlank()) ||
                        AppearanceHelper.isPlayerNpc(entityRef, store)

                if (!isPlayerNpc) continue

                val displayName = vaultComp?.displayName?.trim() ?: ""

                if (!filterAll) {
                    val matches = displayName.equals(targetFilter, ignoreCase = true) ||
                            displayName.contains(targetFilter, ignoreCase = true)
                    if (!matches) continue
                }

                val transform = chunk.getComponent(i, TransformComponent.getComponentType()) ?: continue
                val headRot = chunk.getComponent(i, HeadRotation.getComponentType())?.rotation ?: transform.rotation

                val npcEntity = if (npcCompType != null) chunk.getComponent(i, npcCompType) else null
                val roleIndex = npcEntity?.roleIndex ?: AssetMapWithIndexes.NOT_FOUND

                val wardrobeComp = try {
                    val wType = PlayerWardrobeComponent.getComponentType()
                    if (wType != null) chunk.getComponent(i, wType)?.clone() else null
                } catch (_: Throwable) {
                    null
                }

                val deliveryComp = try {
                    val dType = DeliveryNpc.getComponentType()
                    if (dType != null) chunk.getComponent(i, dType)?.clone() else null
                } catch (_: Throwable) {
                    null
                }

                val existingSkinComp = chunk.getComponent(i, PlayerSkinComponent.getComponentType())
                val existingSkin = existingSkinComp?.playerSkin ?: run {
                    val json = vaultComp?.skinJson?.takeIf { it.isNotBlank() }
                        ?: playerNpcComp?.skinJson?.takeIf { it.isNotBlank() }
                    if (json != null) AppearanceHelper.jsonToPlayerSkin(json) else null
                }

                val currentMainHand = vaultComp?.mainHandItem?.takeIf { it.isNotBlank() }
                    ?: playerNpcComp?.mainHandItem?.takeIf { it.isNotBlank() }
                    ?: InventoryComponent.getItemInHand(store, entityRef)?.itemId
                    ?: ""

                val utilityComp = chunk.getComponent(i, InventoryComponent.Utility.getComponentType())
                val currentOffHand = vaultComp?.offHandItem?.takeIf { it.isNotBlank() }
                    ?: playerNpcComp?.offHandItem?.takeIf { it.isNotBlank() }
                    ?: utilityComp?.activeItem?.itemId
                    ?: ""

                val currentPose = vaultComp?.pose?.takeIf { it.isNotBlank() }
                    ?: playerNpcComp?.pose?.takeIf { it.isNotBlank() }
                    ?: chunk.getComponent(i, ActiveAnimationComponent.getComponentType())
                        ?.activeAnimations?.getOrNull(AnimationSlot.Status.ordinal)
                    ?: ""

                val finalVaultComp = (vaultComp?.clone() as? VaultNpcComponent) ?: VaultNpcComponent()
                finalVaultComp.mainHandItem = currentMainHand
                finalVaultComp.offHandItem = currentOffHand
                finalVaultComp.pose = currentPose

                val finalPlayerNpcComp = (playerNpcComp?.clone() as? PlayerNpcComponent) ?: PlayerNpcComponent()
                finalPlayerNpcComp.mainHandItem = currentMainHand
                finalPlayerNpcComp.offHandItem = currentOffHand
                finalPlayerNpcComp.pose = currentPose

                snapshots.add(
                    NpcSnapshot(
                        oldRef = entityRef,
                        position = Vector3d(transform.position.x, transform.position.y, transform.position.z),
                        rotation = Rotation3f(transform.rotation),
                        headRotation = Rotation3f(headRot),
                        roleIndex = roleIndex,
                        vaultConfig = finalVaultComp,
                        playerConfig = finalPlayerNpcComp,
                        wardrobeComponent = wardrobeComp,
                        deliveryNpc = deliveryComp,
                        existingSkin = existingSkin,
                        targetUsername = displayName
                    )
                )
            }
        })

        if (snapshots.isEmpty()) {
            val msg = if (filterAll) {
                "[VaultNPC] No player NPCs found in the current world."
            } else {
                "[VaultNPC] No player NPCs found matching '$targetFilter'."
            }
            context.sendMessage(Message.raw(msg))
            return
        }

        context.sendMessage(
            Message.raw("[VaultNPC] Found ${snapshots.size} player NPC(s). Re-importing skins and respawning...")
        )

        // Asynchronously re-import skins for each snapshot
        val skinFutures = snapshots.map { snapshot ->
            fetchUpdatedSkin(snapshot.targetUsername, snapshot.existingSkin)
                .thenApply { skin -> Pair(snapshot, skin) }
        }

        CompletableFuture.allOf(*skinFutures.toTypedArray()).thenAccept {
            val results = skinFutures.map { it.join() }

            world.execute {
                var respawnCount = 0
                val npcPlugin = NPCPlugin.get()

                for ((snapshot, reimportedSkin) in results) {
                    // Remove the old NPC entity
                    if (snapshot.oldRef.isValid) {
                        store.removeEntity(snapshot.oldRef, RemoveReason.REMOVE)
                    }

                    // Resolve valid NPC role index
                    var roleIdx = snapshot.roleIndex
                    if (roleIdx == AssetMapWithIndexes.NOT_FOUND || npcPlugin.getName(roleIdx) == null) {
                        roleIdx = npcPlugin.getIndex("Player_NPC")
                        if (roleIdx == AssetMapWithIndexes.NOT_FOUND) {
                            roleIdx = npcPlugin.getIndex("Shops/Player_NPC")
                        }
                        if (roleIdx == AssetMapWithIndexes.NOT_FOUND) {
                            roleIdx = npcPlugin.getIndex("Elf_NPC")
                        }
                        if (roleIdx == AssetMapWithIndexes.NOT_FOUND) {
                            roleIdx = npcPlugin.getIndex("Shops/Elf_NPC")
                        }
                    }

                    if (roleIdx == AssetMapWithIndexes.NOT_FOUND) {
                        continue
                    }

                    val finalSkin = reimportedSkin ?: snapshot.existingSkin
                    val validSkin = finalSkin?.let { AppearanceHelper.ensureValidSkin(it) }

                    if (validSkin != null) {
                        val skinJson = AppearanceHelper.playerSkinToJson(validSkin)
                        snapshot.vaultConfig.skinJson = skinJson
                        snapshot.playerConfig.skinJson = skinJson
                    }

                    val spawnModel: Model? = if (snapshot.vaultConfig.model.isNotBlank()) {
                        val asset = ModelAsset.getAssetMap().getAsset(snapshot.vaultConfig.model)
                        if (asset != null) Model.createScaledModel(asset, 1.0f, null) else null
                    } else if (validSkin != null) {
                        CosmeticsModule.get().createModel(validSkin)
                    } else null

                    val npcPair = npcPlugin.spawnEntity(
                        store,
                        roleIdx,
                        snapshot.position,
                        snapshot.headRotation,
                        spawnModel
                    ) { _, newEntityRef, entityStore ->
                        entityStore.externalData.world.execute {
                            if (!newEntityRef.isValid) return@execute

                            // Set exact body rotation
                            val transformComp = entityStore.getComponent(newEntityRef, TransformComponent.getComponentType())
                            if (transformComp != null) {
                                entityStore.putComponent(
                                    newEntityRef,
                                    TransformComponent.getComponentType(),
                                    TransformComponent(snapshot.position, snapshot.rotation)
                                )
                            }
                            entityStore.putComponent(
                                newEntityRef,
                                HeadRotation.getComponentType(),
                                HeadRotation(snapshot.headRotation)
                            )

                            // Apply PlayerSkinComponent
                            if (validSkin != null) {
                                val skinComp = PlayerSkinComponent(validSkin)
                                skinComp.setNetworkOutdated()
                                entityStore.putComponent(newEntityRef, PlayerSkinComponent.getComponentType(), skinComp)
                            }

                            // Build and apply model with wardrobe and custom attachments preserved
                            val savedData = AppearanceHelper.jsonToModelData(snapshot.vaultConfig.attachmentsJson)
                            val effectiveData = if (snapshot.vaultConfig.model.isNotBlank()) {
                                savedData.copy(modelAssetId = snapshot.vaultConfig.model)
                            } else {
                                savedData
                            }
                            val finalModel = AppearanceHelper.buildFinalModel(validSkin, effectiveData)
                            if (finalModel != null) {
                                entityStore.putComponent(newEntityRef, ModelComponent.getComponentType(), ModelComponent(finalModel))
                                entityStore.putComponent(newEntityRef, PersistentModel.getComponentType(), PersistentModel(finalModel.toReference()))
                                val attJson = AppearanceHelper.modelAttachmentsToJson(finalModel)
                                snapshot.vaultConfig.attachmentsJson = attJson
                                snapshot.playerConfig.attachmentsJson = attJson
                            }

                            // Apply VaultNpcComponent and PlayerNpcComponent
                            entityStore.putComponent(newEntityRef, VaultNpcComponent.componentType, snapshot.vaultConfig)
                            entityStore.putComponent(newEntityRef, PlayerNpcComponent.componentType, snapshot.playerConfig)

                            // Restore PlayerWardrobeComponent if previously equipped
                            val wardrobe = snapshot.wardrobeComponent
                            if (wardrobe != null) {
                                try {
                                    val wType = PlayerWardrobeComponent.getComponentType()
                                    if (wType != null) {
                                        val clonedWardrobe = wardrobe.clone()
                                        if (clonedWardrobe != null) {
                                            entityStore.putComponent(newEntityRef, wType, clonedWardrobe)
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }

                            // Restore DeliveryNpc component if previously present
                            val delivery = snapshot.deliveryNpc
                            if (delivery != null) {
                                try {
                                    val dType = DeliveryNpc.getComponentType()
                                    if (dType != null) {
                                        val clonedDelivery = delivery.clone()
                                        if (clonedDelivery != null) {
                                            entityStore.putComponent(newEntityRef, dType, clonedDelivery)
                                        }
                                    }
                                } catch (_: Throwable) {}
                            }

                            // Equip weapons / items
                            val mainHand = snapshot.vaultConfig.mainHandItem.takeIf { it.isNotBlank() }
                                ?: snapshot.playerConfig.mainHandItem
                            val offHand = snapshot.vaultConfig.offHandItem.takeIf { it.isNotBlank() }
                                ?: snapshot.playerConfig.offHandItem
                            AppearanceHelper.equipItems(newEntityRef, entityStore, mainHand, offHand)

                            // Re-apply pose
                            val pose = snapshot.vaultConfig.pose.takeIf { it.isNotBlank() }
                                ?: snapshot.playerConfig.pose
                            if (pose.isNotBlank() && !pose.equals("None", ignoreCase = true)) {
                                AppearanceHelper.applyPose(newEntityRef, entityStore, pose)
                            }

                            // Re-apply nameplate
                            if (snapshot.vaultConfig.displayName.isNotBlank()) {
                                NpcNameplateHelper.applyNameplate(newEntityRef, snapshot.vaultConfig.displayName, entityStore)
                            }
                        }
                    }

                    if (npcPair != null) {
                        respawnCount++
                    }
                }

                playerRef.sendMessage(
                    Message.raw("[VaultNPC] Successfully respawned $respawnCount player NPC(s) with re-imported skins.")
                )
            }
        }.exceptionally { error ->
            playerRef.sendMessage(
                Message.raw("[VaultNPC] Error while respawning player NPCs: ${error.message}")
            )
            null
        }
    }

    private fun fetchUpdatedSkin(rawUsername: String, fallbackSkin: PlayerSkin?): CompletableFuture<PlayerSkin?> {
        val cleanUsername = rawUsername.replace("§[0-9a-fk-or]".toRegex(), "").trim()
        if (cleanUsername.isBlank()) {
            return CompletableFuture.completedFuture(fallbackSkin)
        }

        // 1. Try finding online player in Universe
        val universe = try {
            Universe.get()
        } catch (_: Throwable) {
            null
        }

        val onlinePlayer = universe?.let { u ->
            try {
                u.getPlayerByUsername(cleanUsername, NameMatching.EXACT_IGNORE_CASE)
                    ?: if (cleanUsername.contains(" ")) u.getPlayerByUsername(cleanUsername.split(" ")[0], NameMatching.EXACT_IGNORE_CASE) else null
            } catch (_: Throwable) {
                null
            }
        }

        val onlineRef = onlinePlayer?.reference
        if (onlinePlayer != null && onlineRef != null && onlineRef.isValid) {
            val playerStore = onlineRef.store
            val onlineSkin = playerStore.getComponent(onlineRef, PlayerSkinComponent.getComponentType())?.playerSkin
            if (onlineSkin != null) {
                return CompletableFuture.completedFuture(AppearanceHelper.ensureValidSkin(onlineSkin))
            }
        }

        // 2. Fetch skin from HyTags API
        val lookupName = if (cleanUsername.contains(" ")) cleanUsername.split(" ")[0] else cleanUsername
        return HyTagsService.fetchSkinByUsername(lookupName).thenApply { fetchedSkin ->
            if (fetchedSkin != null) {
                AppearanceHelper.ensureValidSkin(fetchedSkin)
            } else {
                fallbackSkin
            }
        }.exceptionally {
            fallbackSkin
        }
    }
}
