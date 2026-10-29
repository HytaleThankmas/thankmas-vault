package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.assetstore.map.AssetMapWithIndexes
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.math.vector.Rotation3f
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.NameMatching
import com.hypixel.hytale.server.core.asset.type.model.config.Model
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.cosmetics.CosmeticsModule
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.modules.entity.component.HeadRotation
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.modules.entity.player.PlayerSkinComponent
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.NPCPlugin
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.player.PlayerNpcComponent
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import javax.annotation.Nonnull

class SpawnPlayerNpcCommand : AbstractPlayerCommand(
    "spawnplayernpc",
    "Spawn an NPC clone of yourself with your current skin and attachments"
) {

    private val nameArg = withOptionalArg("name", "Display name of the spawned NPC", ArgTypes.STRING)
    init {
        addAliases("spawnplayer", "playernpc", "splayernpc", "vaultplayernpc")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val player = store.getComponent(ref, Player.getComponentType())
        if (player == null) {
            context.sendMessage(Message.raw("Player component not found."))
            return
        }

        val transformComponent = store.getComponent(ref, TransformComponent.getComponentType())
        if (transformComponent == null) {
            context.sendMessage(Message.raw("Could not retrieve player position."))
            return
        }

        val playerPos = transformComponent.position
        val headRotation = store.getComponent(ref, HeadRotation.getComponentType())?.rotation ?: Rotation3f(0f, 0f, 0f)

        val finalName = if (nameArg.provided(context)) {
            nameArg.get(context).trim()
        } else {
            playerRef.username
        }

        val targetUsername = if (nameArg.provided(context)) {
            nameArg.get(context).trim()
        } else {
            null
        }

        val targetPlayer = if (targetUsername != null) {
            try {
                Universe.get().getPlayerByUsername(targetUsername, NameMatching.EXACT_IGNORE_CASE)
            } catch (_: Throwable) {
                null
            }
        } else null

        val targetPlayerRef = targetPlayer?.reference
        val (sourceRef, sourceUsername) = if (targetPlayer != null && targetPlayerRef != null && targetPlayerRef.isValid) {
            Pair(targetPlayerRef, targetPlayer.username)
        } else {
            Pair(ref, playerRef.username)
        }

        // Determine Model to use
        val sourceStore = sourceRef.store ?: store
        val playerModel = sourceStore.getComponent(sourceRef, ModelComponent.getComponentType())?.model
        val playerSkin = sourceStore.getComponent(sourceRef, PlayerSkinComponent.getComponentType())?.playerSkin

        val spawnModel: Model? = if (playerModel != null) {
            Model(playerModel)
        } else if (playerSkin != null) {
            CosmeticsModule.get().createModel(playerSkin)
        } else {
            null
        }

        // Determine Role
        val npcPlugin = NPCPlugin.get()
        var roleIndex = npcPlugin.getIndex("Player_NPC")
        if (roleIndex == AssetMapWithIndexes.NOT_FOUND) {
            roleIndex = npcPlugin.getIndex("Shops/Player_NPC")
        }
        if (roleIndex == AssetMapWithIndexes.NOT_FOUND) {
            roleIndex = npcPlugin.getIndex("Elf_NPC")
        }
        if (roleIndex == AssetMapWithIndexes.NOT_FOUND) {
            roleIndex = npcPlugin.getIndex("Shops/Elf_NPC")
        }
        if (roleIndex == AssetMapWithIndexes.NOT_FOUND) {
            context.sendMessage(Message.raw("Could not find a valid NPC role for spawning."))
            return
        }

        val npcPair = npcPlugin.spawnEntity(
            store,
            roleIndex,
            playerPos,
            headRotation,
            spawnModel
        ) { _, npcEntityRef, entityStore ->
            entityStore.externalData.world.execute {
                if (!npcEntityRef.isValid) return@execute

                // Copy appearance
                AppearanceHelper.copyPlayerAppearanceToNpc(
                    sourceRef,
                    npcEntityRef,
                    entityStore,
                    saveToConfig = false
                )

                var config = entityStore.getComponent(npcEntityRef, VaultNpcComponent.componentType)
                if (config == null) {
                    config = VaultNpcComponent()
                }
                config.displayName = finalName
                if (playerSkin != null) {
                    config.skinJson = AppearanceHelper.playerSkinToJson(playerSkin)
                }
                val modelToSave = playerModel ?: spawnModel
                if (modelToSave != null) {
                    config.attachmentsJson = AppearanceHelper.modelAttachmentsToJson(modelToSave)
                }

                entityStore.putComponent(npcEntityRef, VaultNpcComponent.componentType, config)

                var playerNpcComp = entityStore.getComponent(npcEntityRef, PlayerNpcComponent.componentType)
                if (playerNpcComp == null) {
                    playerNpcComp = PlayerNpcComponent()
                }
                if (playerSkin != null) {
                    playerNpcComp.skinJson = AppearanceHelper.playerSkinToJson(playerSkin)
                }
                if (modelToSave != null) {
                    playerNpcComp.attachmentsJson = AppearanceHelper.modelAttachmentsToJson(modelToSave)
                }
                entityStore.putComponent(npcEntityRef, PlayerNpcComponent.componentType, playerNpcComp)

                // if user is offline get skin from hytags
                if (targetUsername != null && sourceRef == ref && !targetUsername.equals(playerRef.username, ignoreCase = true)) {
                    AppearanceHelper.copyPlayerAppearanceByUsername(
                        targetUsername,
                        npcEntityRef,
                        entityStore,
                        saveToConfig = true
                    ).thenAccept { success ->
                        if (success) {
                            playerRef.sendMessage(
                                Message.raw("[VaultNPC] Updated '$finalName' skin")
                            )
                        } else {
                            playerRef.sendMessage(
                                Message.raw("[VaultNPC] Failed to update '$finalName' skin")
                            )
                        }
                    }
                }
            }
        }

        if (npcPair == null) {
            context.sendMessage(Message.raw("Failed to spawn NPC."))
            return
        }

        if (sourceRef != ref) {
            context.sendMessage(
                Message.raw("Spawned player NPC '$finalName' Using '$sourceUsername' skin")
            )
        } else if (targetUsername != null && !targetUsername.equals(playerRef.username, ignoreCase = true)) {
            context.sendMessage(
                Message.raw("Spawned player NPC '$finalName'. Player '$targetUsername' is offline. fetching skin")
            )
        } else {
            context.sendMessage(
                Message.raw("Spawned player NPC '$finalName'")
            )
        }
    }
}
