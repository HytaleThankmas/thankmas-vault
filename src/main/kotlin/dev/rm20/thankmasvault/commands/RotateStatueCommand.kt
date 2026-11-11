package dev.rm20.thankmasvault.commands

import com.hypixel.hytale.component.Ref
import com.hypixel.hytale.component.Store
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.arguments.types.EntityWrappedArg
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand
import com.hypixel.hytale.server.core.permissions.provider.HytalePermissionsProvider
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.world.World
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore
import dev.rm20.thankmasvault.npc.player.AppearanceHelper
import dev.rm20.thankmasvault.npc.statue.StatueOrientationService
import javax.annotation.Nonnull

class RotateStatueCommand : AbstractPlayerCommand(
    "rotatestatue", "Rotate a player NPC statue and its point tool anchor"
) {

    private val angleArg = withRequiredArg("angle", "Rotation angle in degrees (0-360)", ArgTypes.FLOAT)
    private val entityArg: EntityWrappedArg = withOptionalArg("entity", "Target statue entity (optional, defaults to targeted NPC)", ArgTypes.ENTITY_ID)

    init {
        addAliases("statuerotate", "rotatenpc", "npcrotate")
        setPermissionGroups(HytalePermissionsProvider.GROUP_ADMIN)
    }

    override fun execute(
        @Nonnull context: CommandContext,
        @Nonnull store: Store<EntityStore>,
        @Nonnull ref: Ref<EntityStore>,
        @Nonnull playerRef: PlayerRef,
        @Nonnull world: World
    ) {
        val targetRef = entityArg.get(store, context)
        if (targetRef == null || !targetRef.isValid) {
            context.sendMessage(Message.raw("No valid entity found in view."))
            return
        }

        if (!AppearanceHelper.isStatue(targetRef, store)) {
            context.sendMessage(Message.raw("Target entity is not a statue."))
            return
        }

        val angle = angleArg.get(context)
        val success = StatueOrientationService.rotateStatue(targetRef, store, angle)
        if (success) {
            context.sendMessage(Message.raw("[VaultNPC] Rotated statue to ${Math.round(angle)}° (point tool anchor updated)."))
        } else {
            context.sendMessage(Message.raw("[VaultNPC] Failed to rotate statue."))
        }
    }
}
