package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.ArchetypeChunk
import com.hypixel.hytale.component.CommandBuffer
import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import com.hypixel.hytale.server.npc.entities.NPCEntity
import dev.rm20.thankmasvault.npc.VaultNpcComponent
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import java.util.function.BiConsumer
import javax.annotation.Nonnull

class SetNpcModelCommand : AbstractPlayerCommand(
    "npcmodel",
    "Change the model of the targeted NPC or all NPCs (e.g. /npcmodel <model> [all])"
) {

    private val modelArg = withRequiredArg("model", "The model asset ID (e.g. Elf_NPC, Player, or 'reset')", ArgTypes.STRING)
    private val entityArg: EntityWrappedArg = withOptionalArg("entity", "Target entity", ArgTypes.ENTITY_ID)

    init {
        addAliases("setnpcmodel")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val modelInput: String = modelArg.get(context).trim()
        val isReset = modelInput.equals("reset", ignoreCase = true) || modelInput.equals("clear", ignoreCase = true)

        if (!isReset) {
            val asset = ModelAsset.getAssetMap().getAsset(modelInput)
            if (asset == null) {
                context.sendMessage(Message.raw("[VaultNPC] Warning: Model '$modelInput' not found in ModelAsset registry."))
            }
        }


        // Targeted mode
        val targetRef = entityArg.get(store, context)
        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.raw("[VaultNPC] No valid NPC in view."))
            return
        }

        store.externalData.world.execute {
            if (!targetRef.isValid) return@execute
            var config = store.getComponent(targetRef, VaultNpcComponent.componentType)
            if (config == null) {
                config = VaultNpcComponent()
            }
            config.model = if (isReset) "" else modelInput
            store.putComponent(targetRef, VaultNpcComponent.componentType, config)

            if (isReset) {
                if (AppearanceHelper.isPlayerNpc(targetRef, store)) {
                    AppearanceHelper.applySavedAppearance(targetRef, store, config)
                }
                context.sendMessage(Message.raw("[VaultNPC] Reset NPC model."))
            } else {
                val applied = AppearanceHelper.applyModelToNpc(targetRef, store, modelInput)
                if (applied) {
                    context.sendMessage(Message.raw("[VaultNPC] Changed model to '$modelInput'."))
                } else {
                    context.sendMessage(Message.raw("[VaultNPC] Failed to apply model '$modelInput'."))
                }
            }
        }
    }
}
